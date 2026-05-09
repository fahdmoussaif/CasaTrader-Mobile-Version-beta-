package com.fahd.casatrader.ui.portfolio;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.fahd.casatrader.R;
import com.fahd.casatrader.data.model.HoldingWithStock;
import com.fahd.casatrader.databinding.ActivityPortfolioBinding;
import com.fahd.casatrader.ui.detail.StockDetailActivity;

import java.util.List;
import java.util.Locale;

public class PortfolioActivity extends AppCompatActivity {

    private static final double STARTING_CASH = 100_000.00;

    public static Intent newIntent(Context ctx) { return new Intent(ctx, PortfolioActivity.class); }

    private ActivityPortfolioBinding binding;
    private PortfolioViewModel viewModel;
    private HoldingsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPortfolioBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        viewModel = new ViewModelProvider(this, new PortfolioViewModelFactory(this))
                .get(PortfolioViewModel.class);

        adapter = new HoldingsAdapter(holding ->
                startActivity(StockDetailActivity.newIntent(this, holding.ticker)));
        binding.holdingsRv.setLayoutManager(new LinearLayoutManager(this));
        binding.holdingsRv.setAdapter(adapter);

        binding.swipeRefresh.setOnRefreshListener(viewModel::load);

        viewModel.getUiState().observe(this, state -> {
            switch (state.loadState) {
                case LOADING:
                    if (adapter.getItemCount() == 0) binding.loadingPb.setVisibility(View.VISIBLE);
                    break;
                case SUCCESS:
                    binding.loadingPb.setVisibility(View.GONE);
                    binding.swipeRefresh.setRefreshing(false);
                    renderSummary(state.profile, state.holdings);
                    adapter.submitList(state.holdings);
                    binding.emptyTv.setVisibility(state.holdings.isEmpty() ? View.VISIBLE : View.GONE);
                    binding.holdingsRv.setVisibility(state.holdings.isEmpty() ? View.GONE : View.VISIBLE);
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
        // Refresh whenever we return from StockDetailActivity (after a possible trade)
        if (viewModel.getUiState().getValue() != null
                && viewModel.getUiState().getValue().loadState != PortfolioViewModel.LoadState.LOADING) {
            viewModel.load();
        }
    }

    private void renderSummary(com.fahd.casatrader.data.model.Profile profile,
                               List<HoldingWithStock> holdings) {
        double cash = profile != null && profile.cashBalance != null ? profile.cashBalance : 0;

        double holdingsValue = 0;
        double costBasis = 0;
        for (HoldingWithStock h : holdings) {
            holdingsValue += h.currentValue();
            costBasis += h.costBasis();
        }
        double totalValue = cash + holdingsValue;
        double totalPl = totalValue - STARTING_CASH;
        double totalPlPct = (totalPl / STARTING_CASH) * 100.0;

        binding.totalValueTv.setText(String.format(Locale.US, "%,.2f MAD", totalValue));

        String sign = totalPl > 0 ? "+" : "";
        binding.totalPlTv.setText(String.format(Locale.US,
                "%s%,.2f MAD (%s%.2f%%)", sign, totalPl, sign, totalPlPct));
        int colorRes = totalPl > 0 ? R.color.gain_green
                : totalPl < 0 ? R.color.loss_red
                : R.color.neutral_gray;
        binding.totalPlTv.setTextColor(ContextCompat.getColor(this, colorRes));

        binding.cashTv.setText(String.format(Locale.US, "%,.2f", cash));
        binding.holdingsValueTv.setText(String.format(Locale.US, "%,.2f", holdingsValue));
    }
}