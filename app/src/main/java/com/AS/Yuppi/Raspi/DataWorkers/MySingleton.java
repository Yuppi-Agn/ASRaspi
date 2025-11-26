package com.AS.Yuppi.Raspi.DataWorkers;

import android.app.Application;
import android.content.Context;

import com.AS.Yuppi.Raspi.DataWorkers.BD.ScheduleRepository;

public class MySingleton {
    private static MySingleton instance;
    private SchedulelController schedulelController;
    private ScheduleRepository scheduleRepository;
    private UserController userController;
    private static String HashedDeviceId="";
    private MySingleton() {
        //schedulelController = new SchedulelController();
    }
    public static synchronized MySingleton getInstance(Context context) {
        if (instance == null) {
            instance = new MySingleton();

            // Проверка и получение Application Context
            Application application = null;
            if (context instanceof Application) {
                application = (Application) context;
            } else if (context != null) {
                application = (Application) context.getApplicationContext();
            }

            if (application == null) {
                throw new IllegalStateException("Не удалось получить Application Context для инициализации ScheduleRepository.");
            }

            HashedDeviceId=DeviceUtils.getHashedDeviceId(context);

            // 2. Инициализация ScheduleRepository
            instance.scheduleRepository = new ScheduleRepository(application);

            // 3. Создание SchedulelController с передачей репозитория
            instance.schedulelController = new SchedulelController(instance.scheduleRepository);

            // 4. При старте приложения пытаемся загрузить последнее активное расписание
            instance.schedulelController.loadLastUsedScheduleOnStartup();

            // 5. Создаем UserController для пользовательских событий/задач
            instance.userController = new UserController(instance.scheduleRepository);
        }
        return instance;
    }
    public SchedulelController getSchedulelController() {
        // Мы предполагаем, что getInstance(Context) был вызван ранее
        if (schedulelController == null) {
            throw new IllegalStateException("MySingleton не был инициализирован. Вызовите getInstance(Context) первым.");
        }
        return schedulelController;
    }

    // Метод для получения репозитория, может быть полезен в других местах
    public ScheduleRepository getScheduleRepository() {
        if (scheduleRepository == null) {
            throw new IllegalStateException("MySingleton не был инициализирован. Вызовите getInstance(Context) первым.");
        }
        return scheduleRepository;
    }

    public UserController getUserController() {
        if (userController == null) {
            throw new IllegalStateException("MySingleton не был инициализирован. Вызовите getInstance(Context) первым.");
        }
        return userController;
    }

    public static String getHashedDeviceId(){
        return HashedDeviceId;
    }
}
