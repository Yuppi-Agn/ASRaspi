package com.AS.Yuppi.Raspi.DataWorkers;import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Schedules {
    private List<Day_Schedule> Days_Schedule= new ArrayList<>();
    private Map<LocalDate, Day_Schedule> Special_Days_Shedule= new HashMap<>();
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
    public Day_Schedule getScheduleForDayOffset(int offset) {
        LocalDate targetDate = LocalDate.now().plusDays(offset);

        // Этапы 1, 2 и 3 остаются без изменений:
        // 1. Проверка на особое расписание
        if (Special_Days_Shedule.containsKey(targetDate)) {
            return Special_Days_Shedule.get(targetDate);
        }
        // 2. Проверка на выходной
        if (Hollidays.contains(targetDate)) {
            return new Day_Schedule(); // Возвращаем пустой день
        }
        // 3. Проверка, входит ли дата в общий диапазон расписания
        if (Start_Date == null || End_Date == null || targetDate.isBefore(Start_Date) || targetDate.isAfter(End_Date)) {
            //return null; // Возвращаем null, если дата вне диапазона
        }
        if (Days_Schedule == null || Days_Schedule.isEmpty()) {
            return null; // Расписание не заполнено
        }

        // --- 4. ГЛАВНОЕ ИСПРАВЛЕНИЕ: МАТЕМАТИЧЕСКИ КОРРЕКТНЫЙ РАСЧЕТ ИНДЕКСА ---

        // Считаем количество дней от даты начала до целевой даты
        long daysBetween = ChronoUnit.DAYS.between(Start_Date, targetDate);

        if (Circle_Mode == 0) { // 7-дневный цикл
            // Корректный расчет остатка для положительных и отрицательных чисел
            int dayIndex = (int) Math.floorMod(daysBetween, 7);
            if (dayIndex >= 0 && dayIndex < Days_Schedule.size()) {
                return Days_Schedule.get(dayIndex);
            }
        } else { // 14-дневный цикл (Circle_Mode == 1)
            // Корректный расчет номера недели и дня недели
            int dayOfWeek = targetDate.getDayOfWeek().getValue() - 1; // 0=Пн, 6=Вс

            // Определяем четность недели относительно Start_Date
            // Math.floorDiv корректно работает с отрицательными числами
            long weekNumber = Math.floorDiv(daysBetween, 7);
            int currentWeekParity = (int) Math.floorMod(weekNumber, 2);

            // FirstWeekId - это четность *первой* недели (0=чет, 1=нечет)
            // Если четность текущей недели совпадает с четностью первой, это 1-я неделя цикла
            if (currentWeekParity == 0) { // 1-я, 3-я, 5-я... неделя цикла
                int dayIndex = dayOfWeek + (FirstWeekId == 0 ? 0 : 7);
                if (dayIndex >= 0 && dayIndex < Days_Schedule.size()){
                    return Days_Schedule.get(dayIndex);
                }
            } else { // 2-я, 4-я, 6-я... неделя цикла
                int dayIndex = dayOfWeek + (FirstWeekId == 0 ? 7 : 0);
                if (dayIndex >= 0 && dayIndex < Days_Schedule.size()){
                    return Days_Schedule.get(dayIndex);
                }
            }
        }
        return null;
    }
    public Integer getStartTimeForDayOffset(int offset, int lessonId) {
        Day_Schedule daySchedule = getScheduleForDayOffset(offset);
        if (daySchedule != null && lessonId >= 0 && lessonId < daySchedule.countLessons_StartTime()) {
            return daySchedule.get_Lesson_StartTime(lessonId);
        }
        return null; // Возвращаем null, если день или урок не найден
    }
    public Integer getEndTimeForDayOffset(int offset, int lessonId) {
        Day_Schedule daySchedule = getScheduleForDayOffset(offset);
        if (daySchedule != null && lessonId >= 0 && lessonId < daySchedule.countLessons_StartTime()) {
            // Возвращаем время ОКОНЧАНИЯ, как и следует из названия функции
            return daySchedule.get_Lesson_EndTime(lessonId);
        }
        return null; // Возвращаем null, если день или урок не найден
    }
    public String getDayLesson(int id) {
        return Days_Schedule.get(id).get_lesson();
    }
    public void setDayLesson(String Data, int id) {
        Days_Schedule.get(id).set_lesson(Data);
    }
    public Integer getLessons_StartTime(int Day, int id) {
        return Days_Schedule.get(Day).get_Lesson_StartTime(id);
    }
    public void setLessons_StartTime(int Day, List<Integer> Data) {
        Days_Schedule.get(Day).set_Lessons_StartTime(Data);
    }
    public void setLesson_StartTime(int Day, int id, Integer Data) {
        Days_Schedule.get(Day).set_Lesson_StartTime(id, Data);
    }
    public Integer getLessons_EndTime(int Day, int id) {
        return Days_Schedule.get(Day).get_Lesson_EndTime(id);
    }
    public void setLessons_EndTime(int Day, List<Integer> Data) {
        Days_Schedule.get(Day).set_Lessons_EndTime(Data);
    }
    public void setLesson_EndTime(int Day, int id, Integer Data) {
        Days_Schedule.get(Day).set_Lesson_EndTime(id, Data);
    }
    public Map<LocalDate, Day_Schedule> getSpecial_Days_Shedule() {
        return Special_Days_Shedule;
    }
    public void setSpecial_Days_Shedule(Map<LocalDate, Day_Schedule> special_Days_Shedule) {
        this.Special_Days_Shedule = special_Days_Shedule;
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
        if (!Days_Schedule.isEmpty()) {
            return Days_Schedule.get(0).countLessons_StartTime();
        }
        return 0;
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
    public List<Day_Schedule> getDays_Schedule() {
        return Days_Schedule;
    }
    public void setDays_Schedule(List<Day_Schedule> days_Schedule) {
        Days_Schedule = days_Schedule;
    }
}
