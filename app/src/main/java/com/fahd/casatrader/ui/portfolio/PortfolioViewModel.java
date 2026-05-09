package com.fahd.casatrader.ui.portfolio;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.fahd.casatrader.data.model.HoldingWithStock;
import com.fahd.casatrader.data.model.Profile;
import com.fahd.casatrader.data.repo.PortfolioRepository;

import java.util.ArrayList;
import java.util.List;

public class PortfolioViewModel extends ViewModel {

    public enum LoadState { IDLE, LOADING, SUCCESS, ERROR }

    public static class UiState {
        public final LoadState loadState;
        public final Profile profile;
        public final List<HoldingWithStock> holdings;
        public final String errorMessage;

        private UiState(LoadState s, Profile p, List<HoldingWithStock> h, String e) {
            this.loadState = s; this.profile = p; this.holdings = h; this.errorMessage = e;
        }
        public static UiState loading()                                  { return new UiState(LoadState.LOADING, null, new ArrayList<>(), null); }
        public static UiState success(Profile p, List<HoldingWithStock> h){ return new UiState(LoadState.SUCCESS, p, h, null); }
        public static UiState error(String m)                            { return new UiState(LoadState.ERROR, null, new ArrayList<>(), m); }
    }

    private final PortfolioRepository repo;
    private final MutableLiveData<UiState> uiState = new MutableLiveData<>(UiState.loading());

    private Profile latestProfile;
    private List<HoldingWithStock> latestHoldings;
    private boolean profileLoaded = false;
    private boolean holdingsLoaded = false;
    private String pendingError;

    public PortfolioViewModel(PortfolioRepository repo) { this.repo = repo; }

    public LiveData<UiState> getUiState() { return uiState; }

    public void load() {
        uiState.setValue(UiState.loading());
        profileLoaded = false;
        holdingsLoaded = false;
        pendingError = null;
        latestProfile = null;
        latestHoldings = null;

        repo.getProfile(new PortfolioRepository.Callback<Profile>() {
            @Override public void onSuccess(Profile data) {
                latestProfile = data; profileLoaded = true; tryEmit();
            }
            @Override public void onError(String message) {
                pendingError = message; profileLoaded = true; tryEmit();
            }
        });

        repo.getHoldings(new PortfolioRepository.Callback<List<HoldingWithStock>>() {
            @Override public void onSuccess(List<HoldingWithStock> data) {
                latestHoldings = data; holdingsLoaded = true; tryEmit();
            }
            @Override public void onError(String message) {
                if (pendingError == null) pendingError = message;
                holdingsLoaded = true; tryEmit();
            }
        });
    }

    private void tryEmit() {
        if (!profileLoaded || !holdingsLoaded) return;
        if (pendingError != null && latestProfile == null) {
            uiState.postValue(UiState.error(pendingError));
        } else {
            uiState.postValue(UiState.success(
                    latestProfile,
                    latestHoldings != null ? latestHoldings : new ArrayList<>()));
        }
    }
}