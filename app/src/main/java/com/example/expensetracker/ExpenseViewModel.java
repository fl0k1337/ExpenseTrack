package com.example.expensetracker;

import android.app.Application;
import androidx.core.util.Pair;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import java.util.Calendar;
import java.util.List;

public class ExpenseViewModel extends AndroidViewModel {
    private final ExpenseDao mDao;
    private final MutableLiveData<Integer> sortOrder = new MutableLiveData<>(0);
    private final MutableLiveData<Pair<Long, Long>> dateRange = new MutableLiveData<>();
    private final LiveData<List<Expense>> statsExpenses;

    public ExpenseViewModel(Application application) {
        super(application);
        mDao = AppDatabase.getDatabase(application).expenseDao();
        setPeriod(7);

        statsExpenses = Transformations.switchMap(dateRange, range ->
                mDao.getExpensesBetweenDates(range.first, range.second)
        );
    }

    public void setSortOrder(int order) { sortOrder.setValue(order); }

    public LiveData<List<Expense>> getAllExpenses() { return mDao.getAllExpenses(); }

    public LiveData<List<Expense>> getSortedSimpleExpenses() {
        return Transformations.switchMap(sortOrder, order -> {
            if (order == 1) return mDao.getSimpleExpensesDesc();
            if (order == 2) return mDao.getSimpleExpensesAsc();
            return mDao.getSimpleExpenses();
        });
    }

    public LiveData<List<Expense>> getStatsExpenses() { return statsExpenses; }
    public LiveData<List<Expense>> getRecurringExpenses() { return mDao.getRecurringExpenses(); }

    public void setPeriod(int days) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59);
        long end = cal.getTimeInMillis();
        long start;
        if (days == 0) {
            start = 0;
        } else {
            cal.add(Calendar.DAY_OF_YEAR, -days);
            cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0);
            start = cal.getTimeInMillis();
        }
        dateRange.setValue(new Pair<>(start, end));
    }

    public void setCustomRange(Long start, Long end) {
        if (start != null && end != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(end);
            cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59);
            dateRange.setValue(new Pair<>(start, cal.getTimeInMillis()));
        }
    }

    public LiveData<List<Expense>> search(String text) { return mDao.searchExpenses("%" + text + "%"); }

    public LiveData<List<Expense>> getFiltered(double min, double max, String cat) {
        String cp = cat.equals("Все") ? "%" : cat;
        return mDao.getFilteredExpenses(min, max, cp);
    }

    public void insert(Expense expense) { AppDatabase.databaseWriteExecutor.execute(() -> mDao.insert(expense)); }
    public void update(Expense expense) { AppDatabase.databaseWriteExecutor.execute(() -> mDao.update(expense)); }
    public void delete(Expense expense) { AppDatabase.databaseWriteExecutor.execute(() -> mDao.delete(expense)); }
    public void deleteAll() { AppDatabase.databaseWriteExecutor.execute(mDao::deleteAll); }

    public LiveData<Goal> getGoal() { return mDao.getActiveGoal(); }
    public void updateGoal(Goal goal) { AppDatabase.databaseWriteExecutor.execute(() -> mDao.updateGoal(goal)); }
    public void setGoal(Goal goal) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            mDao.deleteGoals();
            if (goal != null) mDao.insertGoal(goal);
        });
    }
}