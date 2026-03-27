package com.example.expensetracker;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import com.google.firebase.auth.FirebaseAuth;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ExpenseHolder> {
    private List<Expense> expenses = new ArrayList<>();

    @NonNull
    @Override
    public ExpenseHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.expense_item, parent, false);
        return new ExpenseHolder(view);
    }

    public interface OnItemClickListener {
        void onItemClick(Expense expense);
    }
    private OnItemClickListener listener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @Override
    public void onBindViewHolder(@NonNull ExpenseHolder holder, int position) {
        Expense current = expenses.get(position);
        String currency = PreferenceManager.getCurrency(holder.itemView.getContext());

        holder.tvTitle.setText(current.title);
        holder.tvAmount.setText(String.format("%.2f %s", current.amount, currency));
        holder.tvAmount.setTextColor(CategoryColorMapper.getColor(current.category));

        holder.tvDescription.setText(current.description);
        if (current.description == null || current.description.isEmpty()) {
            holder.tvDescription.setVisibility(View.GONE);
        } else {
            holder.tvDescription.setVisibility(View.VISIBLE);
        }

        if (current.isRecurring) {
            holder.tvCategory.setText(current.category + " • Ежемесячно");
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
            String dateString = sdf.format(new Date(current.date));
            holder.tvCategory.setText(current.category + " • " + dateString);
        }

        holder.ivCategoryIcon.setImageResource(CategoryColorMapper.getIcon(current.category));
        holder.ivCategoryIcon.setColorFilter(CategoryColorMapper.getColor(current.category));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(current);
        });
    }

    @Override
    public int getItemCount() { return expenses.size(); }

    public void setExpenses(List<Expense> expenses) {
        this.expenses = expenses;
        notifyDataSetChanged();
    }

    public Expense getExpenseAt(int position) { return expenses.get(position); }

    class ExpenseHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvCategory, tvAmount, tvDescription;
        ImageView ivCategoryIcon;
        public ExpenseHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            ivCategoryIcon = itemView.findViewById(R.id.ivCategoryIcon);
        }
    }
}