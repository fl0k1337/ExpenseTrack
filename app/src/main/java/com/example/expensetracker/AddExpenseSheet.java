package com.example.expensetracker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class AddExpenseSheet extends BottomSheetDialogFragment {

    public interface OnExpenseAddedListener {
        void onAdded(String title, double amount, String category, String description, long date, boolean isRecurring, String userId);
    }

    private OnExpenseAddedListener listener;
    private Expense existingExpense;
    private boolean isRecurringMode = false;
    private long selectedDate = System.currentTimeMillis();

    public void setListener(OnExpenseAddedListener listener) { this.listener = listener; }
    public void setExistingExpense(Expense expense) { this.existingExpense = expense; }
    public void setRecurringMode(boolean recurringMode) { this.isRecurringMode = recurringMode; }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.sheet_add_expense, container, false);


        TextInputEditText etTitle = v.findViewById(R.id.etTitle);
        TextInputEditText etAmount = v.findViewById(R.id.etAmount);
        TextInputEditText etDescription = v.findViewById(R.id.etDescription);
        ChipGroup chipGroup = v.findViewById(R.id.categoryChips);
        MaterialButton btnSave = v.findViewById(R.id.btnSave);
        MaterialButton btnPickDateTop = v.findViewById(R.id.btnPickDateTop);
        TextView tvSheetTitle = v.findViewById(R.id.sheetTitle);

        if (isRecurringMode) tvSheetTitle.setText("Новая подписка");

        if (existingExpense != null) {
            tvSheetTitle.setText("Изменить");
            etTitle.setText(existingExpense.title);
            etAmount.setText(String.valueOf(existingExpense.amount));
            etDescription.setText(existingExpense.description);
            selectedDate = existingExpense.date;

            for (int i = 0; i < chipGroup.getChildCount(); i++) {
                Chip chip = (Chip) chipGroup.getChildAt(i);
                if (chip.getText().toString().equals(existingExpense.category)) {
                    chip.setChecked(true);
                    break;
                }
            }
        }

        btnPickDateTop.setOnClickListener(view -> {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTimeInMillis(selectedDate);
            new android.app.DatePickerDialog(getContext(), (datePicker, y, m, d) -> {
                cal.set(y, m, d);
                selectedDate = cal.getTimeInMillis();
                Toast.makeText(getContext(), "Дата выбрана", Toast.LENGTH_SHORT).show();
            }, cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH), cal.get(java.util.Calendar.DAY_OF_MONTH)).show();
        });


        btnSave.setOnClickListener(view -> {
            String titleStr = etTitle.getText().toString().trim();
            String amountStr = etAmount.getText().toString().trim();
            String desc = etDescription.getText().toString();

            if (titleStr.isEmpty() || amountStr.isEmpty()) return;

            try {
                double amount = Double.parseDouble(amountStr);
                int chipId = chipGroup.getCheckedChipId();
                String category = (chipId != -1) ? ((Chip) v.findViewById(chipId)).getText().toString() : "Прочее";
                String currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();

                if (listener != null) {
                    listener.onAdded(titleStr, amount, category, desc, selectedDate, isRecurringMode, currentUserId);
                }
                dismiss();
            } catch (Exception e) {
                Toast.makeText(getContext(), "Ошибка данных", Toast.LENGTH_SHORT).show();
            }
        });
        return v;
    }
}