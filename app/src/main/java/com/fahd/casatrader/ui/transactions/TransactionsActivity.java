package com.fahd.casatrader.ui.transactions;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.fahd.casatrader.databinding.ActivityTransactionsBinding;
import com.fahd.casatrader.ui.detail.StockDetailActivity;

public class TransactionsActivity extends AppCompatActivity {

    public static Intent newIntent(Context ctx) { return new Intent(ctx, TransactionsActivity.class); }

    private ActivityTransactionsBinding binding;
    private TransactionsViewModel viewModel;
    private TransactionsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTransactionsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        viewModel = new ViewModelProvider(this, new TransactionsViewModelFactory(this))
                .get(TransactionsViewModel.class);

        adapter = new TransactionsAdapter(t ->
                startActivity(StockDetailActivity.newIntent(this, t.ticker)));
        binding.transactionsRv.setLayoutManager(new LinearLayoutManager(this));
        binding.transactionsRv.setAdapter(adapter);

        binding.swipeRefresh.setOnRefreshListener(viewModel::load);

        viewModel.getUiState().observe(this, state -> {
            switch (state.loadState) {
                case LOADING:
                    if (adapter.getItemCount() == 0) binding.loadingPb.setVisibility(View.VISIBLE);
                    break;
                case SUCCESS:
                    binding.loadingPb.setVisibility(View.GONE);
                    binding.swipeRefresh.setRefreshing(false);
                    adapter.submitList(state.transactions);
                    binding.emptyTv.setVisibility(state.transactions.isEmpty() ? View.VISIBLE : View.GONE);
                    binding.transactionsRv.setVisibility(state.transactions.isEmpty() ? View.GONE : View.VISIBLE);
                    break;
                case ERROR:
                    binding.loadingPb.setVisibility(View.GONE);
                    binding.swipeRefresh.setRefreshing(false);
                    Toast.makeText(this, state.errorMessage, Toast.LENGTH_LONG).show();
                    break;
                case IDLE:
                default: break;
            }
        });

        if (savedInstanceState == null) viewModel.load();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh in case the user came back from making a trade
        if (viewModel.getUiState().getValue() != null
                && viewModel.getUiState().getValue().loadState != TransactionsViewModel.LoadState.LOADING) {
            viewModel.load();
        }
    }
}