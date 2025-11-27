package com.AS.Yuppi.Raspi.DataWorkers.BD;

import androidx.room.TypeConverter;

import com.AS.Yuppi.Raspi.DataWorkers.Day_Schedule;
import com.AS.Yuppi.Raspi.DataWorkers.Schedules;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Converters {

    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(java.time.LocalDate.class, new LocalDateAdapter())
            .create();
    
    // Адаптер для сериализации/десериализации LocalDate в Gson
    private static class LocalDateAdapter extends com.google.gson.TypeAdapter<java.time.LocalDate> {
        @Override
        public void write(com.google.gson.stream.JsonWriter out, java.time.LocalDate value) throws java.io.IOException {
            if (value == null) {
                out.nullValue();
            } else {
                out.value(value.toString());
            }
        }

        @Override
        public java.time.LocalDate read(com.google.gson.stream.JsonReader in) throws java.io.IOException {
            if (in.peek() == com.google.gson.stream.JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            String dateStr = in.nextString();
            return java.time.LocalDate.parse(dateStr);
        }
    }

    // --- Преобразование для LocalDate ---

    @TypeConverter
    public static LocalDate fromTimestamp(String value) {
        // LocalDate введен в API 26, поэтому этот конвертер безопасен для API 26+
        if (value == null || value.isEmpty() || value.equals("null")) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (Exception e) {
            // Если не удалось распарсить, возвращаем null вместо дефолтной даты
            return null;
        }
    }

    @TypeConverter
    public static String dateToTimestamp(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.toString();
    }

    // --- Преобразование для List<Integer> ---

    @TypeConverter
    public static List<Integer> fromIntListString(String value) {
        if (value == null) {
            return new ArrayList<>();
        }
        Type listType = new TypeToken<List<Integer>>() {}.getType();
        return gson.fromJson(value, listType);
    }

    @TypeConverter
    public static String fromIntList(List<Integer> list) {
        return gson.toJson(list);
    }

    // --- Преобразование для List<LocalDate> ---

    @TypeConverter
    public static List<LocalDate> fromLocalDateListString(String value) {
        if (value == null) {
            return new ArrayList<>();
        }
        Type listType = new TypeToken<List<LocalDate>>() {}.getType();
        return gson.fromJson(value, listType);
    }

    @TypeConverter
    public static String fromLocalDateList(List<LocalDate> list) {
        return gson.toJson(list);
    }

    // --- Преобразование для Map<LocalDate, Day_Schedule> ---

    // NOTE: Day_Schedule должен быть @Entity или @Embedded, но для Map лучше использовать Gson.
    // Если Day_Schedule - это не Entity, то это должен быть простой POJO.
    // Здесь мы конвертируем его в JSON.
    @TypeConverter
    public static Map<LocalDate, Day_Schedule> fromSpecialDaysScheduleString(String value) {
        if (value == null) {
            return new HashMap<>();
        }
        // Специальный тип для GSON, учитывая ключи LocalDate (преобразованные в String)
        Type mapType = new TypeToken<Map<String, Day_Schedule>>() {}.getType();
        Map<String, Day_Schedule> tempMap = gson.fromJson(value, mapType);

        // Преобразуем ключи обратно в LocalDate
        Map<LocalDate, Day_Schedule> result = new HashMap<>();
        for (Map.Entry<String, Day_Schedule> entry : tempMap.entrySet()) {
            result.put(LocalDate.parse(entry.getKey()), entry.getValue());
        }
        return result;
    }
    @TypeConverter
    public static List<Day_Schedule> fromDayScheduleListString(String value) {
        if (value == null) {
            return new ArrayList<>();
        }
        Type listType = new TypeToken<List<Day_Schedule>>() {}.getType();
        // Используем существующий Gson-объект
        return gson.fromJson(value, listType);
    }

    @TypeConverter
    public static String fromDayScheduleList(List<Day_Schedule> list) {
        return gson.toJson(list);
    }

    // --- Преобразование для List<Schedules.Hometask> ---

    @TypeConverter
    public static List<Schedules.Hometask> fromHometaskListString(String value) {
        if (value == null) {
            return new ArrayList<>();
        }
        Type listType = new TypeToken<List<Schedules.Hometask>>() {}.getType();
        return gson.fromJson(value, listType);
    }

    @TypeConverter
    public static String fromHometaskList(List<Schedules.Hometask> list) {
        return gson.toJson(list);
    }

    // --- Преобразование для List<Schedules.Note> ---

    @TypeConverter
    public static List<Schedules.Note> fromNoteListString(String value) {
        if (value == null) {
            return new ArrayList<>();
        }
        Type listType = new TypeToken<List<Schedules.Note>>() {}.getType();
        return gson.fromJson(value, listType);
    }

    @TypeConverter
    public static String fromNoteList(List<Schedules.Note> list) {
        return gson.toJson(list);
    }
    @TypeConverter
    public static String fromSpecialDaysSchedule(Map<LocalDate, Day_Schedule> map) {
        // Преобразуем ключи LocalDate в String для GSON
        Map<String, Day_Schedule> tempMap = new HashMap<>();
        for (Map.Entry<LocalDate, Day_Schedule> entry : map.entrySet()) {
            tempMap.put(entry.getKey().toString(), entry.getValue());
        }
        return gson.toJson(tempMap);
    }
}