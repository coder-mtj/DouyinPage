package com.mtj.douyinpage.data.remote;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class ApiClient {

    private static final String BASE_URL = "http://localhost:8080/";

    private static volatile StarUserApi starUserApi;

    private ApiClient() {
    }

    public static StarUserApi getStarUserApi() {
        if (starUserApi == null) {
            synchronized (ApiClient.class) {
                if (starUserApi == null) {
                    starUserApi = createRetrofit().create(StarUserApi.class);
                }
            }
        }
        return starUserApi;
    }

    private static Retrofit createRetrofit() {
        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(createClient())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    private static OkHttpClient createClient() {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BASIC);
        return new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .build();
    }
}
