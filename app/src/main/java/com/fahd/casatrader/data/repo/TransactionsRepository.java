package com.fahd.casatrader.data.repo;

import androidx.annotation.NonNull;

import com.fahd.casatrader.data.model.Transaction;
import com.fahd.casatrader.data.remote.ApiClient;
import com.fahd.casatrader.data.remote.SupabaseApi;

import java.io.IOException;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

public class TransactionsRepository {

    public interface Callback<T> {
        void onSuccess(T data);
        void onError(String message);
    }

    private final SupabaseApi api;

    public TransactionsRepository(ApiClient apiClient) {
        this.api = apiClient.create(SupabaseApi.class);
    }

    public void getAll(@NonNull Callback<List<Transaction>> cb) {
        api.getMyTransactions("*", "created_at.desc").enqueue(new retrofit2.Callback<List<Transaction>>() {
            @Override public void onResponse(@NonNull Call<List<Transaction>> c,
                                             @NonNull Response<List<Transaction>> r) {
                if (r.isSuccessful() && r.body() != null) cb.onSuccess(r.body());
                else cb.onError(parseError(r));
            }
            @Override public void onFailure(@NonNull Call<List<Transaction>> c, @NonNull Throwable t) {
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