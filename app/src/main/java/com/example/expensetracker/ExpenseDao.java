package com.example.expensetracker;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface ExpenseDao {
    @Insert
    void insert(Expense expense);

    @Delete
    void delete(Expense expense);

    @Update
    void update(Expense expense);

    @Query("SELECT * FROM expenses ORDER BY date DESC")
    LiveData<List<Expense>> getAllExpenses();

    @Query("SELECT * FROM expenses WHERE isRecurring = 0 ORDER BY date DESC")
    LiveData<List<Expense>> getSimpleExpenses();

    @Query("SELECT * FROM expenses WHERE isRecurring = 0 ORDER BY amount DESC")
    LiveData<List<Expense>> getSimpleExpensesDesc();

    @Query("SELECT * FROM expenses WHERE isRecurring = 0 ORDER BY amount ASC")
    LiveData<List<Expense>> getSimpleExpensesAsc();

    @Query("SELECT * FROM expenses WHERE isRecurring = 1")
    LiveData<List<Expense>> getRecurringExpenses();

    @Query("SELECT * FROM expenses WHERE date BETWEEN :start AND :end ORDER BY date DESC")
    LiveData<List<Expense>> getExpensesBetweenDates(long start, long end);

    @Query("SELECT * FROM expenses WHERE title LIKE :query OR category LIKE :query ORDER BY date DESC")
    LiveData<List<Expense>> searchExpenses(String query);

    @Query("SELECT * FROM expenses WHERE isRecurring = 0 AND amount >= :min AND amount <= :max AND category LIKE :cat ORDER BY amount DESC")
    LiveData<List<Expense>> getFilteredExpenses(double min, double max, String cat);

    @Query("DELETE FROM expenses")
    void deleteAll();

    @Insert
    void insertGoal(Goal goal);

    @Query("SELECT * FROM goals LIMIT 1")
    LiveData<Goal> getActiveGoal();

    @Query("DELETE FROM goals")
    void deleteGoals();

    @Update
    void updateGoal(Goal goal);
}