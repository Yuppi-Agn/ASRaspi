package com.AS.Yuppi.Raspi.DataWorkers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class JsonConverter {
    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting() // Для красивого форматирования JSON
            .create();
    /**
     * Преобразует объект Schedules в строку формата JSON.
     * @param schedule Объект Schedules, который нужно сериализовать.
     * @return Строка JSON.
     */
    public static String toJson(Schedules schedule) {
        if (schedule == null) {
            return "{}";
        }
        // Gson автоматически обрабатывает все вложенные поля и классы
        return gson.toJson(schedule);
    }

    /**
     * Преобразует строку формата JSON обратно в объект Schedules.
     * @param jsonString Строка JSON для десериализации.
     * @return Объект Schedules или null в случае ошибки.
     */
    public static Schedules fromJson(String jsonString) {
        if (jsonString == null || jsonString.isEmpty()) {
            return null;
        }
        try {
            return gson.fromJson(jsonString, Schedules.class);
        } catch (Exception e) {
            System.err.println("Ошибка десериализации JSON в Schedules: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    public static Schedules fromJson_oldversiron(String content){
        try {
        if (content == null || content.isEmpty()) {
            return null;
        }
        Schedules Schedule = new Schedules();
        int weekip;
        String FileContent=content;
                /*try {
                    FileContent = FileUtil.readFile(filePath);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }*/
        List<String> export = new Gson().fromJson(FileContent, new TypeToken<ArrayList<String>>(){}.getType());

        if (export == null || export.size() < 2) {
            return null;
        }

        List<String> Vremechko_T = new Gson().fromJson(export.get(0), new TypeToken<ArrayList<String>>(){}.getType());
        List<String> Raspisa = new Gson().fromJson(export.get(1), new TypeToken<ArrayList<String>>(){}.getType());

        HashMap<String, Object> specialDays = null;
        if (export.size() > 2) {
            specialDays = new Gson().fromJson(export.get(2), new TypeToken<HashMap<String, Object>>(){}.getType());
        }

        weekip = Integer.parseInt(Vremechko_T.get(0));

        Schedule.setCircle_Mode(1);//14
        Schedule.setFirstWeekId(weekip);

        if (weekip == 1) {
            for (int i = 0; i < 7; i++){
                Schedule.setDayLesson(Raspisa.get(i + 1), i); // берём с 1 по 7 индексы
            }
        } else {
            for (int i = 0; i < 7; i++)
            {
                Schedule.setDayLesson(Raspisa.get(i + 8), i+8); // берём с 8 по 14 индексы
            }
        }

        for (int i = 0; i < 14; i++) Schedule.setDayLesson(Raspisa.get(i + 1),i);
        {
            int oldlistsize = (Vremechko_T.size()-1)/14;
            int oldlist_count=1;

            for (int i1 = 0; i1 < 14; i1++){
                int CurId=0;
                for (int i2 = 1; i2 < oldlistsize; i2+=2)
                {
                    Schedule.setLesson_StartTime(i1, CurId, Integer.parseInt(Vremechko_T.get(oldlist_count)));
                    Schedule.setLesson_EndTime(i1, CurId, Integer.parseInt(Vremechko_T.get(oldlist_count+1)));
                    CurId++;
                    oldlist_count+=2;
                }
            }
        }
        return  Schedule;
        } catch (Exception e) {
            System.err.println("Ошибка десериализации JSON в Schedules: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    public static Schedules fromJson_Uni(String content){
        Schedules Schedule = fromJson_oldversiron(content);
        if(Schedule != null)
            return Schedule;
        Schedule = fromJson(content);
        return Schedule;
    }
}
