package com.AS.Yuppi.Raspi.DataWorkers;

public class MySingleton {
    private static MySingleton instance;
    private SchedulelController schedulelController;
    private MySingleton() {
        schedulelController = new SchedulelController();
    }
    public static synchronized MySingleton getInstance() {
        if (instance == null) {
            instance = new MySingleton();
        }
        return instance;
    }
    public SchedulelController getSchedulelController() {
        return schedulelController;
    }
}
