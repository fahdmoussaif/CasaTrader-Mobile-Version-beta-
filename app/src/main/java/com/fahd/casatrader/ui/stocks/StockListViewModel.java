package com.fahd.casatrader.ui.stocks;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.fahd.casatrader.data.model.Stock;
import com.fahd.casatrader.data.repo.StockRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StockListViewModel extends ViewModel {

    public enum LoadState { IDLE, LOADING, SUCCESS, ERROR }

    public static class UiState {
        public final LoadState loadState;
        public final List<Stock> stocks;       // filtered list (what the adapter shows)
        public final String errorMessage;

        private UiState(LoadState s, List<Stock> stocks, String e) {
            this.loadState = s; this.stocks = stocks; this.errorMessage = e;
        }
        public static UiState idle()                          { return new UiState(LoadState.IDLE, new ArrayList<>(), null); }
        public static UiState loading()                       { return new UiState(LoadState.LOADING, new ArrayList<>(), null); }
        public static UiState success(List<Stock> stocks)     { return new UiState(LoadState.SUCCESS, stocks, null); }
        public static UiState error(String message)           { return new UiState(LoadState.ERROR, new ArrayList<>(), message); }
    }

    private final StockRepository repo;
    private final MutableLiveData<UiState> uiState = new MutableLiveData<>(UiState.idle());

    private List<Stock> allStocks = new ArrayList<>();   // unfiltered, kept for client-side search
    private String currentQuery = "";

    public StockListViewModel(StockRepository repo) { this.repo = repo; }

    public LiveData<UiState> getUiState() { return uiState; }

    public void load() {
        uiState.setValue(UiState.loading());
        repo.getAllStocks(new StockRepository.Callback<List<Stock>>() {
            @Override
            public void onSuccess(List<Stock> data) {
                allStocks = data;
                uiState.postValue(UiState.success(filter(allStocks, currentQuery)));
            }
            @Override
            public void onError(String message) {
                uiState.postValue(UiState.error(message));
            }
        });
    }

    public void setSearchQuery(String query) {
        currentQuery = query == null ? "" : query.trim();
        // Re-filter from cached list — no network roundtrip per keystroke.
        if (uiState.getValue() != null && uiState.getValue().loadState == LoadState.SUCCESS) {
            uiState.setValue(UiState.success(filter(allStocks, currentQuery)));
        }
    }

    private static List<Stock> filter(List<Stock> source, String query) {
        if (query.isEmpty()) return source;
        String q = query.toLowerCase(Locale.ROOT);
        List<Stock> out = new ArrayList<>(source.size());
        for (Stock s : source) {
            boolean matchesTicker = s.ticker != null && s.ticker.toLowerCase(Locale.ROOT).contains(q);
            boolean matchesName = s.name != null && s.name.toLowerCase(Locale.ROOT).contains(q);
            if (matchesTicker || matchesName) out.add(s);
        }
        return out;
    }
}