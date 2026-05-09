package com.fahd.casatrader.data.remote;

import com.fahd.casatrader.BuildConfig;
import com.fahd.casatrader.data.model.AuthDtos.AuthSession;
import com.fahd.casatrader.data.model.AuthDtos.RefreshRequest;
import com.fahd.casatrader.util.TokenStore;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    private static volatile ApiClient instance;

    private final TokenStore tokenStore;
    private final Retrofit mainRetrofit;
    private final AuthApi bareAuthApi;

    private ApiClient(TokenStore tokenStore) {
        this.tokenStore = tokenStore;

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(BuildConfig.DEBUG
                ? HttpLoggingInterceptor.Level.BODY
                : HttpLoggingInterceptor.Level.NONE);

        OkHttpClient bareClient = new OkHttpClient.Builder()
                .addInterceptor(new AuthInterceptor(tokenStore))
                .addInterceptor(logging)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build();

        Retrofit bareRetrofit = new Retrofit.Builder()
                .baseUrl(BuildConfig.SUPABASE_URL + "/")
                .client(bareClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        bareAuthApi = bareRetrofit.create(AuthApi.class);

        OkHttpClient mainClient = new OkHttpClient.Builder()
                .addInterceptor(new AuthInterceptor(tokenStore))
                .addInterceptor(new TokenRefreshInterceptor(tokenStore, this::refreshSync))
                .addInterceptor(logging)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build();

        mainRetrofit = new Retrofit.Builder()
                .baseUrl(BuildConfig.SUPABASE_URL + "/")
                .client(mainClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    public static ApiClient getInstance(TokenStore tokenStore) {
        if (instance == null) {
            synchronized (ApiClient.class) {
                if (instance == null) instance = new ApiClient(tokenStore);
            }
        }
        return instance;
    }

    public <T> T create(Class<T> serviceClass) {
        return mainRetrofit.create(serviceClass);
    }

    public boolean refreshSync() {
        String refreshToken = tokenStore.getRefreshToken();
        if (refreshToken == null) return false;

        try {
            Response<AuthSession> response = bareAuthApi
                    .refresh("refresh_token", new RefreshRequest(refreshToken))
                    .execute();

            if (!response.isSuccessful() || response.body() == null) return false;

            AuthSession s = response.body();
            if (s.accessToken == null) return false;

            long expiresAt = s.expiresAt != null
                    ? s.expiresAt
                    : System.currentTimeMillis() / 1000L + (s.expiresIn != null ? s.expiresIn : 3600L);
            String userId = s.user != null ? s.user.id : tokenStore.getUserId();

            tokenStore.saveSession(s.accessToken, s.refreshToken, expiresAt, userId);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}