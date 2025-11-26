package com.AS.Yuppi.Raspi.DataWorkers;

import java.time.LocalDate;

/**
 * Модель пользовательской задачи.
 */
public class UserTask {
    private int Id;              // идентификатор в БД
    private LocalDate Endpoint; // дата, к которой нужно выполнить
    private String Name;        // название
    private String Task;        // информация о задаче
    private boolean IsDone = false; // выполнена ли задача

    public UserTask(LocalDate endpoint, String name, String task) {
        this.Endpoint = endpoint;
        this.Name = name;
        this.Task = task;
    }

    public int getId() {
        return Id;
    }

    public void setId(int id) {
        Id = id;
    }

    public LocalDate getEndpoint() {
        return Endpoint;
    }

    public void setEndpoint(LocalDate endpoint) {
        Endpoint = endpoint;
    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }

    public String getTask() {
        return Task;
    }

    public void setTask(String task) {
        Task = task;
    }

    public boolean isDone() {
        return IsDone;
    }

    public void setDone(boolean done) {
        IsDone = done;
    }
}


