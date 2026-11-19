// private static final String BASE_URL = "http://10.0.2.2:8000/";
//private static final String BASE_URL = "http://192.168.1:8000/";

package com.example.quizapledu.api;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.concurrent.TimeUnit;

public class RetrofitClient {

   // private static final String BASE_URL = "http://192.168.1.5:8000/";
   // private static final String BASE_URL = "http://192.168.1.7:8000/";
   private static final String BASE_URL = "http://10.61.140.121:8000/";

    private static Retrofit retrofit = null;

    public static Retrofit getClient() {
        if (retrofit == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .connectTimeout(60, TimeUnit.SECONDS)  // 60 secunde connect
                    .readTimeout(60, TimeUnit.SECONDS)     // 60 secunde read
                    .writeTimeout(60, TimeUnit.SECONDS)    // 60 secunde write
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    public static ApiService getApiService() {
        return getClient().create(ApiService.class);
    }
}