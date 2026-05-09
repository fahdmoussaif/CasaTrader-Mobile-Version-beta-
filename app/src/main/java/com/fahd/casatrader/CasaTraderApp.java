package com.fahd.casatrader;

import android.app.Application;
import androidx.appcompat.app.AppCompatDelegate;
import com.fahd.casatrader.data.remote.ApiClient;
import com.fahd.casatrader.util.TokenStore;

public class CasaTraderApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        TokenStore tokenStore = TokenStore.getInstance(this);
        ApiClient.getInstance(tokenStore);
    }
}