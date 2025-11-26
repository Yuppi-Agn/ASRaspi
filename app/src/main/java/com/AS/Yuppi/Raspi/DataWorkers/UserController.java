package com.AS.Yuppi.Raspi.DataWorkers;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Контроллер пользователя: управляет событиями и личными задачами.
 */
public class UserController {

    private final List<UserEvents> userEvents = new ArrayList<>();
    private final List<UserTask> userTasks = new ArrayList<>();
    private final com.AS.Yuppi.Raspi.DataWorkers.BD.ScheduleRepository repository;

    public UserController(com.AS.Yuppi.Raspi.DataWorkers.BD.ScheduleRepository repository) {
        this.repository = repository;
    }

    public List<UserEvents> getUserEvents() {
        return userEvents;
    }

    public List<UserTask> getUserTasks() {
        return userTasks;
    }

    public void addUserEvent(UserEvents event) {
        if (event != null) {
            userEvents.add(event);
            // также сохраняем в БД
            com.AS.Yuppi.Raspi.DataWorkers.BD.UserEventEntity entity =
                    new com.AS.Yuppi.Raspi.DataWorkers.BD.UserEventEntity(
                            event.getDate(),
                            event.getTime(),
                            event.getCircle_Mode(),
                            event.getCircle_Days(),
                            event.getName(),
                            event.getInfo(),
                            event.isEnable()
                    );
            repository.insertUserEvent(entity);
        }
    }

    public void addUserTask(UserTask task) {
        if (task != null) {
            userTasks.add(task);
            com.AS.Yuppi.Raspi.DataWorkers.BD.UserTaskEntity entity =
                    new com.AS.Yuppi.Raspi.DataWorkers.BD.UserTaskEntity(
                            task.getEndpoint(),
                            task.getName(),
                            task.getTask(),
                            task.isDone()
                    );
            repository.insertUserTask(entity);
        }
    }

    /**
     * Возвращает список событий для дня с указанным смещением (0 = сегодня, 1 = завтра и т.д.).
     */
    public List<UserEvents> getEventsForDayOffset(int offset) {
        LocalDate targetDate = LocalDate.now().plusDays(offset);
        List<UserEvents> result = new ArrayList<>();

        // загружаем из БД
        List<com.AS.Yuppi.Raspi.DataWorkers.BD.UserEventEntity> dbEvents =
                repository.getEventsForDateSync(targetDate);
        for (com.AS.Yuppi.Raspi.DataWorkers.BD.UserEventEntity e : dbEvents) {
            UserEvents ue = new UserEvents(
                    e.getDate(),
                    e.getTime(),
                    e.getCircleMode(),
                    e.getCircleDays(),
                    e.getName(),
                    e.getInfo()
            );
            ue.setId(e.getId());
            ue.setEnable(e.isEnable());
            result.add(ue);
        }

        // плюс уже существующие в памяти (учитываем повторения)
        for (UserEvents event : userEvents) {
            if (isEventOnDate(event, targetDate)) {
                result.add(event);
            }
        }
        return result;
    }

    /**
     * Получить все события (для экрана редактирования).
     */
    public List<UserEvents> getAllEvents() {
        List<UserEvents> result = new ArrayList<>();
        List<com.AS.Yuppi.Raspi.DataWorkers.BD.UserEventEntity> dbEvents =
                repository.getAllUserEventsSync();
        for (com.AS.Yuppi.Raspi.DataWorkers.BD.UserEventEntity e : dbEvents) {
            UserEvents ue = new UserEvents(
                    e.getDate(),
                    e.getTime(),
                    e.getCircleMode(),
                    e.getCircleDays(),
                    e.getName(),
                    e.getInfo()
            );
            ue.setId(e.getId());
            ue.setEnable(e.isEnable());
            result.add(ue);
        }
        return result;
    }

    public UserEvents getEventById(int id) {
        com.AS.Yuppi.Raspi.DataWorkers.BD.UserEventEntity e =
                repository.getUserEventByIdSync(id);
        if (e == null) return null;
        UserEvents ue = new UserEvents(
                e.getDate(),
                e.getTime(),
                e.getCircleMode(),
                e.getCircleDays(),
                e.getName(),
                e.getInfo()
        );
        ue.setId(e.getId());
        ue.setEnable(e.isEnable());
        return ue;
    }

    public void saveOrUpdateEvent(UserEvents event) {
        if (event == null) return;
        com.AS.Yuppi.Raspi.DataWorkers.BD.UserEventEntity entity =
                new com.AS.Yuppi.Raspi.DataWorkers.BD.UserEventEntity(
                        event.getDate(),
                        event.getTime(),
                        event.getCircle_Mode(),
                        event.getCircle_Days(),
                        event.getName(),
                        event.getInfo(),
                        event.isEnable()
                );
        if (event.getId() != 0) {
            entity.setId(event.getId());
        }
        repository.insertUserEvent(entity);
    }

