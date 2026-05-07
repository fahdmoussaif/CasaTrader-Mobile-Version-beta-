package com.fahd.casatrader.ui.auth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.fahd.casatrader.data.repo.AuthRepository;

public class AuthViewModel extends ViewModel {

    public enum State { IDLE, LOADING, SUCCESS, ERROR }

    public static class UiState {
        public final State state;
        public final String errorMessage;   // null unless state == ERROR

        private UiState(State s, String e) { this.state = s; this.errorMessage = e; }

        public static UiState idle()                 { return new UiState(State.IDLE, null); }
        public static UiState loading()              { return new UiState(State.LOADING, null); }
        public static UiState success()              { return new UiState(State.SUCCESS, null); }
        public static UiState error(String message)  { return new UiState(State.ERROR, message); }
    }

    private final AuthRepository repo;
    private final MutableLiveData<UiState> uiState = new MutableLiveData<>(UiState.idle());

    public AuthViewModel(AuthRepository repo) { this.repo = repo; }

    public LiveData<UiState> getUiState() { return uiState; }

    public void login(String email, String password) {
        if (email.isEmpty() || password.isEmpty()) {
            uiState.setValue(UiState.error("Email and password required")); return;
        }
        uiState.setValue(UiState.loading());
        repo.login(email, password, new AuthRepository.AuthCallback() {
            @Override public void onSuccess() { uiState.postValue(UiState.success()); }
            @Override public void onError(String m) { uiState.postValue(UiState.error(m)); }
        });
    }

    public void signup(String email, String password, String username) {
        if (email.isEmpty() || password.isEmpty() || username.isEmpty()) {
            uiState.setValue(UiState.error("All fields required")); return;
        }
        if (password.length() < 6) {
            uiState.setValue(UiState.error("Password must be at least 6 characters")); return;
        }
        uiState.setValue(UiState.loading());
        repo.signup(email, password, username, new AuthRepository.AuthCallback() {
            @Override public void onSuccess() { uiState.postValue(UiState.success()); }
            @Override public void onError(String m) { uiState.postValue(UiState.error(m)); }
        });
    }

    /** Reset to idle after the activity has handled an error/success. */
    public void resetState() { uiState.setValue(UiState.idle()); }
}