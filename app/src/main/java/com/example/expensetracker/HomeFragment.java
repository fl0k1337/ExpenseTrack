package com.example.expensetracker;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class HomeFragment extends Fragment {
    private ExpenseViewModel viewModel;
    private TextView tvTotalSum;
    private ExpenseAdapter adapter;
    private LinearLayout layoutEmpty;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_home, container, false);
        tvTotalSum = v.findViewById(R.id.tvTotalSum);
        layoutEmpty = v.findViewById(R.id.layoutEmpty);
        RecyclerView recyclerView = v.findViewById(R.id.recyclerView);

        adapter = new ExpenseAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        viewModel = new ViewModelProvider(requireActivity()).get(ExpenseViewModel.class);

        // Наблюдение за данными
        viewModel.getSortedSimpleExpenses().observe(getViewLifecycleOwner(), expenses -> {
            adapter.setExpenses(expenses);
            layoutEmpty.setVisibility(expenses.isEmpty() ? View.VISIBLE : View.GONE);
            updateTotalSum(expenses);
        });

        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView r, @NonNull RecyclerView.ViewHolder vh, @NonNull RecyclerView.ViewHolder t) { return false; }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Expense deleted = adapter.getExpenseAt(position);
                viewModel.delete(deleted);
                com.google.android.material.snackbar.Snackbar.make(recyclerView, "Удалено: " + deleted.title, 3000)
                        .setAction("ОТМЕНИТЬ", view -> viewModel.insert(deleted)).show();
            }
        }).attachToRecyclerView(recyclerView);

        // Клик для редактирования
        adapter.setOnItemClickListener(expense -> {
            AddExpenseSheet sheet = new AddExpenseSheet();
            sheet.setExistingExpense(expense);
            sheet.setListener((title, amount, category, description, date, isRecurring, userId) -> {
                expense.title = title; expense.amount = amount; expense.category = category;
                expense.description = description; expense.date = date;
                viewModel.update(expense);
            });
            sheet.show(getChildFragmentManager(), "edit_expense");
        });

        // Добавление
        v.findViewById(R.id.fabAddNormal).setOnClickListener(view -> {
            AddExpenseSheet sheet = new AddExpenseSheet();
            sheet.setListener((title, amount, category, description, date, isRecurring, userId) -> {
                viewModel.insert(new Expense(title, amount, category, userId, description, date, false));
            });
            sheet.show(getChildFragmentManager(), "add_expense");
        });

        // --- ПОПРАВИЛИ УСТАНОВКУ ЛИМИТА ---
        v.findViewById(R.id.totalCard).setOnClickListener(view -> {
            EditText etLimit = new EditText(getContext());
            etLimit.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
            etLimit.setText(String.valueOf(PreferenceManager.getBudget(requireContext())));

            new AlertDialog.Builder(requireContext())
                    .setTitle("Месячный лимит")
                    .setMessage("Введите сумму лимита:")
                    .setView(etLimit)
                    .setPositiveButton("Сохранить", (d, w) -> {
                        String val = etLimit.getText().toString();
                        if (!val.isEmpty()) {
                            PreferenceManager.setBudget(requireContext(), Float.parseFloat(val));
                            // Перерисовываем UI
                            updateTotalSum(viewModel.getSortedSimpleExpenses().getValue());
                        }
                    }).setNegativeButton("Отмена", null).show();
        });

        v.findViewById(R.id.totalCard).setOnLongClickListener(view -> {
            String[] options = {"По дате (новые сверху)", "Сначала дорогие", "Сначала дешевые"};

            new AlertDialog.Builder(requireContext())
                    .setTitle("Сортировка списка")
                    .setItems(options, (dialog, which) -> {
                        // 0 - дата, 1 - дорого, 2 - дешево
                        viewModel.setSortOrder(which);
                    })
                    .show();
            return true;
        });

        v.findViewById(R.id.btnFilter).setOnClickListener(view -> {
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_filter, null);
            EditText etMin = dialogView.findViewById(R.id.etMinPrice);
            EditText etMax = dialogView.findViewById(R.id.etMaxPrice);
            Spinner spinner = dialogView.findViewById(R.id.spinnerCategory);

            String[] categories = {"Все", "Еда", "Транспорт", "Развлечения", "Жилье", "Здоровье", "Покупки", "Прочее"};
            android.widget.ArrayAdapter<String> adapterSpin = new android.widget.ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, categories);
            spinner.setAdapter(adapterSpin);

            new AlertDialog.Builder(requireContext())
                    .setTitle("Фильтр расходов")
                    .setView(dialogView)
                    .setPositiveButton("Применить", (d, w) -> {
                        // Если поля пустые, ставим значения по умолчанию (от 0 до бесконечности)
                        double min = etMin.getText().toString().isEmpty() ? 0 : Double.parseDouble(etMin.getText().toString());
                        double max = etMax.getText().toString().isEmpty() ? 9999999 : Double.parseDouble(etMax.getText().toString());
                        String selectedCat = spinner.getSelectedItem().toString();

                        // Вызываем фильтрацию из ViewModel
                        viewModel.getFiltered(min, max, selectedCat).observe(getViewLifecycleOwner(), filteredExpenses -> {
                            // Обновляем список в адаптере
                            adapter.setExpenses(filteredExpenses);
                            // Скрываем/показываем заглушку "Нет расходов"
                            layoutEmpty.setVisibility(filteredExpenses.isEmpty() ? View.VISIBLE : View.GONE);
                            // ПЕРЕСЧИТЫВАЕМ И ОБНОВЛЯЕМ СУММУ БЮДЖЕТА под текущий фильтр
                            updateTotalSum(filteredExpenses);
                        });
                    })
                    .setNegativeButton("Сбросить", (d, w) -> {
                        // При сбросе возвращаем обычный список
                        viewModel.getSortedSimpleExpenses().observe(getViewLifecycleOwner(), expenses -> {
                            adapter.setExpenses(expenses);
                            layoutEmpty.setVisibility(expenses.isEmpty() ? View.VISIBLE : View.GONE);
                            updateTotalSum(expenses);
                        });
                    })
                    .show();
        });

        return v;
    }

    private void updateTotalSum(List<Expense> expenses) {
        if (getView() == null || expenses == null) return;
        double total = 0;
        for (Expense e : expenses) total += e.amount;

        String currency = PreferenceManager.getCurrency(requireContext());
        float limit = PreferenceManager.getBudget(requireContext());

        tvTotalSum.setText(String.format("%.2f %s", total, currency));

        TextView tvStatus = getView().findViewById(R.id.tvBudgetStatus);
        com.google.android.material.progressindicator.LinearProgressIndicator pb = getView().findViewById(R.id.budgetProgress);

        if (pb != null && tvStatus != null) {
            int progress = (limit > 0) ? (int) ((total / limit) * 100) : 0;
            pb.setProgress(Math.min(progress, 100), true);
            if (total >= limit) {
                tvStatus.setText("Лимит превышен!");
                pb.setIndicatorColor(android.graphics.Color.RED);
            } else {
                tvStatus.setText(String.format("Осталось: %.2f %s", (limit - total), currency));
                pb.setIndicatorColor(android.graphics.Color.WHITE);
            }
        }
    }
}