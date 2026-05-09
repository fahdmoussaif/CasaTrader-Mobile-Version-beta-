package com.fahd.casatrader.data.remote;

import androidx.annotation.NonNull;

import com.fahd.casatrader.util.TokenStore;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class TokenRefreshInterceptor implements Interceptor {

    public interface Refresher {
        boolean refresh();
    }

    private final TokenStore tokenStore;
    private final Refresher refresher;
    private final Object refreshLock = new Object();

    public TokenRefreshInterceptor(TokenStore tokenStore, Refresher refresher) {
        this.tokenStore = tokenStore;
        this.refresher = refresher;
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request request = chain.request();
        Response response = chain.proceed(request);

        if (response.code() != 401) return response;
        if (!isRestRequest(request)) return response;
        if (!tokenStore.isLoggedIn()) return response;

        String tokenAtFailure = extractBearer(request);

        boolean refreshed;
        synchronized (refreshLock) {
            String currentToken = tokenStore.getAccessToken();

            if (currentToken != null && !currentToken.equals(tokenAtFailure)) {
                refreshed = true;
            } else {
                response.close();
                refreshed = refresher.refresh();
                if (!refreshed) {
                    tokenStore.clear();
                    return chain.proceed(request);
                }
            }
        }

        response.close();
        return chain.proceed(request.newBuilder().build());
    }

    private static boolean isRestRequest(Request request) {
        return request.url().encodedPath().startsWith("/rest/v1/");
    }

    private static String extractBearer(Request request) {
        String header = request.header("Authorization");
        if (header == null) return null;
        if (header.startsWith("Bearer ")) return header.substring("Bearer ".length());
        return header;
    }
}