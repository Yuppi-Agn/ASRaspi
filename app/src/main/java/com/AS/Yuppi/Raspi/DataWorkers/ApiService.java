package com.AS.Yuppi.Raspi.DataWorkers;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import com.AS.Yuppi.Raspi.DataWorkers.DTOs.ApiSchedule;

public interface ApiService {

    // Получить все расписания
    @GET("api/Schedule")
    Call<List<ApiSchedule>> getSchedules();

    // Получить расписание по ID
    @GET("api/Schedule/{id}")
    Call<ApiSchedule> getScheduleById(@Path("id") int id);
    
    // Получить расписание по имени и автору
    @GET("api/Schedule/{author}/{name}")
    Call<ApiSchedule> getScheduleByNameAndAuthor(@Path("author") String author, @Path("name") String name);

    // Отправить одно расписание (создание)
    @POST("api/Schedule")
    Call<ApiSchedule> postSchedule(@Body ApiSchedule schedule);
    
    // Отправить список расписаний (массовая отправка)
    @POST("api/Schedule/batch")
    Call<Void> postSchedules(@Body List<ApiSchedule> schedules);

    // Обновить расписание (полная синхронизация с сервера на клиент)
    @PUT("api/Schedule/{id}")
    Call<ApiSchedule> updateSchedule(@Path("id") int id, @Body ApiSchedule schedule);
    
    // Синхронизация: обновить расписания на сервере из данных приложения
    @PUT("api/Schedule/sync/upload")
    Call<Void> syncUploadSchedules(@Body List<ApiSchedule> schedules);
    
    // Синхронизация: получить обновленные расписания с сервера
    @GET("api/Schedule/sync/download")
    Call<List<ApiSchedule>> syncDownloadSchedules();
}
