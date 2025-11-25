package com.AS.Yuppi.Raspi.DataWorkers.BD;

import android.app.Application;
import androidx.lifecycle.LiveData;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScheduleRepository {
    private SchedulesDao schedulesDao;
    private LiveData<List<SchedulesEntity>> allSchedules;
    private static final int NUMBER_OF_THREADS = 4;
    private static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);
    private static final ExecutorService databaseReadExecutor =
            Executors.newSingleThreadExecutor();
    private LastActiveScheduleDao lastActiveScheduleDao;

    public ScheduleRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        schedulesDao = db.schedulesDao();
        allSchedules = schedulesDao.getAllSchedules();
        lastActiveScheduleDao = db.lastActiveScheduleDao();
    }
    public void saveLastActiveScheduleName(final String authorName) {
        databaseWriteExecutor.execute(() -> {
            LastActiveScheduleEntity entity = new LastActiveScheduleEntity(authorName);
            lastActiveScheduleDao.insertOrUpdate(entity);
        });
    }
    public String getLastActiveScheduleNameSync() {
        try {
            // Использование .get() для блокировки и получения синхронного результата
            return databaseWriteExecutor.submit(() -> lastActiveScheduleDao.getLastActiveScheduleAuthorNameSync()).get();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public LiveData<List<SchedulesEntity>> getAllSchedules() {
        return allSchedules;
    }

    // Вставка должна выполняться в фоновом потоке
    public void insert(final SchedulesEntity schedule) {
        databaseWriteExecutor.execute(() -> {
            schedulesDao.insert(schedule);
        });
    }

    // Обновление также должно выполняться в фоновом потоке
    public void update(final SchedulesEntity schedule) {
        databaseWriteExecutor.execute(() -> {
            schedulesDao.update(schedule);
        });
    }

    /**
     * Получает список проекций "Имя-Автор" синхронно в фоновом потоке.
     * @return Список ScheduleNameAuthor.
     */
    public List<ScheduleNameAuthor> getScheduleNamesAndAuthorsSync() {
        try {
            return databaseReadExecutor.submit(() ->
                    schedulesDao.getScheduleNamesAndAuthorsSync()
            ).get(); // .get() блокирует поток до получения результата
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Получает полный объект SchedulesEntity по имени синхронно в фоновом потоке.
     * @param name Имя расписания.
     * @return SchedulesEntity или null.
     */
    public SchedulesEntity getScheduleByNameSync(String name) {
        try {
            return databaseReadExecutor.submit(() ->
                    schedulesDao.getScheduleByNameSync(name)
            ).get();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}