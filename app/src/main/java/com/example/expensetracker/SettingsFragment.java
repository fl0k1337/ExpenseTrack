package com.example.expensetracker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.materialswitch.MaterialSwitch;

public class SettingsFragment extends Fragment {
    private ExpenseViewModel viewModel;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_settings, container, false);
        viewModel = new ViewModelProvider(requireActivity()).get(ExpenseViewModel.class);

        TextView tvName = v.findViewById(R.id.tvCurrentName);
        TextView tvCurrency = v.findViewById(R.id.tvCurrentCurrency);
        MaterialSwitch switchDark = v.findViewById(R.id.switchDark);
        View cardName = v.findViewById(R.id.cardName);
        View btnCurrency = v.findViewById(R.id.btnCurrency);
        View btnInstruction = v.findViewById(R.id.btnInstruction);
        View btnReset = v.findViewById(R.id.btnReset);

        tvName.setText(PreferenceManager.getUserName(requireContext()));
        tvCurrency.setText(PreferenceManager.getCurrency(requireContext()));

        cardName.setOnClickListener(view -> {
            EditText et = new EditText(getContext());
            et.setText(tvName.getText());
            new AlertDialog.Builder(requireContext())
                    .setTitle("Ваше имя")
                    .setView(et)
                    .setPositiveButton("Сохранить", (d, w) -> {
                        String newName = et.getText().toString();
                        if (!newName.isEmpty()) {
                            PreferenceManager.setUserName(requireContext(), newName);
                            tvName.setText(newName);
                        }
                    }).setNegativeButton("Отмена", null).show();
        });

        btnCurrency.setOnClickListener(view -> {
            String[] items = {"₽", "$", "€", "₸", "£", "Br"};
            new AlertDialog.Builder(requireContext())
                    .setTitle("Выберите валюту")
                    .setItems(items, (d, which) -> {
                        PreferenceManager.setCurrency(requireContext(), items[which]);
                        tvCurrency.setText(items[which]);
                        Toast.makeText(getContext(), "Валюта обновлена", Toast.LENGTH_SHORT).show();
                    }).show();
        });

        switchDark.setChecked(PreferenceManager.isDarkMode(requireContext()));
        switchDark.setOnCheckedChangeListener((btn, isChecked) -> {
            PreferenceManager.setDarkMode(requireContext(), isChecked);
            AppCompatDelegate.setDefaultNightMode(isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        });

        btnInstruction.setOnClickListener(view -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Руководство")
                    .setMessage("💰 Главная: следите за бюджетом.\n📊 Статистика: анализируйте траты.\n📅 План: подписки и цели.")
                    .setPositiveButton("Понятно", null).show();
        });

        btnReset.setOnClickListener(view -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Внимание!")
                    .setMessage("Это удалит все данные. Продолжить?")
                    .setPositiveButton("Удалить всё", (d, w) -> {
                        viewModel.deleteAll();
                        Toast.makeText(getContext(), "Данные очищены", Toast.LENGTH_SHORT).show();
                    }).setNegativeButton("Отмена", null).show();
        });

        return v;
    }
}