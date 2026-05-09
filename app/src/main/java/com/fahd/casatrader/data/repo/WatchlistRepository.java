package com.fahd.casatrader.data.repo;

import androidx.annotation.NonNull;

import com.fahd.casatrader.data.model.WatchlistEntry;
import com.fahd.casatrader.data.model.WriteDtos.WatchlistInsert;
import com.fahd.casatrader.data.remote.ApiClient;
import com.fahd.casatrader.data.remote.SupabaseApi;
import com.fahd.casatrader.util.TokenStore;

import java.io.IOException;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

public class WatchlistRepository {

    public interface Callback<T> {
        void onSuccess(T data);
        void onError(String message);
    }

    private final SupabaseApi api;
    private final TokenStore tokenStore;

    public WatchlistRepository(ApiClient apiClient, TokenStore tokenStore) {
        this.api = apiClient.create(SupabaseApi.class);
        this.tokenStore = tokenStore;
    }

    public void getAll(@NonNull Callback<List<WatchlistEntry>> cb) {
        api.getMyWatchlist(
                        "ticker,added_at,stocks(ticker,name,price,change_percent,sector)",
                        "added_at.desc")
                .enqueue(new retrofit2.Callback<List<WatchlistEntry>>() {
                    @Override public void onResponse(@NonNull Call<List<WatchlistEntry>> c,
                                                     @NonNull Response<List<WatchlistEntry>> r) {
                        if (r.isSuccessful() && r.body() != null) cb.onSuccess(r.body());
                        else cb.onError(parseError(r));
                    }
                    @Override public void onFailure(@NonNull Call<List<WatchlistEntry>> c, @NonNull Throwable t) {
                        cb.onError("Network error: " + t.getMessage());
                    }
                });
    }

    public void isWatched(String ticker, @NonNull Callback<Boolean> cb) {
        api.getWatchlistEntry("eq." + ticker, "ticker").enqueue(new retrofit2.Callback<List<WatchlistEntry>>() {
            @Override public void onResponse(@NonNull Call<List<WatchlistEntry>> c,
                                             @NonNull Response<List<WatchlistEntry>> r) {
                if (r.isSuccessful() && r.body() != null) cb.onSuccess(!r.body().isEmpty());
                else cb.onError(parseError(r));
            }
            @Override public void onFailure(@NonNull Call<List<WatchlistEntry>> c, @NonNull Throwable t) {
                cb.onError("Network error: " + t.getMessage());
            }
        });
    }

    public void add(String ticker, @NonNull Callback<Void> cb) {
        String userId = tokenStore.getUserId();
        if (userId == null) { cb.onError("Not logged in"); return; }

        WatchlistInsert body = new WatchlistInsert();
        body.userId = userId;
        body.ticker = ticker;

        api.addToWatchlist("return=minimal", body).enqueue(new retrofit2.Callback<Void>() {
            @Override public void onResponse(@NonNull Call<Void> c, @NonNull Response<Void> r) {
                if (r.isSuccessful()) cb.onSuccess(null);
                else cb.onError(parseError(r));
            }
            @Override public void onFailure(@NonNull Call<Void> c, @NonNull Throwable t) {
                cb.onError("Network error: " + t.getMessage());
            }
        });
    }

    public void remove(String ticker, @NonNull Callback<Void> cb) {
        api.removeFromWatchlist("eq." + ticker).enqueue(new retrofit2.Callback<Void>() {
            @Override public void onResponse(@NonNull Call<Void> c, @NonNull Response<Void> r) {
                if (r.isSuccessful()) cb.onSuccess(null);
                else cb.onError(parseError(r));
            }
            @Override public void onFailure(@NonNull Call<Void> c, @NonNull Throwable t) {
                cb.onError("Network error: " + t.getMessage());
            }
        });
    }

    private String parseError(Response<?> response) {
        try {
            if (response.errorBody() != null)
                return "HTTP " + response.code() + ": " + response.errorBody().string();
        } catch (IOException ignored) {}
        return "HTTP " + response.code();
    }
}