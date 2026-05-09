package com.fahd.casatrader.ui.watchlist;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.fahd.casatrader.databinding.ActivityWatchlistBinding;
import com.fahd.casatrader.ui.detail.StockDetailActivity;

public class WatchlistActivity extends AppCompatActivity {

    public static Intent newIntent(Context ctx) { return new Intent(ctx, WatchlistActivity.class); }

    private ActivityWatchlistBinding binding;
    private WatchlistViewModel viewModel;
    private WatchlistAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWatchlistBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        viewModel = new ViewModelProvider(this, new WatchlistViewModelFactory(this))
                .get(WatchlistViewModel.class);

        adapter = new WatchlistAdapter(new WatchlistAdapter.OnEntryActionListener() {
            @Override
            public void onEntryClick(com.fahd.casatrader.data.model.WatchlistEntry entry) {
                startActivity(StockDetailActivity.newIntent(WatchlistActivity.this, entry.ticker));
            }
            @Override
            public void onRemoveClick(com.fahd.casatrader.data.model.WatchlistEntry entry) {
                viewModel.remove(entry.ticker);
            }
        });
        binding.watchlistRv.setLayoutManager(new LinearLayoutManager(this));
        binding.watchlistRv.setAdapter(adapter);

        binding.swipeRefresh.setOnRefreshListener(viewModel::load);

        viewModel.getUiState().observe(this, state -> {
            switch (state.loadState) {
                case LOADING:
                    if (adapter.getItemCount() == 0) binding.loadingPb.setVisibility(View.VISIBLE);
                    break;
                case SUCCESS:
                    binding.loadingPb.setVisibility(View.GONE);
                    binding.swipeRefresh.setRefreshing(false);
                    adapter.submitList(state.entries);
                    binding.emptyTv.setVisibility(state.entries.isEmpty() ? View.VISIBLE : View.GONE);
                    binding.watchlistRv.setVisibility(state.entries.isEmpty() ? View.GONE : View.VISIBLE);
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
        if (viewModel.getUiState().getValue() != null
                && viewModel.getUiState().getValue().loadState != WatchlistViewModel.LoadState.LOADING) {
            viewModel.load();
        }
    }
}