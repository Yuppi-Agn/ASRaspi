package com.AS.Yuppi.Raspi.DataWorkers;

import java.time.LocalDate;

/**
 * Модель пользовательского события.
 */
public class UserEvents {
    private int Id;            // идентификатор в БД
    private LocalDate Date;   // дата начала события
    private int Time;         // время в минутах от 00:00
    private int Circle_Mode;  // 0=один раз, 1=еженедельно, 2=через неделю, 3=каждые Circle_Days дней
    private int Circle_Days;  // количество дней между событиями (для режима 3)
    private String Name;      // название события
    private String Info;      // дополнительная информация
    private boolean isEnable = true; // включено ли событие для показа

    public UserEvents(LocalDate date, int time, int circle_Mode, int circle_Days, String name, String info) {
        this.Date = date;
        this.Time = time;
        this.Circle_Mode = circle_Mode;
        this.Circle_Days = circle_Days;
        this.Name = name;
        this.Info = info;
    }

    public int getId() {
        return Id;
    }

    public void setId(int id) {
        Id = id;
    }

    public LocalDate getDate() {
        return Date;
    }

    public void setDate(LocalDate date) {
        Date = date;
    }

    public int getTime() {
        return Time;
    }

    public void setTime(int time) {
        Time = time;
    }

    public int getCircle_Mode() {
        return Circle_Mode;
    }

    public void setCircle_Mode(int circle_Mode) {
        Circle_Mode = circle_Mode;
    }

    public int getCircle_Days() {
        return Circle_Days;
    }

    public void setCircle_Days(int circle_Days) {
        Circle_Days = circle_Days;
    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }

    public String getInfo() {
        return Info;
    }

    public void setInfo(String info) {
        Info = info;
    }

    public boolean isEnable() {
        return isEnable;
    }

    public void setEnable(boolean enable) {
        isEnable = enable;
    }
}


