package com.fahd.casatrader;

import android.app.Application;
import com.fahd.casatrader.data.remote.ApiClient;
import com.fahd.casatrader.util.TokenStore;

public class CasaTraderApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        TokenStore tokenStore = TokenStore.getInstance(this);
        ApiClient.getInstance(tokenStore);
    }
}