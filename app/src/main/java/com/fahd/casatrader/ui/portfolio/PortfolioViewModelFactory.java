package com.fahd.casatrader.ui.portfolio;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.fahd.casatrader.data.remote.ApiClient;
import com.fahd.casatrader.data.repo.PortfolioRepository;
import com.fahd.casatrader.util.TokenStore;

public class PortfolioViewModelFactory implements ViewModelProvider.Factory {
    private final Context appContext;
    public PortfolioViewModelFactory(Context appContext) { this.appContext = appContext.getApplicationContext(); }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(PortfolioViewModel.class)) {
            TokenStore ts = TokenStore.getInstance(appContext);
            ApiClient api = ApiClient.getInstance(ts);
            return (T) new PortfolioViewModel(new PortfolioRepository(api, ts));
        }
        throw new IllegalArgumentException("Unknown ViewModel: " + modelClass);
    }
}