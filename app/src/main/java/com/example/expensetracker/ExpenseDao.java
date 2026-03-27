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

    // Получить ВООБЩЕ ВСЕ записи пользователя (и обычные, и подписки)
    @Query("SELECT * FROM expenses WHERE userId = :uid ORDER BY date DESC")
    LiveData<List<Expense>> getAllExpensesForUser(String uid);

    @Query("SELECT * FROM expenses WHERE userId = :uid AND isRecurring = 0 ORDER BY date DESC")
    LiveData<List<Expense>> getSimpleExpenses(String uid);

    // Сначала дорогие
    @Query("SELECT * FROM expenses WHERE userId = :uid AND isRecurring = 0 ORDER BY amount DESC")
    LiveData<List<Expense>> getSimpleExpensesDesc(String uid);

    // Сначала дешевые
    @Query("SELECT * FROM expenses WHERE userId = :uid AND isRecurring = 0 ORDER BY amount ASC")
    LiveData<List<Expense>> getSimpleExpensesAsc(String uid);

    @Query("SELECT * FROM expenses WHERE userId = :uid AND isRecurring = 1")
    LiveData<List<Expense>> getRecurringExpenses(String uid);

    @Query("SELECT * FROM expenses WHERE userId = :uid AND date BETWEEN :start AND :end ORDER BY date DESC")
    LiveData<List<Expense>> getExpensesBetweenDates(String uid, long start, long end);

    @Query("SELECT * FROM expenses WHERE userId = :uid AND (title LIKE :query OR category LIKE :query) ORDER BY date DESC")
    LiveData<List<Expense>> searchExpenses(String uid, String query);

    // Расширенный фильтр
    @Query("SELECT * FROM expenses WHERE userId = :uid AND isRecurring = 0 AND amount >= :min AND amount <= :max AND category LIKE :cat ORDER BY amount DESC")
    LiveData<List<Expense>> getFilteredExpenses(String uid, double min, double max, String cat);

    @Query("DELETE FROM expenses WHERE userId = :uid")
    void deleteAll(String uid);

    // МЕТОДЫ ДЛЯ КОПИЛКИ
    @Insert
    void insertGoal(Goal goal);

    @Query("SELECT * FROM goals LIMIT 1")
    LiveData<Goal> getActiveGoal();

    @Query("DELETE FROM goals")
    void deleteGoals();

    @Update
    void updateGoal(Goal goal);
}