package com.example.expensetracker;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.google.android.material.chip.ChipGroup;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.google.firebase.auth.FirebaseAuth;

public class StatsFragment extends Fragment {
    private PieChart pieChart;
    private TextView tvTotalStats, tvEmptyStats;
    private LinearLayout categoriesContainer;
    private ExpenseViewModel viewModel;
    private Map<String, Double> categorySumMap = new HashMap<>();
    private double currentTotal = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_stats, container, false);

        pieChart = v.findViewById(R.id.pieChart);
        tvTotalStats = v.findViewById(R.id.tvTotalStats);
        tvEmptyStats = v.findViewById(R.id.tvEmptyStats);
        categoriesContainer = v.findViewById(R.id.categoriesContainer);
        ChipGroup periodChips = v.findViewById(R.id.periodChips);

        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleRadius(75f);
        pieChart.setHoleColor(Color.TRANSPARENT);
        pieChart.getDescription().setEnabled(false);
        pieChart.getLegend().setEnabled(false);
        pieChart.setDrawEntryLabels(false);

        viewModel = new ViewModelProvider(requireActivity()).get(ExpenseViewModel.class);

        periodChips.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(R.id.chipWeek)) viewModel.setPeriod(7);
            else if (checkedIds.contains(R.id.chipMonth)) viewModel.setPeriod(30);
            else if (checkedIds.contains(R.id.chipYear)) viewModel.setPeriod(365);
            else if (checkedIds.contains(R.id.chipAll)) viewModel.setPeriod(0);
            else if (checkedIds.contains(R.id.chipCustom)) {
                showRangePicker();
            }
        });

        viewModel.getStatsExpenses().observe(getViewLifecycleOwner(), this::updateStats);

        pieChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                PieEntry pe = (PieEntry) e;
                showSingleCategory(pe.getLabel(), pe.getValue());
            }
            @Override
            public void onNothingSelected() {
                renderCategoryList(categorySumMap);
            }
        });

        return v;
    }

    private void updateStats(List<Expense> expenses) {
        if (expenses == null || expenses.isEmpty()) {
            pieChart.setVisibility(View.GONE);
            categoriesContainer.setVisibility(View.GONE);
            tvEmptyStats.setVisibility(View.VISIBLE);
            tvTotalStats.setText("0 ₽");
        } else {
            pieChart.setVisibility(View.VISIBLE);
            categoriesContainer.setVisibility(View.VISIBLE);
            tvEmptyStats.setVisibility(View.GONE);

            currentTotal = 0;
            categorySumMap.clear();
            for (Expense e : expenses) {
                currentTotal += e.amount;
                categorySumMap.put(e.category, categorySumMap.getOrDefault(e.category, 0.0) + e.amount);
            }
            String currency = PreferenceManager.getCurrency(requireContext());
            tvTotalStats.setText(String.format("%.2f %s", currentTotal, currency));

            List<PieEntry> entries = new ArrayList<>();
            List<Integer> colors = new ArrayList<>();
            for (Map.Entry<String, Double> entry : categorySumMap.entrySet()) {
                entries.add(new PieEntry(entry.getValue().floatValue(), entry.getKey()));
                colors.add(CategoryColorMapper.getColor(entry.getKey()));
            }

            PieDataSet set = new PieDataSet(entries, "");
            set.setColors(colors);
            set.setSliceSpace(3f);
            set.setDrawValues(false);

            pieChart.setData(new PieData(set));
            pieChart.animateY(800);
            pieChart.invalidate();

            renderCategoryList(categorySumMap);
        }

        // Обновление ачивок
        RecyclerView rvAch = getView().findViewById(R.id.rvAchievements);
        if (rvAch != null) {
            rvAch.setAdapter(new AchievementAdapter(calculateAchievements(expenses != null ? expenses : new ArrayList<>())));
        }
    }

    private void renderCategoryList(Map<String, Double> map) {
        categoriesContainer.removeAllViews();
        for (Map.Entry<String, Double> entry : map.entrySet()) {
            addCategoryItem(entry.getKey(), entry.getValue());
        }
    }

    private void showSingleCategory(String name, float amount) {
        categoriesContainer.removeAllViews();
        addCategoryItem(name, (double) amount);
    }

    private void addCategoryItem(String name, Double amount) {
        String currency = PreferenceManager.getCurrency(requireContext());

        View item = getLayoutInflater().inflate(R.layout.item_category_stat, categoriesContainer, false);
        ((TextView)item.findViewById(R.id.tvCategoryName)).setText(name);
        ((TextView)item.findViewById(R.id.tvCategoryAmount)).setText(
                String.format("%.0f %s (%.1f%%)", amount, currency, (amount/currentTotal)*100)
        );
        item.findViewById(R.id.colorDot).setBackgroundColor(CategoryColorMapper.getColor(name));
        categoriesContainer.addView(item);
    }

    private List<Achievement> calculateAchievements(List<Expense> expenses) {
        List<Achievement> list = new ArrayList<>();

        // Подготовка данных для проверок
        double total = 0;
        double maxSingleAmount = 0;
        int smallPurchases = 0; // покупки меньше 150 руб
        int nightPurchases = 0; // с 00:00 до 05:00
        java.util.Set<String> categories = new java.util.HashSet<>();
        java.util.Calendar cal = java.util.Calendar.getInstance();

        for (Expense e : expenses) {
            total += e.amount;
            categories.add(e.category);
            if (e.amount > maxSingleAmount) maxSingleAmount = e.amount;
            if (e.amount < 150) smallPurchases++;

            cal.setTimeInMillis(e.date);
            int hour = cal.get(java.util.Calendar.HOUR_OF_DAY);
            if (hour >= 0 && hour < 5) nightPurchases++;
        }

        float budget = PreferenceManager.getBudget(requireContext());

        // --- БРОНЗОВЫЕ (Начало пути) ---
        list.add(new Achievement("Первая кровь", "Сделана первая запись", "💰",
                expenses.size() > 0 ? 1 : 0, 1, Color.parseColor("#CD7F32")));

        list.add(new Achievement("Новичок", "Добавьте 10 расходов", "🌱",
                Math.min(expenses.size(), 10), 10, Color.parseColor("#CD7F32")));

        // --- СЕРЕБРЯНЫЕ (Активность) ---
        list.add(new Achievement("Разнообразие", "Используйте 4 категории", "🎨",
                Math.min(categories.size(), 4), 4, Color.parseColor("#C0C0C0")));

        list.add(new Achievement("Мелочёвщик", "10 покупок дешевле 150₽", "🪙",
                Math.min(smallPurchases, 10), 10, Color.parseColor("#C0C0C0")));

        list.add(new Achievement("Дисциплина", "Траты < 90% лимита", "📊",
                (total > 0 && total < budget * 0.9) ? 1 : 0, 1, Color.parseColor("#C0C0C0")));

        // --- ЗОЛОТЫЕ (Мастерство) ---
        list.add(new Achievement("Шопоголик", "Добавьте 30 расходов", "🛒",
                Math.min(expenses.size(), 30), 30, Color.parseColor("#FFD700")));

        list.add(new Achievement("Инвестор", "Разовая трата > 5000₽", "🏗️",
                maxSingleAmount >= 5000 ? 1 : 0, 1, Color.parseColor("#FFD700")));

        list.add(new Achievement("Экономист", "Траты < 50% лимита", "📉",
                (total > 0 && total < budget * 0.5) ? 1 : 0, 1, Color.parseColor("#FFD700")));

        list.add(new Achievement("Всеядный", "Использованы все 7 категорий", "🌈",
                Math.min(categories.size(), 7), 7, Color.parseColor("#FFD700")));

        list.add(new Achievement("Ночная жрица", "5 покупок ночью (00:00-05:00)", "🍕",
                Math.min(nightPurchases, 5), 5, Color.parseColor("#5D3FD3"))); // Фиолетовый

        list.add(new Achievement("Миллионер", "Общие траты > 50 000₽", "🏢",
                Math.min((int)total, 50000), 50000, Color.parseColor("#5D3FD3")));
        return list;
    }

    private class AchievementAdapter extends RecyclerView.Adapter<AchievementAdapter.VH> {
        private List<Achievement> data;
        public AchievementAdapter(List<Achievement> data) { this.data = data; }

        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new VH(getLayoutInflater().inflate(R.layout.item_achievement, p, false));
        }

        @Override public void onBindViewHolder(@NonNull VH h, int p) {
            Achievement a = data.get(p);
            h.title.setText(a.title);
            h.desc.setText(a.description);

            // Форматирование текста прогресса
            if (a.maxProgress >= 1000) {
                h.progressText.setText(String.format("%.1fk / %.1fk", a.currentProgress/1000.0, a.maxProgress/1000.0));
            } else {
                h.progressText.setText(a.currentProgress + " / " + a.maxProgress);
            }

            h.icon.setText(a.isUnlocked() ? a.icon : "🔒");
            h.progress.setMax(a.maxProgress);
            h.progress.setProgress(a.currentProgress);

            // КРАСИМ КАРТОЧКУ (Исправлено)
            if (a.isUnlocked()) {
                h.card.setCardBackgroundColor(a.color); // Теперь вызываем у MaterialCardView
                h.title.setTextColor(Color.WHITE);
                h.desc.setTextColor(Color.parseColor("#EEEEEE"));
                h.progressText.setTextColor(Color.WHITE);
                h.progress.setIndicatorColor(Color.WHITE);
                h.progress.setTrackColor(Color.parseColor("#40FFFFFF"));
            } else {
                h.card.setCardBackgroundColor(Color.WHITE);
                h.title.setTextColor(Color.BLACK);
                h.desc.setTextColor(Color.GRAY);
                h.progressText.setTextColor(Color.GRAY);
                h.progress.setIndicatorColor(a.color);
                h.progress.setTrackColor(Color.parseColor("#15000000"));
            }
        }

        @Override public int getItemCount() { return data.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView title, desc, icon, progressText;
            com.google.android.material.progressindicator.LinearProgressIndicator progress;
            com.google.android.material.card.MaterialCardView card; // Тип изменен на MaterialCardView

            public VH(View v) { super(v);
                title = v.findViewById(R.id.tvAchTitle);
                desc = v.findViewById(R.id.tvAchDesc);
                icon = v.findViewById(R.id.tvAchIcon);
                progressText = v.findViewById(R.id.tvProgressText);
                progress = v.findViewById(R.id.achProgress);
                card = (com.google.android.material.card.MaterialCardView) v;
            }
        }
    }
    private void showRangePicker() {
        com.google.android.material.datepicker.MaterialDatePicker<androidx.core.util.Pair<Long, Long>> picker =
                com.google.android.material.datepicker.MaterialDatePicker.Builder.dateRangePicker()
                        .setTitleText("Выберите период")
                        .setSelection(new androidx.core.util.Pair<>(
                                com.google.android.material.datepicker.MaterialDatePicker.todayInUtcMilliseconds(),
                                com.google.android.material.datepicker.MaterialDatePicker.todayInUtcMilliseconds()
                        ))
                        .build();

        picker.show(getChildFragmentManager(), "range_picker");

        picker.addOnPositiveButtonClickListener(selection -> {
            if (selection != null && selection.first != null && selection.second != null) {
                viewModel.setCustomRange(selection.first, selection.second);

                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd.MM", java.util.Locale.getDefault());
                String rangeStr = sdf.format(new java.util.Date(selection.first)) + " - " + sdf.format(new java.util.Date(selection.second));
                com.google.android.material.chip.Chip chipCustom = getView().findViewById(R.id.chipCustom);
                chipCustom.setText(rangeStr);
            }
        });
    }
}