package com.fahd.casatrader.ui.stocks;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.fahd.casatrader.databinding.ActivityStockListBinding;

public class StockListActivity extends AppCompatActivity {

    private ActivityStockListBinding binding;
    private StockListViewModel viewModel;
    private StockListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStockListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        viewModel = new ViewModelProvider(this, new StockListViewModelFactory(this))
                .get(StockListViewModel.class);

        adapter = new StockListAdapter(stock -> {
            // TODO step 6: open StockDetailActivity
            Toast.makeText(this, "Tapped " + stock.ticker, Toast.LENGTH_SHORT).show();
        });
        binding.stocksRv.setLayoutManager(new LinearLayoutManager(this));
        binding.stocksRv.setAdapter(adapter);

        binding.swipeRefresh.setOnRefreshListener(() -> viewModel.load());

        binding.searchEt.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                viewModel.setSearchQuery(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        viewModel.getUiState().observe(this, state -> {
            switch (state.loadState) {
                case LOADING:
                    if (adapter.getItemCount() == 0) {
                        binding.loadingPb.setVisibility(View.VISIBLE);
                    }
                    break;
                case SUCCESS:
                    binding.loadingPb.setVisibility(View.GONE);
                    binding.swipeRefresh.setRefreshing(false);
                    adapter.submitList(state.stocks);
                    binding.emptyTv.setVisibility(state.stocks.isEmpty() ? View.VISIBLE : View.GONE);
                    break;
                case ERROR:
                    binding.loadingPb.setVisibility(View.GONE);
                    binding.swipeRefresh.setRefreshing(false);
                    Toast.makeText(this, state.errorMessage, Toast.LENGTH_LONG).show();
                    break;
                case IDLE:
                default:
                    break;
            }
        });

        // First load on cold start. Configuration changes (rotation) keep the ViewModel and
        // its cached list, so no refetch needed there.
        if (savedInstanceState == null) {
            viewModel.load();
        }
    }
}