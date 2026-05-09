package com.fahd.casatrader.ui.transactions;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.fahd.casatrader.data.remote.ApiClient;
import com.fahd.casatrader.data.repo.TransactionsRepository;
import com.fahd.casatrader.util.TokenStore;

public class TransactionsViewModelFactory implements ViewModelProvider.Factory {
    private final Context appContext;
    public TransactionsViewModelFactory(Context appContext) { this.appContext = appContext.getApplicationContext(); }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(TransactionsViewModel.class)) {
            ApiClient api = ApiClient.getInstance(TokenStore.getInstance(appContext));
            return (T) new TransactionsViewModel(new TransactionsRepository(api));
        }
        throw new IllegalArgumentException("Unknown ViewModel: " + modelClass);
    }
}