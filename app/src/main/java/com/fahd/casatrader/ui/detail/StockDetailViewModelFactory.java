package com.fahd.casatrader.ui.detail;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.fahd.casatrader.data.remote.ApiClient;
import com.fahd.casatrader.data.repo.StockRepository;
import com.fahd.casatrader.util.TokenStore;

public class StockDetailViewModelFactory implements ViewModelProvider.Factory {
    private final Context appContext;
    private final String ticker;

    public StockDetailViewModelFactory(Context appContext, String ticker) {
        this.appContext = appContext.getApplicationContext();
        this.ticker = ticker;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(StockDetailViewModel.class)) {
            ApiClient api = ApiClient.getInstance(TokenStore.getInstance(appContext));
            return (T) new StockDetailViewModel(new StockRepository(api), ticker);
        }
        throw new IllegalArgumentException("Unknown ViewModel: " + modelClass);
    }
}