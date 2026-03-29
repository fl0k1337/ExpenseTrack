package com.example.expensetracker;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class PlannedFragment extends Fragment {
    private ExpenseViewModel viewModel;
    private ExpenseAdapter adapter;
    private TextView tvPlannedTotal;

    // Переменная для хранения текущей цели
    private Goal activeGoal = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_planned, container, false);

        tvPlannedTotal = v.findViewById(R.id.tvPlannedTotal);
        RecyclerView rv = v.findViewById(R.id.rvPlanned);
        View cardGoal = v.findViewById(R.id.cardGoal);
        TextView tvGoalTitle = v.findViewById(R.id.tvGoalTitle);
        TextView tvGoalStatus = v.findViewById(R.id.tvGoalStatus);
        View btnReplenish = v.findViewById(R.id.btnReplenish);
        com.google.android.material.progressindicator.LinearProgressIndicator goalProgress = v.findViewById(R.id.goalProgress);

        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ExpenseAdapter();
        rv.setAdapter(adapter);

        viewModel = new ViewModelProvider(requireActivity()).get(ExpenseViewModel.class);

        // 1. НАБЛЮДЕНИЕ ЗА ЦЕЛЬЮ (Самое важное)
        viewModel.getGoal().observe(getViewLifecycleOwner(), goal -> {
            activeGoal = goal; // Запоминаем цель для кликов

            if (goal != null) {
                tvGoalTitle.setText(goal.title);
                String currency = PreferenceManager.getCurrency(requireContext());
                tvGoalStatus.setText(String.format("%.0f / %.0f %s", goal.savedAmount, goal.targetAmount, currency));

                int progress = (int) ((goal.savedAmount / goal.targetAmount) * 100);
                goalProgress.setProgress(Math.min(progress, 100), true);

                if (progress >= 100) goalProgress.setIndicatorColor(Color.parseColor("#4CAF50"));
                else goalProgress.setIndicatorColor(Color.parseColor("#6750A4"));
            } else {
                tvGoalTitle.setText("Нажмите, чтобы создать цель");
                tvGoalStatus.setText("0 / 0 ₽");
                goalProgress.setProgress(0);
            }
        });

        // 2. КЛИК ПО КНОПКЕ "ПОПОЛНИТЬ"
        btnReplenish.setOnClickListener(view -> {
            if (activeGoal != null) {
                showAddMoneyDialog(activeGoal);
            } else {
                Toast.makeText(getContext(), "Сначала создайте цель!", Toast.LENGTH_SHORT).show();
            }
        });

        // 3. КЛИК ПО КАРТОЧКЕ ДЛЯ УПРАВЛЕНИЯ
        cardGoal.setOnClickListener(view -> {
            if (activeGoal == null) {
                showCreateGoalDialog();
            } else {
                String[] options = {"Изменить цель", "Удалить цель"};
                new AlertDialog.Builder(requireContext())
                        .setTitle("Управление целью")
                        .setItems(options, (dialog, which) -> {
                            if (which == 0) showCreateGoalDialog();
                            else viewModel.setGoal(null); // Удаление
                        }).show();
            }
        });

        // 4. СПИСОК РЕГУЛЯРНЫХ ПЛАТЕЖЕЙ
        viewModel.getRecurringExpenses().observe(getViewLifecycleOwner(), expenses -> {
            List<Expense> recurringOnly = new ArrayList<>();
            double sum = 0;
            if (expenses != null) {
                for (Expense e : expenses) {
                    recurringOnly.add(e);
                    sum += e.amount;
                }
            }
            adapter.setExpenses(recurringOnly);
            String currency = PreferenceManager.getCurrency(requireContext());
            tvPlannedTotal.setText(String.format("%.2f %s", sum, currency));
        });

        v.findViewById(R.id.fabAddRecurring).setOnClickListener(view -> {
            AddExpenseSheet sheet = new AddExpenseSheet();
            sheet.setRecurringMode(true);
            sheet.setListener((title, amount, category, description, date, isRecurring) -> {
                viewModel.insert(new Expense(title, amount, category, description, date, true));
            });
            sheet.show(getChildFragmentManager(), "add_recurring");
        });

        return v;
    }

    private void showAddMoneyDialog(Goal goal) {
        EditText et = new EditText(getContext());
        et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        et.setHint("Сумма");

        new AlertDialog.Builder(requireContext())
                .setTitle("Пополнить: " + goal.title)
                .setView(et)
                .setPositiveButton("Добавить", (d, w) -> {
                    String val = et.getText().toString();
                    if (!val.isEmpty()) {
                        goal.savedAmount += Double.parseDouble(val);
                        viewModel.updateGoal(goal);
                        Toast.makeText(getContext(), "Прибавлено!", Toast.LENGTH_SHORT).show();
                    }
                }).setNegativeButton("Отмена", null).show();
    }

    private void showCreateGoalDialog() {
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(60, 40, 60, 20);
        EditText etT = new EditText(getContext()); etT.setHint("Название");
        EditText etS = new EditText(getContext()); etS.setHint("Сумма");
        etS.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        layout.addView(etT); layout.addView(etS);

        new AlertDialog.Builder(requireContext())
                .setTitle("Настройка цели")
                .setView(layout)
                .setPositiveButton("Готово", (d, w) -> {
                    String t = etT.getText().toString();
                    String s = etS.getText().toString();
                    if (!t.isEmpty() && !s.isEmpty()) {
                        viewModel.setGoal(new Goal(t, Double.parseDouble(s), 0));
                    }
                }).setNegativeButton("Отмена", null).show();
    }
}