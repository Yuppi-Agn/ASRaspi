package com.AS.Yuppi.Raspi.DataWorkers;

import android.content.Context;
import android.content.SharedPreferences;

import com.AS.Yuppi.Raspi.DataWorkers.BD.ScheduleNameAuthor;
import com.AS.Yuppi.Raspi.DataWorkers.BD.ScheduleRepository;
import com.AS.Yuppi.Raspi.DataWorkers.BD.SchedulesEntity;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import com.AS.Yuppi.Raspi.DataWorkers.DTOs.ApiSchedule;
import com.AS.Yuppi.Raspi.DataWorkers.DTOs.ApiDaySchedule;
import com.AS.Yuppi.Raspi.DataWorkers.DTOs.ApiHometask;
import com.AS.Yuppi.Raspi.DataWorkers.DTOs.ApiNote;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import android.util.Log;

public class SchedulelController{
    private List<String> SchedulesList= new ArrayList<String>();
    private Schedules CurrentSchedule, editableSchedule;
    private final ScheduleRepository repository;
    private static final String PREFS_NAME = "SchedulePreferences";
    private static final String KEY_SELECTED_SCHEDULES = "selected_schedules";
    private Context context;
    
    //public SchedulelController(){}
    public SchedulelController(ScheduleRepository repository){
        this.repository = repository;
    }
    
    public void setContext(Context context) {
        this.context = context;
    }
    public Schedules geteditableSchedule(){
        if(editableSchedule== null)
            editableSchedule=new Schedules();

        editableSchedule.setAuthor(MySingleton.getHashedDeviceId());
        return editableSchedule;
    }
    public Schedules createeditableSchedule(){
        editableSchedule=new Schedules();
        editableSchedule.setAuthor(MySingleton.getHashedDeviceId());
        return editableSchedule;
    }
    public Schedules seteditableSchedule(Schedules Data){
        editableSchedule=Data;
        return editableSchedule;
    }
    public void saveEditableSchedule(){
        /*
        if(editableSchedule==null) return;
        CurrentSchedule=editableSchedule;
        editableSchedule=null;

        notifyAction("Update");*/

        if(editableSchedule==null) return;

        // 3) Сохраняет обьект editableSchedule в бд с внутренними классами "Day_Schedule".
        saveEditableScheduleToDB();

        CurrentSchedule=editableSchedule;
        editableSchedule=null;

        notifyAction("Update");
    }
    /**
     * 1) Заполняет список SchedulesList данными из БД.
     * Проходится по всем сохраненным классам Schedules и берет оттуда "Name" и "Author",
     * суммирует в формате "Author-Name" и добавляет в этот список.
     *
     * @return
     */
    public List<String> fillSchedulesListFromDB() {
        // Репозиторий выполнит синхронный запрос в фоновом потоке
        List<ScheduleNameAuthor> projectionList = repository.getScheduleNamesAndAuthorsSync();

        SchedulesList.clear();
        for (ScheduleNameAuthor item : projectionList) {
            SchedulesList.add(item.toString());
        }
        System.out.println("\n--- " + " (Элементов: " + SchedulesList.size() + ") ---");
        for (int i = 0; i < SchedulesList.size(); i++) {
            System.out.println("  [" + i + "]: " + SchedulesList.get(i));
        }

        return SchedulesList;

        //notifyAction("SchedulesListUpdated");
    }

    /**
     * 2) Берет строку формата "Author-Name", находит ее в бд и
     * сохраняет полный обьект Schedules (с внутренними классами) в CurrentSchedule.
     * @param authorNameString Строка в формате "Author-Name".
     */
    public void loadCurrentSchedule(String authorNameString) {
        if (authorNameString == null || authorNameString.isEmpty()) return;

        // 1. Извлечь Name из строки "Author-Name"
        // Name всегда находится после первого символа '-'
        String[] parts = authorNameString.split("-", 2);
        if (parts.length < 2) return;
        // Убираем возможные пробелы вокруг Name
        String scheduleName = parts[1].trim();

        // 2. Получить полный SchedulesEntity из БД синхронно
        SchedulesEntity entity = repository.getScheduleByNameSync(scheduleName);

        if (entity != null) {
            // 3. Преобразовать Entity в POJO Schedules с вложенными классами
            CurrentSchedule = ScheduleMapper.toPojo(entity);
            repository.saveLastActiveScheduleName(authorNameString);
            notifyAction("CurrentScheduleLoaded");
        }
    }

