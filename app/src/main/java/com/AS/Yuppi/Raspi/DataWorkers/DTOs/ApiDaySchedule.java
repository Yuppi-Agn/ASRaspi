package com.AS.Yuppi.Raspi.DataWorkers.DTOs;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ApiDaySchedule {
    // Имена полей могут быть любыми, @SerializedName обеспечит правильную связь с JSON
    @SerializedName("id")
    private int id;
    @SerializedName("lessons")
    private String lessons;
    @SerializedName("lessonsStartTime")
    private List<Integer> lessonsStartTime;
    @SerializedName("lessonsEndTime")
    private List<Integer> lessonsEndTime;
    // Геттеры и сеттеры (можно сгенерировать через Alt+Insert)
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getLessons() { return lessons; }
    public void setLessons(String lessons) { this.lessons = lessons; }
    public List<Integer> getLessonsStartTime() { return lessonsStartTime; }
    public void setLessonsStartTime(List<Integer> lessonsStartTime) { this.lessonsStartTime = lessonsStartTime; }
    public List<Integer> getLessonsEndTime() { return lessonsEndTime; }
    public void setLessonsEndTime(List<Integer> lessonsEndTime) { this.lessonsEndTime = lessonsEndTime; }
}
    