package com.AS.Yuppi.Raspi.DataWorkers.BD;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.time.LocalDate;
import java.util.List;

@Dao
public interface UserEventsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(UserEventEntity event);

    @Delete
    void delete(UserEventEntity event);

    @Query("SELECT * FROM user_events WHERE date = :date")
    List<UserEventEntity> getEventsForDate(LocalDate date);

    @Query("SELECT * FROM user_events ORDER BY date, time")
    List<UserEventEntity> getAllEvents();

    @Query("SELECT * FROM user_events WHERE id = :id LIMIT 1")
    UserEventEntity getById(int id);

    @Query("UPDATE user_events SET isEnable = :enabled WHERE id = :id")
    void setEnabled(int id, boolean enabled);
}


