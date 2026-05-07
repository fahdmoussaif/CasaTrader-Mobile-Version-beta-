package com.fahd.casatrader;

import android.content.Intent;
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
        setContentView(R.layout.activity_main);

        TokenStore ts = TokenStore.getInstance(this);
        String userId = ts.getUserId();
        String token = ts.getAccessToken();
        android.util.Log.d("CasaTrader", "Logged in as " + userId
                + ", token starts with " + (token == null ? "null" : token.substring(0, 12)));
        SupabaseApi api = ApiClient.getInstance(ts).create(SupabaseApi.class);
        api.getMyProfileSingle("application/vnd.pgrst.object+json", "eq." + ts.getUserId(),
                "id,username,email,cash_balance").enqueue(new retrofit2.Callback<com.fahd.casatrader.data.model.Profile>() {
            @Override
            public void onResponse(@NonNull retrofit2.Call<com.fahd.casatrader.data.model.Profile> call,
                                   @NonNull retrofit2.Response<com.fahd.casatrader.data.model.Profile> response) {
                if (response.isSuccessful() && response.body() != null) {
                    android.util.Log.d("CasaTrader", "Profile: " + response.body().username
                            + " | balance: " + response.body().cashBalance);
                } else {
                    android.util.Log.e("CasaTrader", "Profile fetch failed: " + response.code());
                }
            }
            @Override
            public void onFailure(@NonNull retrofit2.Call<com.fahd.casatrader.data.model.Profile> call,
                                  @NonNull Throwable t) {
                android.util.Log.e("CasaTrader", "Profile fetch error", t);
            }
        });

        // In MainActivity.onCreate, attached to whatever placeholder view you have
        findViewById(R.id.Logout).setOnLongClickListener(v -> {
            TokenStore.getInstance(this).clear();
            startActivity(new Intent(this, com.fahd.casatrader.ui.auth.LoginActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
            return true;
        });

        // TODO step 5: replace with stock list
    }
}