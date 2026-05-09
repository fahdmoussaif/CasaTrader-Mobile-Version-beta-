package com.fahd.casatrader.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.fahd.casatrader.data.remote.ApiClient;
import com.fahd.casatrader.databinding.ActivityLoginBinding;
import com.fahd.casatrader.ui.stocks.StockListActivity;
import com.fahd.casatrader.util.TokenStore;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private AuthViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TokenStore tokenStore = TokenStore.getInstance(this);

        if (tokenStore.isLoggedIn()) {
            if (!tokenStore.isAccessTokenExpired()) {
                goHome();
                return;
            }
            // Token expired but we have a refresh token — try silently.
            showRefreshingState();
            new Thread(() -> {
                ApiClient client = ApiClient.getInstance(tokenStore);
                boolean ok = client.refreshSync();
                runOnUiThread(() -> {
                    if (ok) goHome();
                    else showLoginForm();
                });
            }).start();
            return;
        }

        showLoginForm();
    }

    private void showRefreshingState() {
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.loadingPb.setVisibility(View.VISIBLE);
        binding.loginBtn.setVisibility(View.GONE);
        binding.goToSignupBtn.setVisibility(View.GONE);
        binding.emailLayout.setVisibility(View.GONE);
        binding.passwordLayout.setVisibility(View.GONE);
        binding.titleTv.setVisibility(View.GONE);
    }

    private void showLoginForm() {
        if (binding == null) {
            binding = ActivityLoginBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());
        } else {
            binding.loginBtn.setVisibility(View.VISIBLE);
            binding.goToSignupBtn.setVisibility(View.VISIBLE);
            binding.emailLayout.setVisibility(View.VISIBLE);
            binding.passwordLayout.setVisibility(View.VISIBLE);
            binding.titleTv.setVisibility(View.VISIBLE);
            binding.loadingPb.setVisibility(View.GONE);
        }

        viewModel = new ViewModelProvider(this, new AuthViewModelFactory(this))
                .get(AuthViewModel.class);

        binding.loginBtn.setOnClickListener(v -> viewModel.login(
                binding.emailEt.getText().toString().trim(),
                binding.passwordEt.getText().toString()));

        binding.goToSignupBtn.setOnClickListener(v ->
                startActivity(new Intent(this, SignupActivity.class)));

        viewModel.getUiState().observe(this, state -> {
            binding.loadingPb.setVisibility(
                    state.state == AuthViewModel.State.LOADING ? View.VISIBLE : View.GONE);
            binding.loginBtn.setEnabled(state.state != AuthViewModel.State.LOADING);

            if (state.state == AuthViewModel.State.SUCCESS) {
                goHome();
            } else if (state.state == AuthViewModel.State.ERROR) {
                Toast.makeText(this, state.errorMessage, Toast.LENGTH_LONG).show();
                viewModel.resetState();
            }
        });
    }

    private void goHome() {
        Intent home = new Intent(this, StockListActivity.class);
        home.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(home);
        finish();
    }
}