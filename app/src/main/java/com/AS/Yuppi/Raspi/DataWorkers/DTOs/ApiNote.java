package com.AS.Yuppi.Raspi.DataWorkers.DTOs;

import com.google.gson.annotations.SerializedName;

public class ApiNote {
    @SerializedName("lesson")
    private String lesson;
    
    @SerializedName("data")
    private String data;
    
    @SerializedName("isPersonal")
    private boolean isPersonal;
    
    // Конструкторы
    public ApiNote() {}
    
    public ApiNote(String lesson, String data, boolean isPersonal) {
        this.lesson = lesson;
        this.data = data;
        this.isPersonal = isPersonal;
    }
    
    // Геттеры и сеттеры
    public String getLesson() { return lesson; }
    public void setLesson(String lesson) { this.lesson = lesson; }
    
    public String getData() { return data; }
    public void setData(String data) { this.data = data; }
    
    public boolean isPersonal() { return isPersonal; }
    public void setPersonal(boolean personal) { isPersonal = personal; }
}

