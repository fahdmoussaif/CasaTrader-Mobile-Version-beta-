package com.fahd.casatrader.data.repo;

import androidx.annotation.NonNull;

import com.fahd.casatrader.data.model.Holding;
import com.fahd.casatrader.data.model.Profile;
import com.fahd.casatrader.data.model.WriteDtos.BuyRequest;
import com.fahd.casatrader.data.model.WriteDtos.SellRequest;
import com.fahd.casatrader.data.model.WriteDtos.TradeResult;
import com.fahd.casatrader.data.remote.ApiClient;
import com.fahd.casatrader.data.remote.SupabaseApi;
import com.fahd.casatrader.util.TokenStore;

import java.io.IOException;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

public class TradeRepository {

    public interface Callback<T> {
        void onSuccess(T data);
        void onError(String message);
    }

    private final SupabaseApi api;
    private final TokenStore tokenStore;

    public TradeRepository(ApiClient apiClient, TokenStore tokenStore) {
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

    public void getHolding(String ticker, @NonNull Callback<Holding> cb) {
        api.getMyHolding("eq." + ticker, "*").enqueue(new retrofit2.Callback<List<Holding>>() {
            @Override public void onResponse(@NonNull Call<List<Holding>> c, @NonNull Response<List<Holding>> r) {
                if (r.isSuccessful() && r.body() != null) {
                    cb.onSuccess(r.body().isEmpty() ? null : r.body().get(0));
                } else cb.onError(parseError(r));
            }
            @Override public void onFailure(@NonNull Call<List<Holding>> c, @NonNull Throwable t) {
                cb.onError("Network error: " + t.getMessage());
            }
        });
    }

    public void buy(String ticker, int shares, double price, @NonNull Callback<TradeResult> cb) {
        api.buyStock(new BuyRequest(ticker, shares, price))
                .enqueue(new retrofit2.Callback<TradeResult>() {
                    @Override public void onResponse(@NonNull Call<TradeResult> c, @NonNull Response<TradeResult> r) {
                        if (r.isSuccessful() && r.body() != null) cb.onSuccess(r.body());
                        else cb.onError(parsePostgresError(r));
                    }
                    @Override public void onFailure(@NonNull Call<TradeResult> c, @NonNull Throwable t) {
                        cb.onError("Network error: " + t.getMessage());
                    }
                });
    }

    public void sell(String ticker, int shares, double price, @NonNull Callback<TradeResult> cb) {
        api.sellStock(new SellRequest(ticker, shares, price))
                .enqueue(new retrofit2.Callback<TradeResult>() {
                    @Override public void onResponse(@NonNull Call<TradeResult> c, @NonNull Response<TradeResult> r) {
                        if (r.isSuccessful() && r.body() != null) cb.onSuccess(r.body());
                        else cb.onError(parsePostgresError(r));
                    }
                    @Override public void onFailure(@NonNull Call<TradeResult> c, @NonNull Throwable t) {
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

    private String parsePostgresError(Response<?> response) {
        try {
            if (response.errorBody() != null) {
                String raw = response.errorBody().string();
                int msgStart = raw.indexOf("\"message\":\"");
                if (msgStart >= 0) {
                    int valStart = msgStart + "\"message\":\"".length();
                    int valEnd = raw.indexOf("\"", valStart);
                    if (valEnd > valStart) return humanize(raw.substring(valStart, valEnd));
                }
                return raw;
            }
        } catch (IOException ignored) {}
        return "Trade failed (" + response.code() + ")";
    }

    private String humanize(String msg) {
        if (msg.startsWith("insufficient_funds")) return "Not enough cash for this purchase.";
        if (msg.startsWith("insufficient_shares")) return "You don't own enough shares to sell that many.";
        if (msg.startsWith("no_holding")) return "You don't own this stock.";
        if (msg.startsWith("invalid_shares")) return "Invalid share count.";
        if (msg.startsWith("invalid_price")) return "Invalid price.";
        if (msg.startsWith("not_authenticated")) return "Please log in again.";
        if (msg.startsWith("unknown_ticker")) return "Unknown stock ticker.";
        return msg;
    }
}