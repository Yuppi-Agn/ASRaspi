package com.AS.Yuppi.Raspi.DataWorkers.BD;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

@Database(entities = {SchedulesEntity.class, LastActiveScheduleEntity.class, UserEventEntity.class, UserTaskEntity.class}, version = 7, exportSchema = false)
@TypeConverters({Converters.class}) // Регистрация преобразователей типов
public abstract class AppDatabase extends RoomDatabase {
    public abstract LastActiveScheduleDao lastActiveScheduleDao();
    public abstract SchedulesDao schedulesDao();
    public abstract UserEventsDao userEventsDao();
    public abstract UserTasksDao userTasksDao();
    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    // Создание базы данных
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "schedule_database")
                            // ВНИМАНИЕ: при изменении схемы база будет пересоздана (старые данные удалятся)
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}