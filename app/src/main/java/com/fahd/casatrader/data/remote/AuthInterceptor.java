package com.fahd.casatrader.data.remote;

import androidx.annotation.NonNull;
import com.fahd.casatrader.BuildConfig;
import com.fahd.casatrader.util.TokenStore;
import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AuthInterceptor implements Interceptor {
    private final TokenStore tokenStore;

    public AuthInterceptor(TokenStore tokenStore) { this.tokenStore = tokenStore; }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request original = chain.request();
        String anonKey   = BuildConfig.SUPABASE_ANON_KEY;
        String userToken = tokenStore.getAccessToken();
        String bearer    = userToken != null ? userToken : anonKey;

        Request authenticated = original.newBuilder()
                .header("apikey", anonKey)
                .header("Authorization", "Bearer " + bearer)
                .build();

        return chain.proceed(authenticated);
    }
}