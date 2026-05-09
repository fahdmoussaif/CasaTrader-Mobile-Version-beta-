package com.fahd.casatrader.ui.watchlist;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.fahd.casatrader.data.model.WatchlistEntry;
import com.fahd.casatrader.data.repo.WatchlistRepository;

import java.util.ArrayList;
import java.util.List;

public class WatchlistViewModel extends ViewModel {

    public enum LoadState { IDLE, LOADING, SUCCESS, ERROR }

    public static class UiState {
        public final LoadState loadState;
        public final List<WatchlistEntry> entries;
        public final String errorMessage;

        private UiState(LoadState s, List<WatchlistEntry> e, String err) {
            this.loadState = s; this.entries = e; this.errorMessage = err;
        }
        public static UiState loading()                            { return new UiState(LoadState.LOADING, new ArrayList<>(), null); }
        public static UiState success(List<WatchlistEntry> data)   { return new UiState(LoadState.SUCCESS, data, null); }
        public static UiState error(String m)                      { return new UiState(LoadState.ERROR, new ArrayList<>(), m); }
    }

    private final WatchlistRepository repo;
    private final MutableLiveData<UiState> uiState = new MutableLiveData<>(UiState.loading());

    public WatchlistViewModel(WatchlistRepository repo) { this.repo = repo; }
    public LiveData<UiState> getUiState() { return uiState; }

    public void load() {
        uiState.setValue(UiState.loading());
        repo.getAll(new WatchlistRepository.Callback<List<WatchlistEntry>>() {
            @Override public void onSuccess(List<WatchlistEntry> data) { uiState.postValue(UiState.success(data)); }
            @Override public void onError(String m) { uiState.postValue(UiState.error(m)); }
        });
    }

    
    public void remove(String ticker) {
        UiState current = uiState.getValue();
        if (current != null && current.loadState == LoadState.SUCCESS) {
            List<WatchlistEntry> filtered = new ArrayList<>();
            for (WatchlistEntry e : current.entries) {
                if (!ticker.equals(e.ticker)) filtered.add(e);
            }
            uiState.setValue(UiState.success(filtered));
        }
        repo.remove(ticker, new WatchlistRepository.Callback<Void>() {
            @Override public void onSuccess(Void data) {  }
            @Override public void onError(String m) { load();  }
        });
    }
}