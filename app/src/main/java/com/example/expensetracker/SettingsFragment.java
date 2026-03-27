package com.example.expensetracker;

import android.content.Intent;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SettingsFragment extends Fragment {

    private ExpenseViewModel viewModel;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // 1. Инфлейтим макет
        View v = inflater.inflate(R.layout.fragment_settings, container, false);

        // 2. Инициализируем ViewModel
        viewModel = new ViewModelProvider(requireActivity()).get(ExpenseViewModel.class);

        // 3. Находим все View элементы
        TextView tvName = v.findViewById(R.id.tvCurrentName);
        TextView tvUserEmail = v.findViewById(R.id.tvUserEmail);
        TextView tvCurrency = v.findViewById(R.id.tvCurrentCurrency);
        MaterialSwitch switchDark = v.findViewById(R.id.switchDark);
        View btnLogout = v.findViewById(R.id.btnLogout);
        View cardName = v.findViewById(R.id.cardName);
        View btnCurrency = v.findViewById(R.id.btnCurrency);
        View btnInstruction = v.findViewById(R.id.btnInstruction);
        View btnReset = v.findViewById(R.id.btnReset);

        // --- БЛОК ПРОФИЛЯ ---

        // Отображаем имя из настроек
        tvName.setText(PreferenceManager.getUserName(requireContext()));

        // Отображаем Email текущего пользователя Firebase
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && tvUserEmail != null) {
            tvUserEmail.setText(user.getEmail());
        }

        // Клик по имени для изменения
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
                    })
                    .setNegativeButton("Отмена", null)
                    .show();
        });

        // Кнопка выхода из аккаунта
        if (btnLogout != null) {
            btnLogout.setOnClickListener(view -> {
                new AlertDialog.Builder(requireContext())
                        .setTitle("Выход")
                        .setMessage("Вы действительно хотите выйти из аккаунта?")
                        .setPositiveButton("Выйти", (dialog, which) -> {
                            // Выход из Firebase
                            FirebaseAuth.getInstance().signOut();
                            // Сброс ID в ViewModel, чтобы очистить списки
                            viewModel.setUserId(null);

                            // Переход на экран логина
                            Intent intent = new Intent(getActivity(), AuthActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            getActivity().finish();
                        })
                        .setNegativeButton("Отмена", null)
                        .show();
            });
        }

        // --- БЛОК ВНЕШНЕГО ВИДА ---

        // Отображаем текущую валюту
        tvCurrency.setText(PreferenceManager.getCurrency(requireContext()));

        // Смена валюты
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

        // Темная тема
        switchDark.setChecked(PreferenceManager.isDarkMode(requireContext()));
        switchDark.setOnCheckedChangeListener((btn, isChecked) -> {
            PreferenceManager.setDarkMode(requireContext(), isChecked);
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });

        // --- БЛОК ПОМОЩИ И ДАННЫХ ---

        // Инструкция
        btnInstruction.setOnClickListener(view -> showInstruction());

        // Полный сброс данных текущего пользователя
        btnReset.setOnClickListener(view -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Внимание!")
                    .setMessage("Это удалит все ВАШИ записи. Продолжить?")
                    .setPositiveButton("Удалить всё", (d, w) -> {
                        viewModel.deleteAll(); // Удалит только записи текущего юзера
                        Toast.makeText(getContext(), "Данные очищены", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Отмена", null).show();
        });

        return v;
    }

    private void showInstruction() {
        String msg = "💰 Главная: следите за бюджетом и лимитом на день.\n\n" +
                "📊 Статистика: анализируйте траты по категориям.\n\n" +
                "📅 План: ваши ежемесячные подписки и цели.\n\n" +
                "☁️ Облако: ваши данные привязаны к Email и доступны на любом устройстве.";

        new AlertDialog.Builder(requireContext())
                .setTitle("Руководство")
                .setMessage(msg)
                .setPositiveButton("Понятно", null)
                .show();
    }
}