package com.AS.Yuppi.Raspi.DataWorkers;import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Schedules {
    private List<Day_Schedule> Days_Schedule= new ArrayList<>();
    private Map<LocalDate, Day_Schedule> Special_Days_Shedule= new HashMap<>();
    // Array of hometasks related to this schedule
    private List<Hometask> Hometasks = new ArrayList<>();
    // Array of notes related to this schedule
    private List<Note> Notes = new ArrayList<>();
    private int Circle_Mode=0;//0=7, 1=14
    private int FirstWeekId=0;//Четная или нечетная первая неделя
    private LocalDate Start_Date=LocalDate.of(2025, 9, 11);
    private LocalDate End_Date= LocalDate.of(2025, 9, 11);
    private List<LocalDate> Hollidays= new ArrayList<>();
    private String Name="",
            Author="";
    private Date UpdateDate = new Date(); // Дата и время последнего изменения/создания (DateTime)

    public Schedules(){
        setCircle_Mode(0);
        updateDate(); // Устанавливаем дату создания
    }

    public Schedules(int Circle_Mode){
        setCircle_Mode(Circle_Mode);
        updateDate(); // Устанавливаем дату создания
    }

    // ---------------- Hometask helpers ----------------

    public Hometask addHometask(String lesson, String task, LocalDate endpoint) {
        Hometask hometask = new Hometask(lesson, task, endpoint);
        Hometasks.add(hometask);
        updateDate();
        return hometask;
    }

    public List<Hometask> getHometasks() {
        return Hometasks;
    }

    public void setHometasks(List<Hometask> hometasks) {
        this.Hometasks = hometasks;
        updateDate();
    }

    // ---------------- Notes helpers ----------------

    public Note addNote(String lesson, String data) {
        Note note = new Note(lesson, data);
        Notes.add(note);
        updateDate();
        return note;
    }

    public List<Note> getNotes() {
        return Notes;
    }

    public void setNotes(List<Note> notes) {
        this.Notes = notes;
        updateDate();
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

            // Определяем номер недели относительно Start_Date
            // Math.floorDiv корректно работает с отрицательными числами
            // Для дней до Start_Date weekNumber будет отрицательным
            long weekNumber = Math.floorDiv(daysBetween, 7);
            
            // Определяем четность недели относительно Start_Date
            // Используем Math.floorMod для корректной работы с отрицательными числами
            // weekNumber = 0, 1, 2, 3... -> parity = 0, 1, 0, 1...
            // weekNumber = -1, -2, -3... -> parity = 1, 0, 1, 0...
            int weekParity = (int) Math.floorMod(weekNumber, 2);
            
            // Определяем, является ли текущая неделя первой или второй неделей цикла
            // FirstWeekId определяет, какая неделя цикла соответствует Start_Date (weekNumber = 0)
            // FirstWeekId = 0 означает, что Start_Date - это 1-я неделя цикла (индексы 0-6)
            // FirstWeekId = 1 означает, что Start_Date - это 2-я неделя цикла (индексы 7-13)
            //
            // Логика: Неделя 0 (Start_Date) всегда имеет parity = 0
            // Недели с той же четностью, что и неделя 0 (parity = 0), имеют ту же неделю цикла, что и Start_Date
            // Недели с противоположной четностью (parity = 1) имеют противоположную неделю цикла
            //
            // Пример 1: FirstWeekId = 0 (Start_Date - это 1-я неделя цикла)
            //   weekNumber = 0: parity = 0 → 1-я неделя (индексы 0-6)
            //   weekNumber = 1: parity = 1 → 2-я неделя (индексы 7-13)
            //   weekNumber = -1: parity = 1 → 2-я неделя (индексы 7-13)
            //   weekNumber = -2: parity = 0 → 1-я неделя (индексы 0-6)
            //
            // Пример 2: FirstWeekId = 1 (Start_Date - это 2-я неделя цикла)
            //   weekNumber = 0: parity = 0 → 2-я неделя (индексы 7-13)
            //   weekNumber = 1: parity = 1 → 1-я неделя (индексы 0-6)
            //   weekNumber = -1: parity = 1 → 1-я неделя (индексы 0-6)
            //   weekNumber = -2: parity = 0 → 2-я неделя (индексы 7-13)
            //
            // Формула: 
            // Если parity == 0, то неделя цикла = FirstWeekId (0 = 1-я, 1 = 2-я)
            // Если parity == 1, то неделя цикла = 1 - FirstWeekId (0 = 2-я, 1 = 1-я)
            boolean isFirstWeekOfCycle = (weekParity == 0) ? (FirstWeekId == 0) : (FirstWeekId == 0);
            
            if (isFirstWeekOfCycle) {
                // 1-я неделя цикла: дни 0-6 (индексы 0-6 в Days_Schedule)
                int dayIndex = dayOfWeek;
                if (dayIndex >= 0 && dayIndex < Days_Schedule.size()){
                    return Days_Schedule.get(dayIndex);
                }
            } else {
                // 2-я неделя цикла: дни 7-13 (индексы 7-13 в Days_Schedule)
                int dayIndex = dayOfWeek + 7;
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
        updateDate();
    }
    public LocalDate getEnd_Date() {
        return End_Date;
    }
    public void setEnd_Date(LocalDate end_Date) {
        this.End_Date = end_Date;
        updateDate();
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
        updateDate();
    }
    public String getAuthor() {
        return Author;
    }
    public void setAuthor(String author) {
        this.Author = author;
        updateDate();
    }
    
    public Date getUpdateDate() {
        return UpdateDate;
    }
    
    public void setUpdateDate(Date updateDate) {
        this.UpdateDate = updateDate;
    }
    
    /**
     * Обновляет UpdateDate до текущего времени (с миллисекундами).
     * Вызывается автоматически при любом изменении расписания.
     * Использует Date, который содержит дату и время, что позволяет отслеживать
     * изменения даже в течение одного дня.
     */
    public void updateDate() {
        this.UpdateDate = new Date(); // Date содержит и дату, и время с миллисекундами
    }
    public List<Day_Schedule> getDays_Schedule() {
        return Days_Schedule;
    }
    public void setDays_Schedule(List<Day_Schedule> days_Schedule) {
        Days_Schedule = days_Schedule;
        updateDate();
    }

    /**
     * Извлекает список уникальных предметов из всех Day_Schedule.
     * Парсит строку занятий аналогично HomeFragment.parseDaySchedule.
     */
    public List<String> getSubjectsList() {
        Set<String> subjectsSet = new HashSet<>();
        
        // Парсим все дни расписания
        for (Day_Schedule daySchedule : Days_Schedule) {
            String lessonsRaw = daySchedule.get_lesson();
            if (lessonsRaw == null || lessonsRaw.trim().isEmpty()) {
                continue;
            }
            
            // Разбиваем на блоки занятий (формат "цифра)")
            String[] lessonBlocks = lessonsRaw.split("\\s*(?=\\d+\\))");
            
            for (String block : lessonBlocks) {
                String trimmedBlock = block.trim();
                if (trimmedBlock.isEmpty()) continue;
                
                try {
                    int parenIndex = trimmedBlock.indexOf(')');
                    if (parenIndex < 0) continue;
                    
                    // Извлекаем название предмета
                    String tempSubject = trimmedBlock.substring(parenIndex + 1).trim();
                    
                    // Ищем преподавателя (Ф.И.О. или Ф. И.)
                    Pattern teacherPattern = Pattern.compile("([А-ЯЁ][а-яё]+(?:\\s[А-ЯЁ]\\.){1,2})");
                    Matcher teacherMatcher = teacherPattern.matcher(tempSubject);
                    String teacherName = "";
                    if (teacherMatcher.find()) {
                        teacherName = teacherMatcher.group(1);
                    }
                    
                    // Ищем аудиторию (буква и 3 цифры)
                    Pattern classroomPattern = Pattern.compile("([А-Я]\\d{3})");
                    Matcher classroomMatcher = classroomPattern.matcher(tempSubject);
                    String classroom = "";
                    if (classroomMatcher.find()) {
                        classroom = classroomMatcher.group(1).trim();
                    }
                    
                    // Определяем тип и очищаем название предмета
                    String subjectName = "";
                    if (tempSubject.contains("лек.")) {
                        subjectName = tempSubject.split("лек.")[0].trim();
                    } else if (tempSubject.contains("лаб.")) {
                        subjectName = tempSubject.split("лаб.")[0].trim();
                    } else if (tempSubject.contains("сем.")) {
                        subjectName = tempSubject.split("сем.")[0].trim();
                    } else {
                        // Если тип не указан, берем все до преподавателя или аудитории
                        int endOfSubject = tempSubject.length();
                        if (!teacherName.isEmpty()) {
                            int idx = tempSubject.indexOf(teacherName);
                            if (idx >= 0) endOfSubject = Math.min(endOfSubject, idx);
                        }
                        if (!classroom.isEmpty()) {
                            int idx = tempSubject.indexOf(classroom);
                            if (idx >= 0) endOfSubject = Math.min(endOfSubject, idx);
                        }
                        subjectName = tempSubject.substring(0, endOfSubject).trim();
                    }
                    
                    // Убираем лишние переносы строк и добавляем в множество
                    subjectName = subjectName.replace("\n", " ").trim();
                    if (!subjectName.isEmpty()) {
                        subjectsSet.add(subjectName);
                    }
                } catch (Exception e) {
                    // Игнорируем ошибки парсинга отдельного блока
                }
            }
        }
        
        // Также парсим специальные дни
        for (Day_Schedule daySchedule : Special_Days_Shedule.values()) {
            String lessonsRaw = daySchedule.get_lesson();
            if (lessonsRaw == null || lessonsRaw.trim().isEmpty()) {
                continue;
            }
            
            String[] lessonBlocks = lessonsRaw.split("\\s*(?=\\d+\\))");
            for (String block : lessonBlocks) {
                String trimmedBlock = block.trim();
                if (trimmedBlock.isEmpty()) continue;
                
                try {
                    int parenIndex = trimmedBlock.indexOf(')');
                    if (parenIndex < 0) continue;
                    
                    String tempSubject = trimmedBlock.substring(parenIndex + 1).trim();
                    Pattern teacherPattern = Pattern.compile("([А-ЯЁ][а-яё]+(?:\\s[А-ЯЁ]\\.){1,2})");
                    Matcher teacherMatcher = teacherPattern.matcher(tempSubject);
                    String teacherName = "";
                    if (teacherMatcher.find()) {
                        teacherName = teacherMatcher.group(1);
                    }
                    
                    Pattern classroomPattern = Pattern.compile("([А-Я]\\d{3})");
                    Matcher classroomMatcher = classroomPattern.matcher(tempSubject);
                    String classroom = "";
                    if (classroomMatcher.find()) {
                        classroom = classroomMatcher.group(1).trim();
                    }
                    
                    String subjectName = "";
                    if (tempSubject.contains("лек.")) {
                        subjectName = tempSubject.split("лек.")[0].trim();
                    } else if (tempSubject.contains("лаб.")) {
                        subjectName = tempSubject.split("лаб.")[0].trim();
                    } else if (tempSubject.contains("сем.")) {
                        subjectName = tempSubject.split("сем.")[0].trim();
                    } else {
                        int endOfSubject = tempSubject.length();
                        if (!teacherName.isEmpty()) {
                            int idx = tempSubject.indexOf(teacherName);
                            if (idx >= 0) endOfSubject = Math.min(endOfSubject, idx);
                        }
                        if (!classroom.isEmpty()) {
                            int idx = tempSubject.indexOf(classroom);
                            if (idx >= 0) endOfSubject = Math.min(endOfSubject, idx);
                        }
                        subjectName = tempSubject.substring(0, endOfSubject).trim();
                    }
                    
                    subjectName = subjectName.replace("\n", " ").trim();
                    if (!subjectName.isEmpty()) {
                        subjectsSet.add(subjectName);
                    }
                } catch (Exception e) {
                    // Игнорируем ошибки парсинга
                }
            }
        }
        
        // Возвращаем отсортированный список
        List<String> result = new ArrayList<>(subjectsSet);
        result.sort(String::compareTo);
        return result;
    }

    // ---------------- Inner classes ----------------

    public static class Hometask {
        private String Lesson;
        private String Task;
        private LocalDate Endpoint;
        private boolean IsDone = false;
        private boolean IsPersonal = false;

        public Hometask(String lesson, String task, LocalDate endpoint) {
            this.Lesson = lesson;
            this.Task = task;
            this.Endpoint = endpoint;
        }

        public String getLesson() {
            return Lesson;
        }

        public void setLesson(String lesson) {
            Lesson = lesson;
        }

        public String getTask() {
            return Task;
        }

        public void setTask(String task) {
            Task = task;
        }

        public LocalDate getEndpoint() {
            return Endpoint;
        }

        public void setEndpoint(LocalDate endpoint) {
            Endpoint = endpoint;
        }

        public boolean isDone() {
            return IsDone;
        }

        public void setDone(boolean done) {
            IsDone = done;
        }

        public boolean isPersonal() {
            return IsPersonal;
        }

        public void setPersonal(boolean personal) {
            IsPersonal = personal;
        }
    }

    public static class Note {
        private String Lesson;
        private String Data;
        private boolean IsPersonal = false;

        public Note(String lesson, String data) {
            this.Lesson = lesson;
            this.Data = data;
        }

        public String getLesson() {
            return Lesson;
        }

        public void setLesson(String lesson) {
            Lesson = lesson;
        }

        public String getData() {
            return Data;
        }

        public void setData(String data) {
            Data = data;
        }

        public boolean isPersonal() {
            return IsPersonal;
        }

        public void setPersonal(boolean personal) {
            IsPersonal = personal;
        }
    }
}
