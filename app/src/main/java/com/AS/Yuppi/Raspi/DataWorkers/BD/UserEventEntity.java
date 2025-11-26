package com.AS.Yuppi.Raspi.DataWorkers.BD;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.time.LocalDate;

/**
 * Entity для сохранения пользовательских событий.
 */
@Entity(tableName = "user_events")
public class UserEventEntity {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private LocalDate date;
    private int time;
    private int circleMode;
    private int circleDays;
    private String name;
    private String info;
    private boolean isEnable;

    public UserEventEntity(LocalDate date, int time, int circleMode, int circleDays, String name, String info, boolean isEnable) {
        this.date = date;
        this.time = time;
        this.circleMode = circleMode;
        this.circleDays = circleDays;
        this.name = name;
        this.info = info;
        this.isEnable = isEnable;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public int getCircleMode() {
        return circleMode;
    }

    public void setCircleMode(int circleMode) {
        this.circleMode = circleMode;
    }

    public int getCircleDays() {
        return circleDays;
    }

    public void setCircleDays(int circleDays) {
        this.circleDays = circleDays;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public boolean isEnable() {
        return isEnable;
    }

    public void setEnable(boolean enable) {
        isEnable = enable;
    }
}


