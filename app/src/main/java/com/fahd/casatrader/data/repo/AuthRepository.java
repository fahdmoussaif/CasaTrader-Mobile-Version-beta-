package com.fahd.casatrader.data.repo;

import androidx.annotation.NonNull;

import com.fahd.casatrader.data.model.AuthDtos.AuthSession;
import com.fahd.casatrader.data.model.AuthDtos.EmailPasswordRequest;
import com.fahd.casatrader.data.model.AuthDtos.RefreshRequest;
import com.fahd.casatrader.data.remote.ApiClient;
import com.fahd.casatrader.data.remote.AuthApi;
import com.fahd.casatrader.util.TokenStore;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthRepository {

    public interface AuthCallback {
        void onSuccess();
        void onError(String message);
    }

    private final AuthApi authApi;
    private final TokenStore tokenStore;

    public AuthRepository(ApiClient apiClient, TokenStore tokenStore) {
        this.authApi = apiClient.create(AuthApi.class);
        this.tokenStore = tokenStore;
    }

    public void signup(String email, String password, String username,
                       @NonNull AuthCallback cb) {
        Map<String, String> meta = new HashMap<>();
        meta.put("username", username);
        EmailPasswordRequest body = new EmailPasswordRequest(email, password, meta);

        authApi.signup(body).enqueue(new Callback<AuthSession>() {
            @Override
            public void onResponse(@NonNull Call<AuthSession> call,
                                   @NonNull Response<AuthSession> response) {
                handleSession(response, cb);
            }
            @Override
            public void onFailure(@NonNull Call<AuthSession> call, @NonNull Throwable t) {
                cb.onError("Network error: " + t.getMessage());
            }
        });
    }

    public void login(String email, String password, @NonNull AuthCallback cb) {
        EmailPasswordRequest body = new EmailPasswordRequest(email, password);
        authApi.login("password", body).enqueue(new Callback<AuthSession>() {
            @Override
            public void onResponse(@NonNull Call<AuthSession> call,
                                   @NonNull Response<AuthSession> response) {
                handleSession(response, cb);
            }
            @Override
            public void onFailure(@NonNull Call<AuthSession> call, @NonNull Throwable t) {
                cb.onError("Network error: " + t.getMessage());
            }
        });
    }

    public void logout() { tokenStore.clear(); }

    private void handleSession(Response<AuthSession> response, AuthCallback cb) {
        if (!response.isSuccessful() || response.body() == null) {
            cb.onError(parseError(response));
            return;
        }
        AuthSession s = response.body();
        if (s.accessToken == null) {
            cb.onError("Email confirmation required. Disable it in Supabase dashboard for dev.");
            return;
        }
        long expiresAt = s.expiresAt != null
                ? s.expiresAt
                : System.currentTimeMillis() / 1000L + (s.expiresIn != null ? s.expiresIn : 3600L);
        String userId = s.user != null ? s.user.id : null;

        tokenStore.saveSession(s.accessToken, s.refreshToken, expiresAt, userId);
        cb.onSuccess();
    }

    private String parseError(Response<?> response) {
        try {
            if (response.errorBody() != null) {
                String raw = response.errorBody().string();
                // Supabase auth errors look like {"error":"invalid_grant","error_description":"..."}
                // or {"msg":"...","code":...}. Cheap and cheerful: just show what we got.
                return "Auth failed (" + response.code() + "): " + raw;
            }
        } catch (IOException ignored) {}
        return "Auth failed (" + response.code() + ")";
    }
}