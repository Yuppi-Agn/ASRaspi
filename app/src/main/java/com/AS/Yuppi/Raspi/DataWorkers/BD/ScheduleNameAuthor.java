package com.AS.Yuppi.Raspi.DataWorkers.BD;

public class ScheduleNameAuthor {
    private String Name;
    private String Author;

    // Конструктор, который Room использует для создания объекта
    public ScheduleNameAuthor(String Name, String Author) {
        this.Name = Name;
        this.Author = Author;
    }
    public String getName() { return Name; }
    public String getAuthor() { return Author; }

    /**
     * Форматирует данные в строку "Автор-Имя".
     */
    @Override
    public String toString() {
        return Author + "-" + Name;
    }
}