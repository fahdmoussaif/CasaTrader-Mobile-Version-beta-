package com.fahd.casatrader.data.repo;

import androidx.annotation.NonNull;

import com.fahd.casatrader.data.model.Stock;
import com.fahd.casatrader.data.remote.ApiClient;
import com.fahd.casatrader.data.remote.SupabaseApi;

import java.io.IOException;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StockRepository {

    public interface Callback<T> {
        void onSuccess(T data);
        void onError(String message);
    }

    private final SupabaseApi api;

    public StockRepository(ApiClient apiClient) {
        this.api = apiClient.create(SupabaseApi.class);
    }

    public void getAllStocks(@NonNull Callback<List<Stock>> cb) {
        api.getAllStocks("*", "name.asc").enqueue(new retrofit2.Callback<List<Stock>>() {
            @Override
            public void onResponse(@NonNull Call<List<Stock>> call,
                                   @NonNull Response<List<Stock>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    cb.onSuccess(response.body());
                } else {
                    cb.onError(parseError(response));
                }
            }
            @Override
            public void onFailure(@NonNull Call<List<Stock>> call, @NonNull Throwable t) {
                cb.onError("Network error: " + t.getMessage());
            }
        });
    }

    private String parseError(Response<?> response) {
        try {
            if (response.errorBody() != null) {
                return "HTTP " + response.code() + ": " + response.errorBody().string();
            }
        } catch (IOException ignored) {}
        return "HTTP " + response.code();
    }
}
