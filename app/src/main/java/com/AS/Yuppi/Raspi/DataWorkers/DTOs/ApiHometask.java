package com.AS.Yuppi.Raspi.DataWorkers.DTOs;

import com.google.gson.annotations.SerializedName;
import java.time.LocalDate;

public class ApiHometask {
    @SerializedName("lesson")
    private String lesson;
    
    @SerializedName("task")
    private String task;
    
    @SerializedName("endpoint")
    private String endpoint; // ISO 8601 format string
    
    @SerializedName("isDone")
    private boolean isDone;
    
    @SerializedName("isPersonal")
    private boolean isPersonal;
    
    // Конструкторы
    public ApiHometask() {}
    
    public ApiHometask(String lesson, String task, LocalDate endpoint, boolean isDone, boolean isPersonal) {
        this.lesson = lesson;
        this.task = task;
        this.endpoint = endpoint != null ? endpoint.toString() : null;
        this.isDone = isDone;
        this.isPersonal = isPersonal;
    }
    
    // Геттеры и сеттеры
    public String getLesson() { return lesson; }
    public void setLesson(String lesson) { this.lesson = lesson; }
    
    public String getTask() { return task; }
    public void setTask(String task) { this.task = task; }
    
    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    
    public LocalDate getEndpointAsLocalDate() {
        if (endpoint == null || endpoint.isEmpty()) {
            return null;
        }
        try {
            // Парсим дату в формате "yyyy-MM-dd"
            return LocalDate.parse(endpoint);
        } catch (Exception e) {
            // Логируем ошибку, но не падаем
            android.util.Log.e("ApiHometask", "Ошибка парсинга endpoint: " + endpoint + ", ошибка: " + e.getMessage());
            return null;
        }
    }
    
    public boolean isDone() { return isDone; }
    public void setDone(boolean done) { isDone = done; }
    
    public boolean isPersonal() { return isPersonal; }
    public void setPersonal(boolean personal) { isPersonal = personal; }
}