    /**
     * 3) Сохраняет обьект editableSchedule в бд с внутренними классами "Day_Schedule".
     * Вложенные классы сохраняются благодаря Type Converters, которые преобразуют их в JSON.
     */
    public void saveEditableScheduleToDB() {
        if (editableSchedule == null) return;

        // Обновляем дату изменения перед сохранением
        editableSchedule.updateDate();

        // 1. Преобразовать POJO Schedules в Entity
        SchedulesEntity entity = ScheduleMapper.toEntity(editableSchedule);

        // 2. Если расписание уже существует в БД, получаем его ID для обновления
        String scheduleName = editableSchedule.getName();
        if (scheduleName != null && !scheduleName.isEmpty()) {
            SchedulesEntity existingEntity = repository.getScheduleByNameSync(scheduleName);
            if (existingEntity != null) {
                // Используем существующий ID для обновления
                entity.setId(existingEntity.getId());
            }
        }

        // 3. Вставить/Обновить Entity в БД синхронно, чтобы изменения сразу были видны
        // OnConflictStrategy.REPLACE обновит существующее расписание по ID или Name+Author
        repository.insertSync(entity);

        // 4. Обновляем CurrentSchedule из editableSchedule (уже содержит актуальные данные)
        if (editableSchedule != null) {
            CurrentSchedule = editableSchedule;
        }

        notifyAction("ScheduleSaved");
    }

    /**
     * Удаляет расписание по строке формата \"Author-Name\".
     */
    public void deleteSchedule(String authorNameString) {
        if (authorNameString == null || authorNameString.isEmpty()) return;

        String[] parts = authorNameString.split("-", 2);
        if (parts.length < 2) return;
        String scheduleName = parts[1].trim();

        SchedulesEntity entity = repository.getScheduleByNameSync(scheduleName);
        if (entity != null) {
            repository.delete(entity);

            // Если удалили текущее расписание — очищаем ссылку
            if (CurrentSchedule != null && scheduleName.equals(CurrentSchedule.getName())) {
                CurrentSchedule = null;
            }
            notifyAction("ScheduleDeleted");
        }
    }
    public Schedules loadLastUsedScheduleOnStartup() {
        // 1. Читаем сохраненную строку "Author-Name"
        String lastScheduleName = repository.getLastActiveScheduleNameSync();

        if (lastScheduleName == null || lastScheduleName.isEmpty()) {
            // Ничего не сохранено в БД (первый запуск).
            CurrentSchedule = null;
            return null; // Возвращаем null, как вы и просили
        }

        // 2. Используем существующую функцию для загрузки полного объекта
        // loadCurrentSchedule сам обновит CurrentSchedule, если найдет расписание.
        loadCurrentSchedule(lastScheduleName);

        // 3. Возвращаем текущее расписание (которое могло быть обновлено)
        return CurrentSchedule;
    }
    public Schedules getCurrentSchedule(){
        return CurrentSchedule;
    }
    public void setCurrentSchedule(Schedules data){
         CurrentSchedule=data;
    }
    public Schedules createCurrentSchedule(){
        return CurrentSchedule=new Schedules();
    }
    public interface OnActionListener {
        void onAction(String data);
    }
    private final List<OnActionListener> listeners = new ArrayList<>();
    public void addOnActionListener(OnActionListener listener) {
        listeners.add(listener);
    }
    public void removeOnActionListener(OnActionListener listener) {
        listeners.remove(listener);
    }
    public void notifyAction(String data) {
        for (OnActionListener listener : listeners) {
            listener.onAction(data);
        }
    }

