package com.AS.Yuppi.Raspi.DataWorkers;

import com.AS.Yuppi.Raspi.DataWorkers.BD.SchedulesEntity;

public class ScheduleMapper {

    /**
     * Преобразование POJO Schedules в SchedulesEntity для сохранения в Room.
     */
    public static SchedulesEntity toEntity(Schedules pojo) {
        if (pojo == null) return null;

        SchedulesEntity entity = new SchedulesEntity(
                pojo.getDays_Schedule(),
                pojo.getSpecial_Days_Shedule(),
                pojo.getCircle_Mode(),
                pojo.getFirstWeekId(),
                pojo.getStart_Date(),
                pojo.getEnd_Date(),
                pojo.getHollidays(),
                pojo.getName(),
                pojo.getAuthor()
        );
        return entity;
    }

    /**
     * Преобразование SchedulesEntity обратно в POJO Schedules для использования в приложении.
     */
    public static Schedules toPojo(SchedulesEntity entity) {
        if (entity == null) return null;

        // Используем конструктор, который корректно инициализирует Days_Schedule
        Schedules pojo = new Schedules(entity.getCircle_Mode());

        // Перенос данных из Entity в POJO
        pojo.setName(entity.getName());
        pojo.setAuthor(entity.getAuthor());
        pojo.setFirstWeekId(entity.getFirstWeekId());
        pojo.setStart_Date(entity.getStart_Date());
        pojo.setEnd_Date(entity.getEnd_Date());
        pojo.setHollidays(entity.getHollidays());
        pojo.setSpecial_Days_Shedule(entity.getSpecial_Days_Shedule());
        pojo.setDays_Schedule(entity.getDays_Schedule());

        return pojo;
    }
}