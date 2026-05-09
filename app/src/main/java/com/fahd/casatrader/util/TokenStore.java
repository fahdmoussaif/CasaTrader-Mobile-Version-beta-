package com.fahd.casatrader.util;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

public class TokenStore {
    private static final String PREFS_NAME = "casatrader_secure_prefs";
    private static final String KEY_ACCESS_TOKEN  = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_EXPIRES_AT    = "expires_at";
    private static final String KEY_USER_ID       = "user_id";

    private static volatile TokenStore instance;
    private final SharedPreferences prefs;

    private TokenStore(Context ctx) {
        try {
            MasterKey masterKey = new MasterKey.Builder(ctx)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
            prefs = EncryptedSharedPreferences.create(
                    ctx,
                    PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to init TokenStore", e);
        }
    }

    public static TokenStore getInstance(Context ctx) {
        if (instance == null) {
            synchronized (TokenStore.class) {
                if (instance == null) instance = new TokenStore(ctx.getApplicationContext());
            }
        }
        return instance;
    }

    public void saveSession(String accessToken, String refreshToken,
                            long expiresAtEpochSec, String userId) {
        prefs.edit()
                .putString(KEY_ACCESS_TOKEN, accessToken)
                .putString(KEY_REFRESH_TOKEN, refreshToken)
                .putLong(KEY_EXPIRES_AT, expiresAtEpochSec)
                .putString(KEY_USER_ID, userId)
                .apply();
    }

    public String getAccessToken()  { return prefs.getString(KEY_ACCESS_TOKEN, null); }
    public String getRefreshToken() { return prefs.getString(KEY_REFRESH_TOKEN, null); }
    public long   getExpiresAt()    { return prefs.getLong(KEY_EXPIRES_AT, 0L); }
    public String getUserId()       { return prefs.getString(KEY_USER_ID, null); }

    public boolean isLoggedIn() { return getAccessToken() != null; }

    public boolean isAccessTokenExpired() {
        long expires = getExpiresAt();
        if (expires == 0L) return true;
        
        return System.currentTimeMillis() / 1000L >= expires - 30;
    }

    public void clear() { prefs.edit().clear().apply(); }
}