    public boolean importScheduleFromJson(Context context, String fileName) {
        File file = new File(context.getFilesDir(), fileName);
        if (!file.exists()) {
            System.err.println("Ошибка импорта: Файл " + fileName + " не найден.");
            return false;
        }

        StringBuilder jsonContent = new StringBuilder();
        BufferedReader reader = null;
        try {
            // 1. Читаем контент файла
            reader = new BufferedReader(new InputStreamReader(context.openFileInput(fileName)));
            String line;
            while ((line = reader.readLine()) != null) {
                jsonContent.append(line);
            }

            // 2. Используем конвертер для создания объекта Schedules
            Schedules importedSchedule = JsonConverter.fromJson(jsonContent.toString());

            if (importedSchedule != null) {
                // 3. Устанавливаем импортированное расписание как текущее
                CurrentSchedule = importedSchedule;
                notifyAction("CurrentScheduleLoaded");
                System.out.println("Расписание успешно импортировано из: " + fileName);
                return true;
            }

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Ошибка при чтении файла JSON: " + e.getMessage());
        } finally {
            try {
                if (reader != null) reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * Получить список выбранных расписаний (в формате "Author-Name").
     */
    public List<String> getSelectedSchedules() {
        if (context == null) return new ArrayList<>();
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> selectedSet = prefs.getStringSet(KEY_SELECTED_SCHEDULES, new HashSet<>());
        return new ArrayList<>(selectedSet);
    }

    /**
     * Установить список выбранных расписаний.
     */
    public void setSelectedSchedules(List<String> selectedSchedules) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putStringSet(KEY_SELECTED_SCHEDULES, new HashSet<>(selectedSchedules));
        editor.apply();
        notifyAction("SelectedSchedulesUpdated");
    }

    /**
     * Добавить расписание в список выбранных.
     */
    public void addSelectedSchedule(String authorNameString) {
        if (context == null || authorNameString == null || authorNameString.isEmpty()) return;
        List<String> selected = getSelectedSchedules();
        if (!selected.contains(authorNameString)) {
            selected.add(authorNameString);
            setSelectedSchedules(selected);
            // Уведомляем об изменении выбранных расписаний
            notifyAction("SelectedSchedulesUpdated");
        }
    }

    /**
     * Удалить расписание из списка выбранных.
     */
    public void removeSelectedSchedule(String authorNameString) {
        if (context == null || authorNameString == null || authorNameString.isEmpty()) return;
        List<String> selected = getSelectedSchedules();
        boolean removed = selected.remove(authorNameString);
        if (removed) {
            setSelectedSchedules(selected);
            // Уведомляем об изменении выбранных расписаний
            notifyAction("SelectedSchedulesUpdated");
        }
    }

    /**
     * Проверить, выбрано ли расписание.
     */
    public boolean isScheduleSelected(String authorNameString) {
        if (context == null || authorNameString == null || authorNameString.isEmpty()) return false;
        return getSelectedSchedules().contains(authorNameString);
    }

    /**
     * Получить список объектов Schedules для всех выбранных расписаний.
     */
    public List<Schedules> getSelectedSchedulesObjects() {
        List<Schedules> result = new ArrayList<>();
        List<String> selected = getSelectedSchedules();
        for (String authorNameString : selected) {
            String[] parts = authorNameString.split("-", 2);
            if (parts.length < 2) continue;
            String scheduleName = parts[1].trim();
            SchedulesEntity entity = repository.getScheduleByNameSync(scheduleName);
            if (entity != null) {
                result.add(ScheduleMapper.toPojo(entity));
            }
        }
        return result;
    }
    
    // ==================== Методы синхронизации с сервером ====================
    
    /**
     * Получает все расписания из БД и конвертирует их в POJO объекты Schedules.
     * @return Список всех расписаний.
     */
    private List<Schedules> getAllSchedulesObjects() {
        List<SchedulesEntity> entities = repository.getAllSchedulesSync();
        List<Schedules> schedules = new ArrayList<>();
        
        for (SchedulesEntity entity : entities) {
            Schedules schedule = ScheduleMapper.toPojo(entity);
            if (schedule != null) {
                schedules.add(schedule);
            }
        }
        
        return schedules;
    }
    
    /**
     * Конвертирует Schedules в ApiSchedule для отправки на сервер.
     */
    private ApiSchedule convertToApiSchedule(Schedules schedule) {
        if (schedule == null) return null;
        
        // Конвертируем Day_Schedule в ApiDaySchedule
        List<ApiDaySchedule> apiDays = new ArrayList<>();
        List<Day_Schedule> localDays = schedule.getDays_Schedule();
        if (localDays != null) {
            for (int i = 0; i < localDays.size(); i++) {
                Day_Schedule localDay = localDays.get(i);
                ApiDaySchedule apiDay = new ApiDaySchedule();
                apiDay.setId(i);
                apiDay.setLessons(localDay.get_lesson());
                apiDay.setLessonsStartTime(localDay.get_Lessons_StartTime());
                apiDay.setLessonsEndTime(localDay.get_Lessons_EndTime());
                apiDays.add(apiDay);
            }
        }
        
        // Конвертируем Hometasks (только учебные, личные не отправляем)
        List<ApiHometask> apiHometasks = new ArrayList<>();
        List<Schedules.Hometask> localHometasks = schedule.getHometasks();
        Log.d("SchedulelController", "convertToApiSchedule: конвертация Hometasks для " + schedule.getName() + 
            ", всего заданий: " + (localHometasks != null ? localHometasks.size() : 0));
        if (localHometasks != null) {
            for (Schedules.Hometask hometask : localHometasks) {
                if (hometask != null) {
                    Log.d("SchedulelController", "  Hometask: lesson=" + hometask.getLesson() + 
                        ", task=" + hometask.getTask() + 
                        ", isPersonal=" + hometask.isPersonal() + 
                        ", isDone=" + hometask.isDone());
                    // Отправляем только учебные задания (не личные)
                    if (!hometask.isPersonal()) {
                        apiHometasks.add(new ApiHometask(
                            hometask.getLesson(),
                            hometask.getTask(),
                            hometask.getEndpoint(),
                            hometask.isDone(),
                            false // Всегда false, так как личные не отправляем
                        ));
                        Log.d("SchedulelController", "    -> Добавлено в ApiSchedule (учебное)");
                    } else {
                        Log.d("SchedulelController", "    -> Пропущено (личное задание)");
                    }
                }
            }
        }
        Log.d("SchedulelController", "convertToApiSchedule: сконвертировано заданий: " + apiHometasks.size());
        
        // Конвертируем Notes (только учебные, личные не отправляем)
        List<ApiNote> apiNotes = new ArrayList<>();
        List<Schedules.Note> localNotes = schedule.getNotes();
        Log.d("SchedulelController", "convertToApiSchedule: конвертация Notes для " + schedule.getName() + 
            ", всего заметок: " + (localNotes != null ? localNotes.size() : 0));
        if (localNotes != null) {
            for (Schedules.Note note : localNotes) {
                if (note != null) {
                    Log.d("SchedulelController", "  Note: lesson=" + note.getLesson() + 
                        ", data=" + note.getData() + 
                        ", isPersonal=" + note.isPersonal());
                    // Отправляем только учебные заметки (не личные)
                    if (!note.isPersonal()) {
                        apiNotes.add(new ApiNote(
                            note.getLesson(),
                            note.getData(),
                            false // Всегда false, так как личные не отправляем
                        ));
                        Log.d("SchedulelController", "    -> Добавлено в ApiSchedule (учебная)");
                    } else {
                        Log.d("SchedulelController", "    -> Пропущено (личная заметка)");
                    }
                }
            }
        }
        Log.d("SchedulelController", "convertToApiSchedule: сконвертировано заметок: " + apiNotes.size());
        
        // Форматируем UpdateDate в ISO 8601 формат с временем и миллисекундами
        String updateDateStr = null;
        if (schedule.getUpdateDate() != null) {
            // Используем UTC время для корректной синхронизации
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
            sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            updateDateStr = sdf.format(schedule.getUpdateDate());
        }
        
        // Конвертируем даты в строки
        String startDateStr = schedule.getStart_Date() != null ? schedule.getStart_Date().toString() : null;
        String endDateStr = schedule.getEnd_Date() != null ? schedule.getEnd_Date().toString() : null;
        
        // Конвертируем список праздников в список строк
        List<String> hollidaysStr = new ArrayList<>();
        if (schedule.getHollidays() != null) {
            for (LocalDate holiday : schedule.getHollidays()) {
                if (holiday != null) {
                    hollidaysStr.add(holiday.toString());
                }
            }
        }
        
        return new ApiSchedule(
            schedule.getName(),
            schedule.getAuthor(),
            apiDays,
            apiHometasks,
            apiNotes,
            updateDateStr,
            schedule.getCircle_Mode(),
            schedule.getFirstWeekId(),
            startDateStr,
            endDateStr,
            hollidaysStr
        );
    }
    
    /**
     * Конвертирует ApiSchedule в Schedules для использования в приложении.
     * @param apiSchedule Расписание с сервера
     * @param existingSchedule Существующее локальное расписание (для сохранения статусов выполнения заданий). Если null, все задания считаются невыполненными.
     */
    private Schedules convertFromApiSchedule(ApiSchedule apiSchedule, Schedules existingSchedule) {
        if (apiSchedule == null) {
            Log.e("SchedulelController", "convertFromApiSchedule: apiSchedule is null");
            return null;
        }
        
        Log.d("SchedulelController", "Конвертация расписания: " + apiSchedule.getName() + 
            ", автор: " + apiSchedule.getAuthor() + 
            ", DaySchedules: " + (apiSchedule.getDaySchedules() != null ? apiSchedule.getDaySchedules().size() : 0) +
            ", Hometasks: " + (apiSchedule.getHometasks() != null ? apiSchedule.getHometasks().size() : 0) +
            ", Notes: " + (apiSchedule.getNotes() != null ? apiSchedule.getNotes().size() : 0));
        
        Schedules schedule = new Schedules();
        schedule.setName(apiSchedule.getName() != null ? apiSchedule.getName() : "");
        schedule.setAuthor(apiSchedule.getAuthor() != null ? apiSchedule.getAuthor() : MySingleton.getHashedDeviceId());
        
        // Устанавливаем Circle_Mode и FirstWeekId
        schedule.setCircle_Mode(apiSchedule.getCircleMode());
        schedule.setFirstWeekId(apiSchedule.getFirstWeekId());
        
        // Парсим даты
        if (apiSchedule.getStartDate() != null && !apiSchedule.getStartDate().isEmpty()) {
            try {
                LocalDate startDate = LocalDate.parse(apiSchedule.getStartDate());
                schedule.setStart_Date(startDate);
            } catch (Exception e) {
                Log.e("SchedulelController", "Ошибка парсинга StartDate: " + apiSchedule.getStartDate(), e);
            }
        }
        
        if (apiSchedule.getEndDate() != null && !apiSchedule.getEndDate().isEmpty()) {
            try {
                LocalDate endDate = LocalDate.parse(apiSchedule.getEndDate());
                schedule.setEnd_Date(endDate);
            } catch (Exception e) {
                Log.e("SchedulelController", "Ошибка парсинга EndDate: " + apiSchedule.getEndDate(), e);
            }
        }
        
        // Парсим список праздников
        if (apiSchedule.getHollidays() != null) {
            List<LocalDate> hollidays = new ArrayList<>();
            for (String holidayStr : apiSchedule.getHollidays()) {
                if (holidayStr != null && !holidayStr.isEmpty()) {
                    try {
                        LocalDate holiday = LocalDate.parse(holidayStr);
                        hollidays.add(holiday);
                    } catch (Exception e) {
                        Log.e("SchedulelController", "Ошибка парсинга Holiday: " + holidayStr, e);
                    }
                }
            }
            schedule.setHollidays(hollidays);
        }
        
        // Конвертируем ApiDaySchedule в Day_Schedule
        List<Day_Schedule> days = new ArrayList<>();
        if (apiSchedule.getDaySchedules() != null) {
            for (ApiDaySchedule apiDay : apiSchedule.getDaySchedules()) {
                if (apiDay != null) {
                    Day_Schedule day = new Day_Schedule();
                    day.set_lesson(apiDay.getLessons() != null ? apiDay.getLessons() : "");
                    day.set_Lessons_StartTime(apiDay.getLessonsStartTime() != null ? apiDay.getLessonsStartTime() : new ArrayList<>());
                    day.set_Lessons_EndTime(apiDay.getLessonsEndTime() != null ? apiDay.getLessonsEndTime() : new ArrayList<>());
                    days.add(day);
                    Log.d("SchedulelController", "Добавлен Day_Schedule: lessons=" + day.get_lesson() + 
                        ", startTimes=" + day.get_Lessons_StartTime().size() + 
                        ", endTimes=" + day.get_Lessons_EndTime().size());
                }
            }
        }
        schedule.setDays_Schedule(days);
        
        // Конвертируем ApiHometask в Hometask (только учебные, личные не принимаем)
        if (apiSchedule.getHometasks() != null) {
            for (ApiHometask apiHometask : apiSchedule.getHometasks()) {
                if (apiHometask != null) {
                    // Пропускаем личные задания с сервера (их не должно быть, но на всякий случай)
                    if (apiHometask.isPersonal()) {
                        Log.w("SchedulelController", "Пропущено личное задание с сервера: " + apiHometask.getLesson());
                        continue;
                    }
                    try {
                        LocalDate endpoint = apiHometask.getEndpointAsLocalDate();
                        Schedules.Hometask hometask = schedule.addHometask(
                            apiHometask.getLesson() != null ? apiHometask.getLesson() : "",
                            apiHometask.getTask() != null ? apiHometask.getTask() : "",
                            endpoint
                        );
                        hometask.setPersonal(false); // Всегда false, так как личные не принимаем с сервера
                        
                        // Логика сохранения статуса выполнения:
                        // Если это обновление и такое задание уже есть в локальном расписании, сохраняем локальный статус
                        // Если это новое задание или полная загрузка, считаем невыполненным
                        boolean isDone = false;
                        if (existingSchedule != null && existingSchedule.getHometasks() != null) {
                            // Ищем соответствующее задание в существующем расписании
                            for (Schedules.Hometask existingHt : existingSchedule.getHometasks()) {
                                if (existingHt != null && !existingHt.isPersonal() &&
                                    existingHt.getLesson() != null && existingHt.getLesson().equals(hometask.getLesson()) &&
                                    existingHt.getTask() != null && existingHt.getTask().equals(hometask.getTask()) &&
                                    ((existingHt.getEndpoint() == null && endpoint == null) ||
                                     (existingHt.getEndpoint() != null && endpoint != null && existingHt.getEndpoint().equals(endpoint)))) {
                                    // Нашли существующее задание - сохраняем его статус выполнения
                                    isDone = existingHt.isDone();
                                    Log.d("SchedulelController", "Сохранен локальный статус выполнения для задания: " + 
                                        hometask.getLesson() + " - " + hometask.getTask() + ", isDone=" + isDone);
                                    break;
                                }
                            }
                        }
                        // Если задание не найдено в существующем расписании, isDone остается false (невыполненное)
                        hometask.setDone(isDone);
                        
                        Log.d("SchedulelController", "Добавлен Hometask: lesson=" + apiHometask.getLesson() + 
                            ", task=" + apiHometask.getTask() + 
                            ", endpoint=" + endpoint + 
                            ", isDone=" + isDone + 
                            ", isPersonal=false (учебное)");
                    } catch (Exception e) {
                        Log.e("SchedulelController", "Ошибка при добавлении Hometask: " + e.getMessage(), e);
                    }
                }
            }
        }
        
        // Конвертируем ApiNote в Note (только учебные, личные не принимаем)
        if (apiSchedule.getNotes() != null) {
            for (ApiNote apiNote : apiSchedule.getNotes()) {
                if (apiNote != null) {
                    // Пропускаем личные заметки с сервера (их не должно быть, но на всякий случай)
                    if (apiNote.isPersonal()) {
                        Log.w("SchedulelController", "Пропущена личная заметка с сервера: " + apiNote.getLesson());
                        continue;
                    }
                    try {
                        Schedules.Note note = schedule.addNote(
                            apiNote.getLesson() != null ? apiNote.getLesson() : "",
                            apiNote.getData() != null ? apiNote.getData() : ""
                        );
                        note.setPersonal(false); // Всегда false, так как личные не принимаем с сервера
                        Log.d("SchedulelController", "Добавлен Note: lesson=" + apiNote.getLesson() + 
                            ", data=" + apiNote.getData() + 
                            ", isPersonal=false (учебная)");
                    } catch (Exception e) {
                        Log.e("SchedulelController", "Ошибка при добавлении Note: " + e.getMessage(), e);
                    }
                }
            }
        }
        
        // Парсим UpdateDate (DateTime с миллисекундами)
        // Пробуем несколько форматов, так как сервер может отправлять в разных форматах
        if (apiSchedule.getUpdateDate() != null && !apiSchedule.getUpdateDate().isEmpty()) {
            try {
                String dateStr = apiSchedule.getUpdateDate();
                Log.d("SchedulelController", "Парсинг UpdateDate: " + dateStr);
                
                SimpleDateFormat sdf = null;
                Date updateDate = null;
                
                // Пробуем разные форматы в порядке приоритета
                String[] formats = {
                    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",  // С миллисекундами и Z
                    "yyyy-MM-dd'T'HH:mm:ss.SSSSSSS'Z'", // С 7 цифрами миллисекунд (как на сервере .fff)
                    "yyyy-MM-dd'T'HH:mm:ss.SSS",     // С миллисекундами без Z
                    "yyyy-MM-dd'T'HH:mm:ss'Z'",      // Без миллисекунд с Z
                    "yyyy-MM-dd'T'HH:mm:ss",         // Без миллисекунд и без Z
                    "yyyy-MM-ddTHH:mm:ss.fffZ",      // Формат сервера без кавычек (попробуем как есть)
                    "yyyy-MM-ddTHH:mm:ss.fff"       // Формат сервера без Z
                };
                
                for (String format : formats) {
                    try {
                        sdf = new SimpleDateFormat(format, Locale.US);
                        sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                        updateDate = sdf.parse(dateStr);
                        Log.d("SchedulelController", "UpdateDate успешно распарсен с форматом: " + format);
                        break;
                    } catch (Exception e) {
                        // Пробуем следующий формат
                        continue;
                    }
                }
                
                if (updateDate == null) {
                    throw new Exception("Не удалось распарсить дату ни одним из форматов");
                }
                schedule.setUpdateDate(updateDate);
                Log.d("SchedulelController", "UpdateDate успешно распарсен: " + updateDate);
            } catch (Exception e) {
                Log.e("SchedulelController", "Ошибка парсинга UpdateDate: " + apiSchedule.getUpdateDate() + 
                    ", ошибка: " + e.getMessage(), e);
                schedule.updateDate(); // Используем текущее время
            }
        } else {
            Log.w("SchedulelController", "UpdateDate отсутствует, используется текущее время");
            schedule.updateDate(); // Используем текущее время
        }
        
        Log.d("SchedulelController", "Расписание успешно сконвертировано: " + schedule.getName() + 
            ", дней: " + schedule.getDays_Schedule().size() + 
            ", заданий: " + schedule.getHometasks().size() + 
            ", заметок: " + schedule.getNotes().size());
        
        return schedule;
    }
    
    /**
     * Отправляет все расписания из БД на сервер с синхронизацией (обновление если UpdateDate новее).
     */
    public void uploadAllSchedulesToServer(SyncCallback callback) {
        List<Schedules> allSchedules = getAllSchedulesObjects();
        List<ApiSchedule> apiSchedules = new ArrayList<>();
        
        Log.d("SchedulelController", "uploadAllSchedulesToServer: всего расписаний для отправки: " + allSchedules.size());
        for (Schedules schedule : allSchedules) {
            Log.d("SchedulelController", "Отправка расписания: " + schedule.getName() + 
                ", заданий в расписании: " + (schedule.getHometasks() != null ? schedule.getHometasks().size() : 0));
            if (schedule.getHometasks() != null) {
                for (Schedules.Hometask h : schedule.getHometasks()) {
                    Log.d("SchedulelController", "  Задание: " + h.getLesson() + " - " + h.getTask() + 
                        ", isPersonal: " + h.isPersonal() + ", isDone: " + h.isDone());
                }
            }
            ApiSchedule apiSchedule = convertToApiSchedule(schedule);
            if (apiSchedule != null) {
                Log.d("SchedulelController", "После конвертации: заданий в ApiSchedule: " + 
                    (apiSchedule.getHometasks() != null ? apiSchedule.getHometasks().size() : 0));
                apiSchedules.add(apiSchedule);
            }
        }
        
        ApiService apiService = NetworkClient.getApiService();
        // Используем syncUploadSchedules для обновления с проверкой UpdateDate
        Call<Void> call = apiService.syncUploadSchedules(apiSchedules);
        
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d("SchedulelController", "Все расписания успешно синхронизированы с сервером");
                    if (callback != null) callback.onSuccess("Расписания синхронизированы с сервером");
                } else {
                    Log.e("SchedulelController", "Ошибка синхронизации: " + response.code());
                    if (callback != null) callback.onError("Ошибка синхронизации: " + response.code());
                }
            }
            
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e("SchedulelController", "Сетевая ошибка: " + t.getMessage());
                if (callback != null) callback.onError("Сетевая ошибка: " + t.getMessage());
            }
        });
    }
    
    /**
     * Загружает все расписания с сервера и сохраняет в БД с синхронизацией (обновление если UpdateDate новее).
     */
    public void downloadAllSchedulesFromServer(SyncCallback callback) {
        ApiService apiService = NetworkClient.getApiService();
        // Используем syncDownloadSchedules для получения всех расписаний
        Call<List<ApiSchedule>> call = apiService.syncDownloadSchedules();
        
        call.enqueue(new Callback<List<ApiSchedule>>() {
            @Override
            public void onResponse(Call<List<ApiSchedule>> call, Response<List<ApiSchedule>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<ApiSchedule> apiSchedules = response.body();
                    Log.d("SchedulelController", "Получено расписаний с сервера: " + apiSchedules.size());
                    
                    int savedCount = 0;
                    int updatedCount = 0;
                    int skippedCount = 0;
                    
                    for (ApiSchedule apiSchedule : apiSchedules) {
                        Log.d("SchedulelController", "Обработка расписания: " + 
                            (apiSchedule != null ? apiSchedule.getName() : "null"));
                        
                        // Сначала проверяем, существует ли расписание с таким же именем И автором
                        SchedulesEntity existingEntity = repository.getScheduleByNameAndAuthorSync(
                            apiSchedule.getName() != null ? apiSchedule.getName() : "",
                            apiSchedule.getAuthor() != null ? apiSchedule.getAuthor() : ""
                        );
                        
                        Schedules existingSchedule = null;
                        if (existingEntity != null) {
                            existingSchedule = ScheduleMapper.toPojo(existingEntity);
                        }
                        
                        // Конвертируем с учетом существующего расписания (для сохранения статусов выполнения)
                        Schedules schedule = convertFromApiSchedule(apiSchedule, existingSchedule);
                        
                        if (schedule != null) {
                            if (existingEntity != null && existingSchedule != null) {
                                // Расписание существует - сравниваем UpdateDate
                                Date existingDate = existingSchedule.getUpdateDate();
                                Date newDate = schedule.getUpdateDate();
                                
                                Log.d("SchedulelController", "Найдено существующее расписание: " + schedule.getName() + 
                                    ", существующая дата: " + existingDate + 
                                    ", новая дата: " + newDate);
                                
                                // Обновляем только если новое расписание новее
                                if (newDate != null && (existingDate == null || newDate.after(existingDate))) {
                                    SchedulesEntity entity = ScheduleMapper.toEntity(schedule);
                                    // Используем ID существующего для обновления
                                    entity.setId(existingEntity.getId());
                                    Log.d("SchedulelController", "Обновление расписания в БД: " + schedule.getName() + 
                                        ", ID: " + existingEntity.getId());
                                    repository.updateSync(entity);
                                    updatedCount++;
                                    Log.d("SchedulelController", "Обновлено расписание: " + schedule.getName());
                                } else {
                                    skippedCount++;
                                    Log.d("SchedulelController", "Пропущено расписание (старая версия или равная): " + 
                                        schedule.getName() + ", существующая: " + existingDate + ", новая: " + newDate);
                                }
                            } else {
                                // Расписание не существует - добавляем новое (все задания невыполненные)
                                Log.d("SchedulelController", "Расписание не найдено, добавляем новое: " + schedule.getName() + 
                                    ", автор: " + schedule.getAuthor());
                                SchedulesEntity entity = ScheduleMapper.toEntity(schedule);
                                Log.d("SchedulelController", "Сохранение в БД: " + schedule.getName() + 
                                    ", дней: " + schedule.getDays_Schedule().size() + 
                                    ", заданий: " + schedule.getHometasks().size() + 
                                    ", заметок: " + schedule.getNotes().size());
                                
                                // Проверяем, что entity содержит правильные данные
                                if (entity != null) {
                                    Log.d("SchedulelController", "Entity перед сохранением: " + 
                                        "дней: " + (entity.getDays_Schedule() != null ? entity.getDays_Schedule().size() : 0) + 
                                        ", заданий: " + (entity.getHometasks() != null ? entity.getHometasks().size() : 0) + 
                                        ", заметок: " + (entity.getNotes() != null ? entity.getNotes().size() : 0));
                                    
                                    if (entity.getHometasks() != null && !entity.getHometasks().isEmpty()) {
                                        for (Schedules.Hometask h : entity.getHometasks()) {
                                            Log.d("SchedulelController", "  Hometask в entity: " + h.getLesson() + " - " + h.getTask() + 
                                                ", endpoint: " + h.getEndpoint() + ", isDone: " + h.isDone() + ", isPersonal: " + h.isPersonal());
                                        }
                                    }
                                }
                                
                                repository.insertSync(entity);
                                
                                // Проверяем, что данные сохранились
                                SchedulesEntity savedEntity = repository.getScheduleByNameAndAuthorSync(schedule.getName(), schedule.getAuthor());
                                if (savedEntity != null) {
                                    Log.d("SchedulelController", "Проверка после сохранения: " + 
                                        "ID: " + savedEntity.getId() + 
                                        ", дней: " + (savedEntity.getDays_Schedule() != null ? savedEntity.getDays_Schedule().size() : 0) + 
                                        ", заданий: " + (savedEntity.getHometasks() != null ? savedEntity.getHometasks().size() : 0) + 
                                        ", заметок: " + (savedEntity.getNotes() != null ? savedEntity.getNotes().size() : 0));
                                    
                                    if (savedEntity.getHometasks() != null && !savedEntity.getHometasks().isEmpty()) {
                                        for (Schedules.Hometask h : savedEntity.getHometasks()) {
                                            Log.d("SchedulelController", "  Hometask после сохранения: " + h.getLesson() + " - " + h.getTask() + 
                                                ", endpoint: " + h.getEndpoint() + ", isDone: " + h.isDone() + ", isPersonal: " + h.isPersonal());
                                        }
                                    }
                                } else {
                                    Log.e("SchedulelController", "ОШИБКА: Расписание не найдено после сохранения!");
                                }
                                
                                savedCount++;
                                Log.d("SchedulelController", "Расписание успешно добавлено: " + schedule.getName());
                            }
                        } else {
                            Log.e("SchedulelController", "Не удалось сконвертировать ApiSchedule в Schedules");
                        }
                    }
                    
                    String message = String.format("Синхронизировано: добавлено %d, обновлено %d, пропущено %d", 
                        savedCount, updatedCount, skippedCount);
                    Log.d("SchedulelController", message);
                    notifyAction("SchedulesDownloaded");
                    if (callback != null) callback.onSuccess(message);
                } else {
                    Log.e("SchedulelController", "Ошибка загрузки: " + response.code());
                    if (callback != null) callback.onError("Ошибка загрузки: " + response.code());
                }
            }
            
            @Override
            public void onFailure(Call<List<ApiSchedule>> call, Throwable t) {
                Log.e("SchedulelController", "Сетевая ошибка: " + t.getMessage());
                if (callback != null) callback.onError("Сетевая ошибка: " + t.getMessage());
            }
        });
    }
    
    /**
     * Отправляет определенные расписания на сервер с синхронизацией (обновление если UpdateDate новее).
     */
    public void uploadSchedulesToServer(List<Schedules> schedules, SyncCallback callback) {
        if (schedules == null || schedules.isEmpty()) {
            if (callback != null) callback.onError("Нет расписаний для отправки");
            return;
        }
        
        List<ApiSchedule> apiSchedules = new ArrayList<>();
        for (Schedules schedule : schedules) {
            ApiSchedule apiSchedule = convertToApiSchedule(schedule);
            if (apiSchedule != null) {
                apiSchedules.add(apiSchedule);
            }
        }
        
        ApiService apiService = NetworkClient.getApiService();
        // Используем syncUploadSchedules для обновления с проверкой UpdateDate
        Call<Void> call = apiService.syncUploadSchedules(apiSchedules);
        
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d("SchedulelController", "Расписания успешно синхронизированы с сервером");
                    if (callback != null) callback.onSuccess("Расписания синхронизированы");
                } else {
                    Log.e("SchedulelController", "Ошибка синхронизации: " + response.code());
                    if (callback != null) callback.onError("Ошибка синхронизации: " + response.code());
                }
            }
            
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e("SchedulelController", "Сетевая ошибка: " + t.getMessage());
                if (callback != null) callback.onError("Сетевая ошибка: " + t.getMessage());
            }
        });
    }
    
    /**
     * Загружает определенное расписание с сервера по имени и автору.
     */
    public void downloadScheduleFromServer(String author, String name, SyncCallback callback) {
        ApiService apiService = NetworkClient.getApiService();
        Call<ApiSchedule> call = apiService.getScheduleByNameAndAuthor(author, name);
        
        call.enqueue(new Callback<ApiSchedule>() {
            @Override
            public void onResponse(Call<ApiSchedule> call, Response<ApiSchedule> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiSchedule apiSchedule = response.body();
                    // При загрузке конкретного расписания не сохраняем локальные статусы (полная загрузка)
                    Schedules schedule = convertFromApiSchedule(apiSchedule, null);
                    
                    if (schedule != null) {
                        SchedulesEntity entity = ScheduleMapper.toEntity(schedule);
                        repository.insertSync(entity);
                        Log.d("SchedulelController", "Расписание загружено с сервера");
                        notifyAction("ScheduleDownloaded");
                        if (callback != null) callback.onSuccess("Расписание загружено");
                    } else {
                        if (callback != null) callback.onError("Ошибка конвертации расписания");
                    }
                } else {
                    Log.e("SchedulelController", "Ошибка загрузки: " + response.code());
                    if (callback != null) callback.onError("Ошибка загрузки: " + response.code());
                }
            }
            
            @Override
            public void onFailure(Call<ApiSchedule> call, Throwable t) {
                Log.e("SchedulelController", "Сетевая ошибка: " + t.getMessage());
                if (callback != null) callback.onError("Сетевая ошибка: " + t.getMessage());
            }
        });
    }
    
    /**
     * Синхронизация: обновляет расписания на сервере из данных приложения.
     */
    public void syncUploadToServer(SyncCallback callback) {
        List<Schedules> allSchedules = getAllSchedulesObjects();
        uploadSchedulesToServer(allSchedules, callback);
    }
    
    /**
     * Синхронизация: обновляет расписания в приложении из данных сервера.
     */
    public void syncDownloadFromServer(SyncCallback callback) {
        downloadAllSchedulesFromServer(callback);
    }
    
    /**
     * Интерфейс для обратного вызова при синхронизации.
     */
    public interface SyncCallback {
        void onSuccess(String message);
        void onError(String error);
    }
}
