package com.AS.Yuppi.Raspi.DataWorkers.DTOs;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ApiSchedule {
    @SerializedName("id")
    private int id; // Только для GET запроса

    @SerializedName("name")
    private String name;

    @SerializedName("daySchedules")
    private List<ApiDaySchedule> daySchedules;

    // Конструктор для POST-запроса (без id)
    public ApiSchedule(String name, List<ApiDaySchedule> daySchedules) {
        this.name = name;
        this.daySchedules = daySchedules;
    }

    // Геттеры и сеттеры
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<ApiDaySchedule> getDaySchedules() { return daySchedules; }
    public void setDaySchedules(List<ApiDaySchedule> daySchedules) { this.daySchedules = daySchedules; }
}
