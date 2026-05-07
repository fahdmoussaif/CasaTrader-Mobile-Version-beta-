package com.fahd.casatrader.ui.auth;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.fahd.casatrader.data.remote.ApiClient;
import com.fahd.casatrader.data.repo.AuthRepository;
import com.fahd.casatrader.util.TokenStore;
import android.content.Context;

public class AuthViewModelFactory implements ViewModelProvider.Factory {
    private final Context appContext;

    public AuthViewModelFactory(Context appContext) {
        this.appContext = appContext.getApplicationContext();
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(AuthViewModel.class)) {
            TokenStore ts = TokenStore.getInstance(appContext);
            ApiClient api = ApiClient.getInstance(ts);
            return (T) new AuthViewModel(new AuthRepository(api, ts));
        }
        throw new IllegalArgumentException("Unknown ViewModel: " + modelClass);
    }
}