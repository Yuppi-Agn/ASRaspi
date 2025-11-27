package com.AS.Yuppi.Raspi.DataWorkers.DTOs;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ApiSchedule {
    @SerializedName("id")
    private int id; // Только для GET запроса

    @SerializedName("name")
    private String name;
    
    @SerializedName("author")
    private String author;

    @SerializedName("daySchedules")
    private List<ApiDaySchedule> daySchedules;
    
    @SerializedName("hometasks")
    private List<ApiHometask> hometasks;
    
    @SerializedName("notes")
    private List<ApiNote> notes;
    
    @SerializedName("updateDate")
    private String updateDate; // ISO 8601 format string

    @SerializedName("circleMode")
    private int circleMode; // 0=7 дней, 1=14 дней

    @SerializedName("firstWeekId")
    private int firstWeekId; // Четная или нечетная первая неделя (0=чет, 1=нечет)

    @SerializedName("startDate")
    private String startDate; // LocalDate в формате "yyyy-MM-dd"

    @SerializedName("endDate")
    private String endDate; // LocalDate в формате "yyyy-MM-dd"

    @SerializedName("hollidays")
    private List<String> hollidays; // Список дат в формате "yyyy-MM-dd"

    // Конструктор для POST-запроса (без id)
    public ApiSchedule(String name, List<ApiDaySchedule> daySchedules) {
        this.name = name;
        this.daySchedules = daySchedules;
    }
    
    // Полный конструктор
    public ApiSchedule(String name, String author, List<ApiDaySchedule> daySchedules, 
                      List<ApiHometask> hometasks, List<ApiNote> notes, String updateDate,
                      int circleMode, int firstWeekId, String startDate, String endDate, List<String> hollidays) {
        this.name = name;
        this.author = author;
        this.daySchedules = daySchedules;
        this.hometasks = hometasks;
        this.notes = notes;
        this.updateDate = updateDate;
        this.circleMode = circleMode;
        this.firstWeekId = firstWeekId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.hollidays = hollidays;
    }

    // Геттеры и сеттеры
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    
    public List<ApiDaySchedule> getDaySchedules() { return daySchedules; }
    public void setDaySchedules(List<ApiDaySchedule> daySchedules) { this.daySchedules = daySchedules; }
    
    public List<ApiHometask> getHometasks() { return hometasks; }
    public void setHometasks(List<ApiHometask> hometasks) { this.hometasks = hometasks; }
    
    public List<ApiNote> getNotes() { return notes; }
    public void setNotes(List<ApiNote> notes) { this.notes = notes; }
    
    public String getUpdateDate() { return updateDate; }
    public void setUpdateDate(String updateDate) { this.updateDate = updateDate; }
    
    public int getCircleMode() { return circleMode; }
    public void setCircleMode(int circleMode) { this.circleMode = circleMode; }
    
    public int getFirstWeekId() { return firstWeekId; }
    public void setFirstWeekId(int firstWeekId) { this.firstWeekId = firstWeekId; }
    
    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }
    
    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }
    
    public List<String> getHollidays() { return hollidays; }
    public void setHollidays(List<String> hollidays) { this.hollidays = hollidays; }
}
