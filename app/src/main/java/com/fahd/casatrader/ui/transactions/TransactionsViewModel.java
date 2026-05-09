package com.fahd.casatrader.ui.transactions;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.fahd.casatrader.data.model.Transaction;
import com.fahd.casatrader.data.repo.TransactionsRepository;

import java.util.ArrayList;
import java.util.List;

public class TransactionsViewModel extends ViewModel {

    public enum LoadState { IDLE, LOADING, SUCCESS, ERROR }

    public static class UiState {
        public final LoadState loadState;
        public final List<Transaction> transactions;
        public final String errorMessage;

        private UiState(LoadState s, List<Transaction> t, String e) {
            this.loadState = s; this.transactions = t; this.errorMessage = e;
        }
        public static UiState loading()                          { return new UiState(LoadState.LOADING, new ArrayList<>(), null); }
        public static UiState success(List<Transaction> txs)     { return new UiState(LoadState.SUCCESS, txs, null); }
        public static UiState error(String m)                    { return new UiState(LoadState.ERROR, new ArrayList<>(), m); }
    }

    private final TransactionsRepository repo;
    private final MutableLiveData<UiState> uiState = new MutableLiveData<>(UiState.loading());

    public TransactionsViewModel(TransactionsRepository repo) { this.repo = repo; }
    public LiveData<UiState> getUiState() { return uiState; }

    public void load() {
        uiState.setValue(UiState.loading());
        repo.getAll(new TransactionsRepository.Callback<List<Transaction>>() {
            @Override public void onSuccess(List<Transaction> data) { uiState.postValue(UiState.success(data)); }
            @Override public void onError(String m) { uiState.postValue(UiState.error(m)); }
        });
    }
}