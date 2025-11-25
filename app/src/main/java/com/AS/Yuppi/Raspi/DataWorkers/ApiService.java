package com.AS.Yuppi.Raspi.DataWorkers;

//import android.telecom.Call;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import com.AS.Yuppi.Raspi.DataWorkers.DTOs.ApiSchedule;

public interface ApiService {

    @GET("api/Schedule")
    Call<List<ApiSchedule>> getSchedules();

    @POST("api/Schedule")
    Call<Void> postSchedule(@Body ApiSchedule schedule); // Call<Void> потому что ответ не важен
}
