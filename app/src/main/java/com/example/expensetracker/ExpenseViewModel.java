package com.example.expensetracker;

import android.app.Application;
import android.util.Log;
import androidx.core.util.Pair;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class ExpenseViewModel extends AndroidViewModel {
    private final ExpenseDao mDao;
    private final MutableLiveData<String> currentUserId = new MutableLiveData<>();
    private final MutableLiveData<Integer> sortOrder = new MutableLiveData<>(0);
    private final MutableLiveData<Pair<Long, Long>> dateRange = new MutableLiveData<>();

    public ExpenseViewModel(Application application) {
        super(application);
        mDao = AppDatabase.getDatabase(application).expenseDao();
        currentUserId.setValue(FirebaseAuth.getInstance().getUid());
        setPeriod(7); // По умолчанию показываем статистику за 7 дней
    }

    public void setUserId(String uid) { currentUserId.setValue(uid); }
    public void setSortOrder(int order) { sortOrder.setValue(order); }

    // --- ЧТЕНИЕ ДАННЫХ (Для HomeFragment) ---

    // Получить ВСЕ траты (и обычные, и подписки)
    public LiveData<List<Expense>> getAllExpenses() {
        return Transformations.switchMap(currentUserId, uid -> mDao.getAllExpensesForUser(uid));
    }

    // Получить только обычные траты, с учетом сортировки (Двойной switchMap)
    public LiveData<List<Expense>> getSortedSimpleExpenses() {
        return Transformations.switchMap(currentUserId, uid ->
                Transformations.switchMap(sortOrder, order -> {
                    if (uid == null) return new MutableLiveData<>(new ArrayList<>());

                    if (order == 1) return mDao.getSimpleExpensesDesc(uid); // Дорогие
                    if (order == 2) return mDao.getSimpleExpensesAsc(uid);  // Дешевые
                    return mDao.getSimpleExpenses(uid); // По дате (по умолчанию)
                })
        );
    }

    // --- ЧТЕНИЕ ДАННЫХ (Для StatsFragment) ---

    // Получить траты за выбранный период
    public LiveData<List<Expense>> getStatsExpenses() {
        return Transformations.switchMap(currentUserId, uid ->
                Transformations.switchMap(dateRange, range -> {
                    if (uid == null) return new MutableLiveData<>(new ArrayList<>());
                    return mDao.getExpensesBetweenDates(uid, range.first, range.second);
                })
        );
    }

    public void setPeriod(int days) {
        Calendar cal = Calendar.getInstance();
        // Конец дня сегодня (23:59:59)
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        long end = cal.getTimeInMillis();

        long start;
        if (days == 0) {
            start = 0; // Всё время
        } else {
            // Начало дня N дней назад
            cal.add(Calendar.DAY_OF_YEAR, -days);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            start = cal.getTimeInMillis();
        }
        dateRange.setValue(new Pair<>(start, end));
    }

    public void setCustomRange(Long start, Long end) {
        if (start != null && end != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(end);
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);
            dateRange.setValue(new Pair<>(start, cal.getTimeInMillis()));
        }
    }

    // --- ПОИСК И ФИЛЬТРЫ ---

    public LiveData<List<Expense>> search(String text) {
        return Transformations.switchMap(currentUserId, uid -> mDao.searchExpenses(uid, "%" + text + "%"));
    }

    public LiveData<List<Expense>> getFiltered(double min, double max, String cat) {
        String cp = cat.equals("Все") ? "%" : cat;
        return Transformations.switchMap(currentUserId, uid -> mDao.getFilteredExpenses(uid, min, max, cp));
    }

    // --- ПОДПИСКИ И ЦЕЛИ (Для PlannedFragment) ---

    public LiveData<List<Expense>> getRecurringExpenses() {
        return Transformations.switchMap(currentUserId, uid -> {
            if (uid == null) return new MutableLiveData<>(new ArrayList<>());
            return mDao.getRecurringExpenses(uid);
        });
    }

    public LiveData<Goal> getGoal() { return mDao.getActiveGoal(); }
    public void updateGoal(Goal goal) { AppDatabase.databaseWriteExecutor.execute(() -> mDao.updateGoal(goal)); }
    public void setGoal(Goal goal) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            mDao.deleteGoals();
            if (goal != null) mDao.insertGoal(goal);
        });
    }

    // --- ЗАПИСЬ И ОБЛАЧНАЯ СИНХРОНИЗАЦИЯ ---

    public void insert(Expense expense) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            mDao.insert(expense);
            syncWithCloud(expense); // Отправка в Firebase
        });
    }

    public void update(Expense expense) { AppDatabase.databaseWriteExecutor.execute(() -> mDao.update(expense)); }
    public void delete(Expense expense) { AppDatabase.databaseWriteExecutor.execute(() -> mDao.delete(expense)); }
    public void deleteAll() { AppDatabase.databaseWriteExecutor.execute(() -> mDao.deleteAll(currentUserId.getValue())); }

    private void syncWithCloud(Expense expense) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            FirebaseFirestore.getInstance().collection("users").document(uid)
                    .collection("expenses").add(expense)
                    .addOnSuccessListener(d -> Log.d("SYNC", "Expense synced!"))
                    .addOnFailureListener(e -> Log.e("SYNC", "Sync error: " + e.getMessage()));
        }
    }

    public void downloadDataFromCloud() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;
        FirebaseFirestore.getInstance().collection("users").document(uid).collection("expenses").get()
                .addOnSuccessListener(snapshots -> {
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        // Очищаем локальную БД от старых данных юзера
                        mDao.deleteAll(uid);
                        // Заливаем всё свежее из облака
                        for (com.google.firebase.firestore.DocumentSnapshot doc : snapshots) {
                            try {
                                mDao.insert(new Expense(
                                        doc.getString("title"),
                                        doc.getDouble("amount"),
                                        doc.getString("category"),
                                        uid,
                                        doc.getString("description"),
                                        doc.getLong("date"),
                                        doc.getBoolean("isRecurring")));
                            } catch (Exception e) {
                                Log.e("SYNC", "Parse error: " + e.getMessage());
                            }
                        }
                    });
                });
    }
}