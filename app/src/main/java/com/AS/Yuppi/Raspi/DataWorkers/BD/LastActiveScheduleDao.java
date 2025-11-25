package com.AS.Yuppi.Raspi.DataWorkers.BD;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface LastActiveScheduleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(LastActiveScheduleEntity entity);
    @Query("SELECT lastActiveScheduleAuthorName FROM last_active_schedule_table WHERE id = 1")
    String getLastActiveScheduleAuthorNameSync();
}