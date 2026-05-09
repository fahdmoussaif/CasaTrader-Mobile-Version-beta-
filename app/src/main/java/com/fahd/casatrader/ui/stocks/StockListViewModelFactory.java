package com.fahd.casatrader.ui.stocks;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.fahd.casatrader.data.remote.ApiClient;
import com.fahd.casatrader.data.repo.StockRepository;
import com.fahd.casatrader.util.TokenStore;

public class StockListViewModelFactory implements ViewModelProvider.Factory {
    private final Context appContext;
    public StockListViewModelFactory(Context appContext) { this.appContext = appContext.getApplicationContext(); }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(StockListViewModel.class)) {
            ApiClient api = ApiClient.getInstance(TokenStore.getInstance(appContext));
            return (T) new StockListViewModel(new StockRepository(api));
        }
        throw new IllegalArgumentException("Unknown ViewModel: " + modelClass);
    }
}