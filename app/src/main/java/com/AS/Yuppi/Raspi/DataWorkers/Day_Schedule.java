package com.AS.Yuppi.Raspi.DataWorkers;

import androidx.room.Entity;

import java.util.ArrayList;
import java.util.List;

@Entity
public class Day_Schedule {

    //private List<String> Lessons  = new ArrayList<>();
    private String Lessons="";
    private List<Integer> Lessons_StartTime= new ArrayList<>();
    private List<Integer> Lessons_EndTime= new ArrayList<>();
    public Day_Schedule() {
        for (int i = 0; i < 10; i++) {
            //Lessons.add("");
            Lessons_StartTime.add(0);
            Lessons_EndTime.add(0);
        }
    }
    public String get_lesson() {
        return Lessons;
    }
    public void set_lesson(String lesson) {
        Lessons= lesson;
    }
    public int get_Lesson_StartTime(int index) {
        return Lessons_StartTime.get(index);
    }
    public void set_Lesson_StartTime(int index, int StartTime) { Lessons_StartTime.set(index, StartTime); }
    public void set_Lessons_StartTime(List<Integer> Data) { Lessons_StartTime = Data; }
    public int get_Lesson_EndTime(int index) {
        return Lessons_EndTime.get(index);
    }
    public void set_Lesson_EndTime(int index, int EndTime) { Lessons_EndTime.set(index, EndTime); }
    public void set_Lessons_EndTime(List<Integer> Data) { Lessons_EndTime = Data; }
    public List<Integer> get_Lessons_StartTime() { return Lessons_StartTime; }
    public List<Integer> get_Lessons_EndTime() {
        return Lessons_EndTime;
    }
    public int countLessons_StartTime(){
        return Lessons_StartTime.size();
    }
}
