package com.AS.Yuppi.Raspi.DataWorkers;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
public class Schedules {
    private List<Day_Schedule> Days_Schedule= new ArrayList<>(); //Не нужно, тк не будут использоваться отдельные строки | Upd: НУЖНО, ИЗ-ЗА MAP
    //private List<String> Lessons;
    //private List<String> DayLessons = new ArrayList<>();
    //private List<List<Integer>> Lessons_StartTime = new ArrayList<>();
    //private List<List<Integer>> Lessons_EndTime = new ArrayList<>();
    private Map<LocalDate, Day_Schedule> Special_Days_Shedule= new HashMap<>();// = new HashMap<>();
    private int Circle_Mode=0;//0=7, 1=14
    private int FirstWeekId=0;//Четная или нечетная первая неделя
    private LocalDate Start_Date=LocalDate.of(2025, 9, 11);
    private LocalDate End_Date= LocalDate.of(2025, 9, 11);
    private List<LocalDate> Hollidays= new ArrayList<>();
    private String Name="",
            Author="";
    public Schedules(){
        setCircle_Mode(0);
    }
    public Schedules(int Circle_Mode){
        setCircle_Mode(Circle_Mode);
    }
    public String getDayLesson(int id) {
        //return DayLessons.get(id);
        return Days_Schedule.get(id).get_lesson();
    }
    public void setDayLesson(String Data, int id) {
        //this.DayLessons.set(id, Data);
        Days_Schedule.get(id).set_lesson(Data);
    }
    public Integer getLessons_StartTime(int Day, int id) {
        //return Lessons_StartTime.get(Day).get(id);
        return Days_Schedule.get(Day).get_Lesson_StartTime(id);
    }
    public void setLessons_StartTime(int Day, List<Integer> Data) {
        //this.Lessons_StartTime.set(Day, Data);
        Days_Schedule.get(Day).set_Lessons_StartTime(Data);
    }
    public void setLesson_StartTime(int Day, int id, Integer Data) {
        //this.Lessons_StartTime.get(Day).set(id, Data);
        Days_Schedule.get(Day).set_Lesson_StartTime(id, Data);
    }
    public Integer getLessons_EndTime(int Day, int id) {
        //return Lessons_EndTime.get(Day).get(id);
        return Days_Schedule.get(Day).get_Lesson_EndTime(id);
    }
    public void setLessons_EndTime(int Day, List<Integer> Data) {
        //this.Lessons_EndTime.set(Day, Data);
        Days_Schedule.get(Day).set_Lessons_EndTime(Data);
    }
    public void setLesson_EndTime(int Day, int id, Integer Data) {
        //this.Lessons_EndTime.get(Day).set(id, Data);
        Days_Schedule.get(Day).set_Lesson_EndTime(id, Data);
    }
    public Map<LocalDate, Day_Schedule> getSpecial_Days_Shedule() {
        return Special_Days_Shedule;
    }
    public void setSpecial_Days_Shedule(LocalDate Date, Day_Schedule Data) {
        this.Special_Days_Shedule.put(Date, Data);
    }
    public int getCircle_Mode() {
        return Circle_Mode;
    }
    public void setCircle_Mode(int circle_Mode) {
        Days_Schedule.clear();
        switch (circle_Mode){
            default:
            case 0:
                this.Circle_Mode = 0;
                for(int i=0; i<7; i++)
                    Days_Schedule.add(new Day_Schedule());
                break;
            case 1:
                this.Circle_Mode = 1;
                for(int i=0; i<14; i++)
                    Days_Schedule.add(new Day_Schedule());
                break;
        }
    }
    public int getFirstWeekId() {
        return FirstWeekId;
    }
    public void setFirstWeekId(int Data) {
        FirstWeekId=Data;
    }
    public int countDays_Schedule(){
        return Days_Schedule.size();
    }
    public int countLessons_StartTime(){
        return Days_Schedule.get(0).countLessons_StartTime();
    }
    public LocalDate getStart_Date() {
        return Start_Date;
    }
    public void setStart_Date(LocalDate start_Date) {
        this.Start_Date = start_Date;
    }
    public LocalDate getEnd_Date() {
        return End_Date;
    }
    public void setEnd_Date(LocalDate end_Date) {
        this.End_Date = end_Date;
    }
    public List<LocalDate> getHollidays() {
        return Hollidays;
    }
    public void setHollidays(List<LocalDate> hollidays) {
        this.Hollidays = hollidays;
    }
    public String getName() {
        return Name;
    }
    public void setName(String name) {
        this.Name = name;
    }
    public String getAuthor() {
        return Author;
    }
    public void setAuthor(String author) {
        this.Author = author;
    }

}
