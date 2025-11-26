package com.AS.Yuppi.Raspi.DataWorkers.BD;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.time.LocalDate;

/**
 * Entity для сохранения пользовательских задач.
 */
@Entity(tableName = "user_tasks")
public class UserTaskEntity {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private LocalDate endpoint;
    private String name;
    private String task;
    private boolean isDone;

    public UserTaskEntity(LocalDate endpoint, String name, String task, boolean isDone) {
        this.endpoint = endpoint;
        this.name = name;
        this.task = task;
        this.isDone = isDone;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public LocalDate getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(LocalDate endpoint) {
        this.endpoint = endpoint;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTask() {
        return task;
    }

    public void setTask(String task) {
        this.task = task;
    }

    public boolean isDone() {
        return isDone;
    }

    public void setDone(boolean done) {
        isDone = done;
    }
}


