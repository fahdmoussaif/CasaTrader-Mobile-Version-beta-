package com.fahd.casatrader.data.remote;

import com.fahd.casatrader.data.model.AuthDtos.AuthSession;
import com.fahd.casatrader.data.model.AuthDtos.EmailPasswordRequest;
import com.fahd.casatrader.data.model.AuthDtos.RefreshRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface AuthApi {

    @POST("auth/v1/signup")
    Call<AuthSession> signup(@Body EmailPasswordRequest body);

    @POST("auth/v1/token")
    Call<AuthSession> login(@Query("grant_type") String grantType,
                            @Body EmailPasswordRequest body);

    @POST("auth/v1/token")
    Call<AuthSession> refresh(@Query("grant_type") String grantType,
                              @Body RefreshRequest body);
}