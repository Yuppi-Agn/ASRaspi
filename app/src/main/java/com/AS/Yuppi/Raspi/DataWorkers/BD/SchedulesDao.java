package com.AS.Yuppi.Raspi.DataWorkers.BD;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface SchedulesDao {

    // Вставка с заменой, если существует конфликт по первичному ключу
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(SchedulesEntity schedule);

    @Update
    void update(SchedulesEntity schedule);

    @Delete
    void delete(SchedulesEntity schedule);

    // Получение всех расписаний, LiveData автоматически обновит UI при изменении данных
    @Query("SELECT * FROM schedules_table ORDER BY Name ASC")
    LiveData<List<SchedulesEntity>> getAllSchedules();

    // Получение расписания по ID
    @Query("SELECT * FROM schedules_table WHERE id = :scheduleId")
    SchedulesEntity getScheduleById(int scheduleId);

    // Получение расписания по имени
    @Query("SELECT * FROM schedules_table WHERE Name = :scheduleName LIMIT 1")
    LiveData<SchedulesEntity> getScheduleByName(String scheduleName);
    /**
     * Получает список расписаний, включая только поля Name и Author. (Синхронно)
     */
    @Query("SELECT Name, Author FROM schedules_table ORDER BY Name ASC")
    List<ScheduleNameAuthor> getScheduleNamesAndAuthorsSync();

    /**
     * Получение расписания по имени. (Синхронно)
     */
    @Query("SELECT * FROM schedules_table WHERE Name = :scheduleName LIMIT 1")
    SchedulesEntity getScheduleByNameSync(String scheduleName);
}