package com.fahd.casatrader.ui.auth;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.viewmodel.CreationExtras;

import com.fahd.casatrader.data.remote.ApiClient;
import com.fahd.casatrader.data.repo.AuthRepository;
import com.fahd.casatrader.util.TokenStore;

public class AuthViewModelFactory implements ViewModelProvider.Factory {
    private final Context appContext;

    public AuthViewModelFactory(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
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
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass, @NonNull CreationExtras extras) {
        return create(modelClass);
    }
}