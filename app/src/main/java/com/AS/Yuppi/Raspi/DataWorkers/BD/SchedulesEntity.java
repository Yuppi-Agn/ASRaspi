package com.AS.Yuppi.Raspi.DataWorkers.BD;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.AS.Yuppi.Raspi.DataWorkers.Day_Schedule;
import com.AS.Yuppi.Raspi.DataWorkers.Schedules;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Сущность Room для сохранения основного объекта Schedules.
 */
@Entity(tableName = "schedules_table")
public class SchedulesEntity {

    @PrimaryKey(autoGenerate = true)
    private int id; // Уникальный идентификатор для БД

    // Поля из Schedules.java

    // List<Day_Schedule> Days_Schedule. Так как List<Day_Schedule> - сложный тип,
    // его нужно конвертировать (в нашем случае через GSON в Converters).
    private List<Day_Schedule> Days_Schedule;

    // Map<LocalDate, Day_Schedule> Special_Days_Shedule - также конвертируется
    private Map<LocalDate, Day_Schedule> Special_Days_Shedule;

    private int Circle_Mode;
    private int FirstWeekId;
    private LocalDate Start_Date; // Использует TypeConverter
    private LocalDate End_Date; // Использует TypeConverter
    private List<LocalDate> Hollidays; // Использует TypeConverter

    private String Name;
    private String Author;

    // Новые поля: списки домашних заданий и заметок
    private List<Schedules.Hometask> Hometasks;
    private List<Schedules.Note> Notes;

    // Конструктор для Room (должен соответствовать полям)
    public SchedulesEntity(List<Day_Schedule> Days_Schedule,
                           Map<LocalDate, Day_Schedule> Special_Days_Shedule,
                           int Circle_Mode, int FirstWeekId,
                           LocalDate Start_Date, LocalDate End_Date,
                           List<LocalDate> Hollidays, String Name, String Author,
                           List<Schedules.Hometask> Hometasks,
                           List<Schedules.Note> Notes) {
        this.Days_Schedule = Days_Schedule;
        this.Special_Days_Shedule = Special_Days_Shedule;
        this.Circle_Mode = Circle_Mode;
        this.FirstWeekId = FirstWeekId;
        this.Start_Date = Start_Date;
        this.End_Date = End_Date;
        this.Hollidays = Hollidays;
        this.Name = Name;
        this.Author = Author;
        this.Hometasks = Hometasks;
        this.Notes = Notes;
    }

    // Геттеры и Сеттеры (обязательны для Room!)

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public List<Day_Schedule> getDays_Schedule() { return Days_Schedule; }
    public void setDays_Schedule(List<Day_Schedule> days_Schedule) { Days_Schedule = days_Schedule; }

    public Map<LocalDate, Day_Schedule> getSpecial_Days_Shedule() { return Special_Days_Shedule; }
    public void setSpecial_Days_Shedule(Map<LocalDate, Day_Schedule> special_Days_Shedule) { Special_Days_Shedule = special_Days_Shedule; }

    public int getCircle_Mode() { return Circle_Mode; }
    public void setCircle_Mode(int circle_Mode) { Circle_Mode = circle_Mode; }

    public int getFirstWeekId() { return FirstWeekId; }
    public void setFirstWeekId(int firstWeekId) { FirstWeekId = firstWeekId; }

    public LocalDate getStart_Date() { return Start_Date; }
    public void setStart_Date(LocalDate start_Date) { Start_Date = start_Date; }

    public LocalDate getEnd_Date() { return End_Date; }
    public void setEnd_Date(LocalDate end_Date) { End_Date = end_Date; }

    public List<LocalDate> getHollidays() { return Hollidays; }
    public void setHollidays(List<LocalDate> hollidays) { Hollidays = hollidays; }

    public String getName() { return Name; }
    public void setName(String name) { Name = name; }

    public String getAuthor() { return Author; }
    public void setAuthor(String author) { Author = author; }

    public List<Schedules.Hometask> getHometasks() { return Hometasks; }
    public void setHometasks(List<Schedules.Hometask> hometasks) { Hometasks = hometasks; }

    public List<Schedules.Note> getNotes() { return Notes; }
    public void setNotes(List<Schedules.Note> notes) { Notes = notes; }
}