package com.fahd.casatrader.data.repo;

import androidx.annotation.NonNull;

import com.fahd.casatrader.data.model.HoldingWithStock;
import com.fahd.casatrader.data.model.Profile;
import com.fahd.casatrader.data.remote.ApiClient;
import com.fahd.casatrader.data.remote.SupabaseApi;
import com.fahd.casatrader.util.TokenStore;

import java.io.IOException;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

public class PortfolioRepository {

    public interface Callback<T> {
        void onSuccess(T data);
        void onError(String message);
    }

    private final SupabaseApi api;
    private final TokenStore tokenStore;

    public PortfolioRepository(ApiClient apiClient, TokenStore tokenStore) {
        this.api = apiClient.create(SupabaseApi.class);
        this.tokenStore = tokenStore;
    }

    public void getProfile(@NonNull Callback<Profile> cb) {
        String userId = tokenStore.getUserId();
        if (userId == null) { cb.onError("Not logged in"); return; }
        api.getMyProfileSingle("application/vnd.pgrst.object+json",
                        "eq." + userId, "id,username,email,cash_balance")
                .enqueue(new retrofit2.Callback<Profile>() {
                    @Override public void onResponse(@NonNull Call<Profile> c, @NonNull Response<Profile> r) {
                        if (r.isSuccessful() && r.body() != null) cb.onSuccess(r.body());
                        else cb.onError(parseError(r));
                    }
                    @Override public void onFailure(@NonNull Call<Profile> c, @NonNull Throwable t) {
                        cb.onError("Network error: " + t.getMessage());
                    }
                });
    }

    public void getHoldings(@NonNull Callback<List<HoldingWithStock>> cb) {
        api.getMyHoldingsWithStock(
                        "*,stocks(ticker,name,price,change_percent)",
                        "ticker.asc")
                .enqueue(new retrofit2.Callback<List<HoldingWithStock>>() {
                    @Override public void onResponse(@NonNull Call<List<HoldingWithStock>> c,
                                                     @NonNull Response<List<HoldingWithStock>> r) {
                        if (r.isSuccessful() && r.body() != null) cb.onSuccess(r.body());
                        else cb.onError(parseError(r));
                    }
                    @Override public void onFailure(@NonNull Call<List<HoldingWithStock>> c,
                                                    @NonNull Throwable t) {
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