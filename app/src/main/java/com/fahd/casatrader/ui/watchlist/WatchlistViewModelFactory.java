package com.fahd.casatrader.ui.watchlist;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.fahd.casatrader.data.remote.ApiClient;
import com.fahd.casatrader.data.repo.WatchlistRepository;
import com.fahd.casatrader.util.TokenStore;

public class WatchlistViewModelFactory implements ViewModelProvider.Factory {
    private final Context appContext;
    public WatchlistViewModelFactory(Context appContext) { this.appContext = appContext.getApplicationContext(); }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(WatchlistViewModel.class)) {
            TokenStore ts = TokenStore.getInstance(appContext);
            return (T) new WatchlistViewModel(new WatchlistRepository(ApiClient.getInstance(ts), ts));
        }
        throw new IllegalArgumentException("Unknown ViewModel: " + modelClass);
    }
}