    public void toggleEventEnabled(int id) {
        com.AS.Yuppi.Raspi.DataWorkers.BD.UserEventEntity e =
                repository.getUserEventByIdSync(id);
        if (e != null) {
            boolean newValue = !e.isEnable();
            repository.setUserEventEnabled(id, newValue);
        }
    }

    public void deleteEvent(int id) {
        repository.deleteUserEventById(id);
    }

    /**
     * Возвращает список задач, актуальных на день с указанным смещением.
     * Здесь считаем задачу актуальной, если ее дедлайн (Endpoint) >= targetDate.
     */
    public List<UserTask> getTasksForDayOffset(int offset) {
        LocalDate targetDate = LocalDate.now().plusDays(offset);
        List<UserTask> result = new ArrayList<>();

        // задачи из БД начиная с этой даты
        List<com.AS.Yuppi.Raspi.DataWorkers.BD.UserTaskEntity> dbTasks =
                repository.getTasksFromDateSync(targetDate);
        for (com.AS.Yuppi.Raspi.DataWorkers.BD.UserTaskEntity t : dbTasks) {
            UserTask ut = new UserTask(
                    t.getEndpoint(),
                    t.getName(),
                    t.getTask()
            );
            ut.setId(t.getId());
            ut.setDone(t.isDone());
            result.add(ut);
        }

        // и уже существующие в памяти
        for (UserTask task : userTasks) {
            if (task.getEndpoint() != null && !task.getEndpoint().isBefore(targetDate)) {
                result.add(task);
            }
        }
        return result;
    }

    // Все задачи (для экрана редактирования)
    public List<UserTask> getAllTasks() {
        List<UserTask> result = new ArrayList<>();
        List<com.AS.Yuppi.Raspi.DataWorkers.BD.UserTaskEntity> dbTasks =
                repository.getAllUserTasksSync();
        for (com.AS.Yuppi.Raspi.DataWorkers.BD.UserTaskEntity t : dbTasks) {
            UserTask ut = new UserTask(
                    t.getEndpoint(),
                    t.getName(),
                    t.getTask()
            );
            ut.setId(t.getId());
            ut.setDone(t.isDone());
            result.add(ut);
        }
        return result;
    }

    public UserTask getTaskById(int id) {
        com.AS.Yuppi.Raspi.DataWorkers.BD.UserTaskEntity t =
                repository.getUserTaskByIdSync(id);
        if (t == null) return null;
        UserTask ut = new UserTask(
                t.getEndpoint(),
                t.getName(),
                t.getTask()
        );
        ut.setId(t.getId());
        ut.setDone(t.isDone());
        return ut;
    }

    public void saveOrUpdateTask(UserTask task) {
        if (task == null) return;
        com.AS.Yuppi.Raspi.DataWorkers.BD.UserTaskEntity entity =
                new com.AS.Yuppi.Raspi.DataWorkers.BD.UserTaskEntity(
                        task.getEndpoint(),
                        task.getName(),
                        task.getTask(),
                        task.isDone()
                );
        if (task.getId() != 0) {
            entity.setId(task.getId());
        }
        repository.insertUserTask(entity);
    }

    public void toggleTaskDone(int id) {
        com.AS.Yuppi.Raspi.DataWorkers.BD.UserTaskEntity t =
                repository.getUserTaskByIdSync(id);
        if (t != null) {
            repository.setUserTaskDone(id, !t.isDone());
        }
    }

    public void deleteTask(int id) {
        repository.deleteUserTaskById(id);
    }

    /**
     * Проверяет, попадает ли событие на указанную дату с учетом режима повторения.
     */
    private boolean isEventOnDate(UserEvents event, LocalDate targetDate) {
        LocalDate start = event.getDate();
        if (start == null) return false;

        int mode = event.getCircle_Mode();
        int circleDays = event.getCircle_Days();

        switch (mode) {
            case 0: // один раз
                return targetDate.equals(start);
            case 1: // еженедельно
            {
                long days = ChronoUnit.DAYS.between(start, targetDate);
                return days >= 0 && days % 7 == 0;
            }
            case 2: // через неделю (раз в 2 недели)
            {
                long days = ChronoUnit.DAYS.between(start, targetDate);
                return days >= 0 && days % 14 == 0;
            }
            case 3: // каждые Circle_Days дней
            {
                if (circleDays <= 0) return false;
                long days = ChronoUnit.DAYS.between(start, targetDate);
                return days >= 0 && days % circleDays == 0;
            }
            default:
                return false;
        }
    }
}


