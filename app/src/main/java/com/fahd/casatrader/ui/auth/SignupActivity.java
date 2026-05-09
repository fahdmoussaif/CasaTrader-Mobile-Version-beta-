package com.fahd.casatrader.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.fahd.casatrader.MainActivity;
import com.fahd.casatrader.databinding.ActivitySignupBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class SignupActivity extends AppCompatActivity {

    private ActivitySignupBinding binding;
    private AuthViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this, new AuthViewModelFactory(this))
                .get(AuthViewModel.class);

        binding.signupBtn.setOnClickListener(v -> viewModel.signup(
                binding.emailEt.getText().toString().trim(),
                binding.passwordEt.getText().toString(),
                binding.usernameEt.getText().toString().trim()));

        binding.goTologinBtn.setOnClickListener(v -> finish());

        viewModel.getUiState().observe(this, state -> {
            binding.loadingPb.setVisibility(
                    state.state == AuthViewModel.State.LOADING ? View.VISIBLE : View.GONE);
            binding.signupBtn.setEnabled(state.state != AuthViewModel.State.LOADING);

            if (state.state == AuthViewModel.State.SUCCESS) {
                Intent home = new Intent(this, MainActivity.class);
                home.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(home);
                finish();
            } else if (state.state == AuthViewModel.State.SUCCESS_CONFIRMATION_REQUIRED) {
                showConfirmationMessage();
            } else if (state.state == AuthViewModel.State.ERROR) {
                Toast.makeText(this, state.errorMessage, Toast.LENGTH_LONG).show();
                viewModel.resetState();
            }
        });
    }

    private void showConfirmationMessage() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Confirm your email")
                .setMessage("We've sent a confirmation link to your email address. Please verify your account before logging in.")
                .setPositiveButton("OK", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }
}