package com.fahd.casatrader;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.fahd.casatrader.data.remote.ApiClient;
import com.fahd.casatrader.data.remote.SupabaseApi;
import com.fahd.casatrader.util.TokenStore;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        SupabaseApi api = ApiClient
                .getInstance(TokenStore.getInstance(this))
                .create(SupabaseApi.class);

        api.pingStocks("ticker,name", 3).enqueue(new retrofit2.Callback<okhttp3.ResponseBody>() {
            @Override
            public void onResponse(@NonNull retrofit2.Call<okhttp3.ResponseBody> call,
                                   @NonNull retrofit2.Response<okhttp3.ResponseBody> response) {
                try {
                    String body = response.body() != null ? response.body().string() : "null";
                    android.util.Log.d("CasaTrader", "Ping " + response.code() + " → " + body);
                } catch (java.io.IOException e) {
                    android.util.Log.e("CasaTrader", "Read error", e);
                }
            }
            @Override
            public void onFailure(@NonNull retrofit2.Call<okhttp3.ResponseBody> call,
                                  @NonNull Throwable t) {
                android.util.Log.e("CasaTrader", "Ping failed", t);
            }
        });
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}