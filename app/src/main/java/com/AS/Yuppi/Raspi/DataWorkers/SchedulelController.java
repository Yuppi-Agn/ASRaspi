package com.AS.Yuppi.Raspi.DataWorkers;

import android.content.Context;

import com.AS.Yuppi.Raspi.DataWorkers.BD.ScheduleNameAuthor;
import com.AS.Yuppi.Raspi.DataWorkers.BD.ScheduleRepository;
import com.AS.Yuppi.Raspi.DataWorkers.BD.SchedulesEntity;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class SchedulelController{
    private List<String> SchedulesList= new ArrayList<String>();
    private Schedules CurrentSchedule, editableSchedule;
    private final ScheduleRepository repository;
    //public SchedulelController(){}
    public SchedulelController(ScheduleRepository repository){
        this.repository = repository;
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

        // 1. Преобразовать POJO Schedules в Entity
        SchedulesEntity entity = ScheduleMapper.toEntity(editableSchedule);

        // 2. Вставить/Обновить Entity в БД (выполняется в фоновом потоке репозитория)
        repository.insert(entity);

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
    private void notifyAction(String data) {
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
}
