package com.fahd.casatrader.ui.detail;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.fahd.casatrader.data.model.PriceSnapshot;
import com.fahd.casatrader.data.model.Stock;
import com.fahd.casatrader.data.repo.StockRepository;

import java.util.List;

public class StockDetailViewModel extends ViewModel {

    public enum LoadState { IDLE, LOADING, SUCCESS, ERROR }

    public static class StockState {
        public final LoadState loadState;
        public final Stock stock;
        public final String errorMessage;
        private StockState(LoadState s, Stock stock, String e) {
            this.loadState = s; this.stock = stock; this.errorMessage = e;
        }
        public static StockState loading()                 { return new StockState(LoadState.LOADING, null, null); }
        public static StockState success(Stock s)          { return new StockState(LoadState.SUCCESS, s, null); }
        public static StockState error(String m)           { return new StockState(LoadState.ERROR, null, m); }
    }

    public static class HistoryState {
        public final LoadState loadState;
        public final List<PriceSnapshot> history;
        public final String errorMessage;
        private HistoryState(LoadState s, List<PriceSnapshot> h, String e) {
            this.loadState = s; this.history = h; this.errorMessage = e;
        }
        public static HistoryState loading()                          { return new HistoryState(LoadState.LOADING, null, null); }
        public static HistoryState success(List<PriceSnapshot> h)     { return new HistoryState(LoadState.SUCCESS, h, null); }
        public static HistoryState error(String m)                    { return new HistoryState(LoadState.ERROR, null, m); }
    }

    private final StockRepository repo;
    private final String ticker;

    private final MutableLiveData<StockState> stockState = new MutableLiveData<>();
    private final MutableLiveData<HistoryState> historyState = new MutableLiveData<>();

    public StockDetailViewModel(StockRepository repo, String ticker) {
        this.repo = repo;
        this.ticker = ticker;
    }

    public LiveData<StockState> getStockState()       { return stockState; }
    public LiveData<HistoryState> getHistoryState()   { return historyState; }

    public void load() { loadStock(); loadHistory(); }

    public void loadStock() {
        stockState.setValue(StockState.loading());
        repo.getStock(ticker, new StockRepository.Callback<Stock>() {
            @Override public void onSuccess(Stock data) { stockState.postValue(StockState.success(data)); }
            @Override public void onError(String message) { stockState.postValue(StockState.error(message)); }
        });
    }

    public void loadHistory() {
        historyState.setValue(HistoryState.loading());
        repo.getPriceHistory(ticker, 365, new StockRepository.Callback<List<PriceSnapshot>>() {
            @Override public void onSuccess(List<PriceSnapshot> data) { historyState.postValue(HistoryState.success(data)); }
            @Override public void onError(String message) { historyState.postValue(HistoryState.error(message)); }
        });
    }
}