package com.AS.Yuppi.Raspi.DataWorkers.BD;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.time.LocalDate;
import java.util.List;

@Dao
public interface UserTasksDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(UserTaskEntity task);

    @Delete
    void delete(UserTaskEntity task);

    @Query("SELECT * FROM user_tasks WHERE endpoint >= :fromDate")
    List<UserTaskEntity> getTasksFromDate(LocalDate fromDate);

    @Query("SELECT * FROM user_tasks ORDER BY endpoint")
    List<UserTaskEntity> getAllTasks();

    @Query("SELECT * FROM user_tasks WHERE id = :id LIMIT 1")
    UserTaskEntity getById(int id);

    @Query("UPDATE user_tasks SET isDone = :done WHERE id = :id")
    void setDone(int id, boolean done);
}


