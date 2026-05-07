package com.fahd.casatrader.data.remote;

import com.fahd.casatrader.data.model.AuthDtos.AuthSession;
import com.fahd.casatrader.data.model.AuthDtos.EmailPasswordRequest;
import com.fahd.casatrader.data.model.AuthDtos.RefreshRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface AuthApi {

    /** Create a new user. With email confirmation OFF, returns a full session. */
    @POST("auth/v1/signup")
    Call<AuthSession> signup(@Body EmailPasswordRequest body);

    /** Login. ?grant_type=password is required. */
    @POST("auth/v1/token")
    Call<AuthSession> login(@Query("grant_type") String grantType,
                            @Body EmailPasswordRequest body);

    /** Refresh an expired JWT. */
    @POST("auth/v1/token")
    Call<AuthSession> refresh(@Query("grant_type") String grantType,
                              @Body RefreshRequest body);
}