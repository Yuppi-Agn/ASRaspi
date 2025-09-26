package com.AS.Yuppi.Raspi.DataWorkers;

import android.content.Context;

import com.AS.Yuppi.Raspi.DataWorkers.BD.ScheduleNameAuthor;
import com.AS.Yuppi.Raspi.DataWorkers.BD.ScheduleRepository;
import com.AS.Yuppi.Raspi.DataWorkers.BD.SchedulesEntity;

import java.util.ArrayList;
import java.util.List;

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
}
