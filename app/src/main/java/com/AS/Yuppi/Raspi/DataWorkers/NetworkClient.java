package com.AS.Yuppi.Raspi.DataWorkers;

import android.content.Context;
import android.util.Log;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.concurrent.atomic.AtomicReference;

public class NetworkClient {
    private static final String TAG = "NetworkClient";
    private static Retrofit retrofit;
    private static String currentBaseUrl;
    private static Context appContext;

    public static void initialize(Context context) {
        appContext = context.getApplicationContext();
    }

    public static Retrofit getRetrofitInstance() {
        if (retrofit == null || currentBaseUrl == null) {
            // Пытаемся найти сервер через mDNS
            String baseUrl = discoverServerUrl();
            
            if (baseUrl == null || baseUrl.isEmpty()) {
                Log.e(TAG, "mDNS discovery failed - server not found. Please ensure server is running and mDNS is enabled.");
                throw new IllegalStateException("Server not found via mDNS. Please ensure the server is running and mDNS is enabled.");
            }
            
            currentBaseUrl = baseUrl;
            createRetrofitInstance(baseUrl);
        }
        return retrofit;
    }
    
    private static String discoverServerUrl() {
        if (appContext == null) {
            Log.e(TAG, "App context not initialized");
            return null;
        }
        
        try {
            MdnsServiceDiscovery discovery = new MdnsServiceDiscovery(appContext);
            String url = discovery.discoverServerSync();
            if (url != null && !url.isEmpty()) {
                Log.d(TAG, "Server found via mDNS: " + url);
                return url;
            } else {
                Log.w(TAG, "mDNS discovery completed but server URL is empty");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during mDNS discovery", e);
        }
        
        return null;
    }
    
    private static void createRetrofitInstance(String baseUrl) {
        // Создаем Interceptor для логирования
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        // Создаем OkHttp клиент с логгером
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .build();

        // Собираем Retrofit
        retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        
        Log.d(TAG, "Retrofit instance created with URL: " + baseUrl);
    }
    
    public static void resetRetrofitInstance() {
        retrofit = null;
        currentBaseUrl = null;
    }

    public static ApiService getApiService() {
        return getRetrofitInstance().create(ApiService.class);
    }
}
