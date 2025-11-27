package com.AS.Yuppi.Raspi.DataWorkers.BD;

import android.app.Application;
import androidx.lifecycle.LiveData;

import java.time.LocalDate;
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
    private UserEventsDao userEventsDao;
    private UserTasksDao userTasksDao;

    public ScheduleRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        schedulesDao = db.schedulesDao();
        allSchedules = schedulesDao.getAllSchedules();
        lastActiveScheduleDao = db.lastActiveScheduleDao();
        userEventsDao = db.userEventsDao();
        userTasksDao = db.userTasksDao();
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
    
    // Синхронная вставка для немедленного обновления
    public void insertSync(final SchedulesEntity schedule) {
        try {
            databaseWriteExecutor.submit(() -> {
                schedulesDao.insert(schedule);
            }).get();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Удаление расписания
    public void delete(final SchedulesEntity schedule) {
        if (schedule == null) return;
        databaseWriteExecutor.execute(() -> {
            schedulesDao.delete(schedule);
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

    // ---- Работа с пользовательскими событиями и задачами ----

    public void insertUserEvent(final UserEventEntity event) {
        databaseWriteExecutor.execute(() -> userEventsDao.insert(event));
    }

    public List<UserEventEntity> getAllUserEventsSync() {
        try {
            return databaseReadExecutor.submit(userEventsDao::getAllEvents).get();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public UserEventEntity getUserEventByIdSync(int id) {
        try {
            return databaseReadExecutor.submit(() -> userEventsDao.getById(id)).get();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void setUserEventEnabled(final int id, final boolean enabled) {
        databaseWriteExecutor.execute(() -> userEventsDao.setEnabled(id, enabled));
    }

    public void deleteUserEventById(final int id) {
        databaseWriteExecutor.execute(() -> {
            UserEventEntity entity = userEventsDao.getById(id);
            if (entity != null) {
                userEventsDao.delete(entity);
            }
        });
    }

    public void insertUserTask(final UserTaskEntity task) {
        databaseWriteExecutor.execute(() -> userTasksDao.insert(task));
    }

    public List<UserEventEntity> getEventsForDateSync(LocalDate date) {
        try {
            return databaseReadExecutor.submit(() -> userEventsDao.getEventsForDate(date)).get();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public List<UserTaskEntity> getTasksFromDateSync(LocalDate fromDate) {
        try {
            return databaseReadExecutor.submit(() -> userTasksDao.getTasksFromDate(fromDate)).get();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public List<UserTaskEntity> getAllUserTasksSync() {
        try {
            return databaseReadExecutor.submit(userTasksDao::getAllTasks).get();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public UserTaskEntity getUserTaskByIdSync(int id) {
        try {
            return databaseReadExecutor.submit(() -> userTasksDao.getById(id)).get();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void setUserTaskDone(final int id, final boolean done) {
        databaseWriteExecutor.execute(() -> userTasksDao.setDone(id, done));
    }

    public void deleteUserTaskById(final int id) {
        databaseWriteExecutor.execute(() -> {
            UserTaskEntity entity = userTasksDao.getById(id);
            if (entity != null) {
                userTasksDao.delete(entity);
            }
        });
    }
}