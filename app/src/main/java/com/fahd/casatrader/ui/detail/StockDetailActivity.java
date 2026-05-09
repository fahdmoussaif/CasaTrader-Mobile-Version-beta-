package com.fahd.casatrader.ui.detail;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.fahd.casatrader.R;
import com.fahd.casatrader.data.model.PriceSnapshot;
import com.fahd.casatrader.data.model.Stock;
import com.fahd.casatrader.data.model.WriteDtos.TradeResult;
import com.fahd.casatrader.data.remote.ApiClient;
import com.fahd.casatrader.data.repo.WatchlistRepository;
import com.fahd.casatrader.databinding.ActivityStockDetailBinding;
import com.fahd.casatrader.util.TokenStore;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StockDetailActivity extends AppCompatActivity implements TradeDialog.OnTradeCompletedListener {

    private static final String EXTRA_TICKER = "extra_ticker";

    public static Intent newIntent(Context ctx, String ticker) {
        return new Intent(ctx, StockDetailActivity.class).putExtra(EXTRA_TICKER, ticker);
    }

    private ActivityStockDetailBinding binding;
    private StockDetailViewModel viewModel;
    private WatchlistRepository watchlistRepo;
    private String ticker;
    private Stock currentStock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStockDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        ticker = getIntent().getStringExtra(EXTRA_TICKER);
        if (ticker == null) { finish(); return; }

        TokenStore ts = TokenStore.getInstance(this);
        watchlistRepo = new WatchlistRepository(ApiClient.getInstance(ts), ts);

        viewModel = new ViewModelProvider(this, new StockDetailViewModelFactory(this, ticker))
                .get(StockDetailViewModel.class);

        setupChart();

        binding.buyBtn.setOnClickListener(v -> {
            if (currentStock != null) {
                TradeDialog.newInstance(TradeDialog.Mode.BUY, ticker, currentStock.price)
                        .show(getSupportFragmentManager(), "trade");
            }
        });

        binding.sellBtn.setOnClickListener(v -> {
            if (currentStock != null) {
                TradeDialog.newInstance(TradeDialog.Mode.SELL, ticker, currentStock.price)
                        .show(getSupportFragmentManager(), "trade");
            }
        });

        binding.watchlistBtn.setOnClickListener(v -> toggleWatchlist());

        viewModel.getStockState().observe(this, this::renderStock);
        viewModel.getHistoryState().observe(this, this::renderHistory);

        checkWatchlistStatus();
        viewModel.load();
    }

    private void renderStock(StockDetailViewModel.StockState state) {
        if (state.loadState == StockDetailViewModel.LoadState.SUCCESS && state.stock != null) {
            currentStock = state.stock;
            Stock s = state.stock;
            binding.tickerTv.setText(s.ticker);
            binding.nameTv.setText(s.name);
            binding.priceTv.setText(String.format(Locale.US, "%,.2f", s.price));

            double changePct = s.changePercent != null ? s.changePercent : 0;
            String sign = changePct >= 0 ? "+" : "";
            binding.changeTv.setText(String.format(Locale.US, "%s%.2f%%", sign, changePct));
            binding.changeTv.setTextColor(ContextCompat.getColor(this,
                    changePct >= 0 ? R.color.gain_green : R.color.loss_red));

            binding.openValueTv.setText(fmt(s.open));
            binding.highValueTv.setText(fmt(s.high));
            binding.lowValueTv.setText(fmt(s.low));
            binding.prevCloseValueTv.setText(fmt(s.previousClose));
            binding.volumeValueTv.setText(String.format(Locale.US, "%,.0f", s.volume));
            binding.tradesValueTv.setText(String.format(Locale.US, "%,d", s.tradesCount));
            binding.sectorValueTv.setText(s.sector);
            binding.marketCapValueTv.setText(String.format(Locale.US, "%,.0f M", s.marketCap / 1_000_000.0));
        } else if (state.loadState == StockDetailViewModel.LoadState.ERROR) {
            Toast.makeText(this, state.errorMessage, Toast.LENGTH_LONG).show();
        }
    }

    private void renderHistory(StockDetailViewModel.HistoryState state) {
        binding.chartLoadingPb.setVisibility(state.loadState == StockDetailViewModel.LoadState.LOADING ? View.VISIBLE : View.GONE);
        if (state.loadState == StockDetailViewModel.LoadState.SUCCESS && state.history != null && !state.history.isEmpty()) {
            binding.chartEmptyState.setVisibility(View.GONE);
            binding.priceChart.setVisibility(View.VISIBLE);
            updateChartData(state.history);
        } else if (state.loadState == StockDetailViewModel.LoadState.SUCCESS || state.loadState == StockDetailViewModel.LoadState.ERROR) {
            binding.chartEmptyState.setVisibility(View.VISIBLE);
            binding.priceChart.setVisibility(View.GONE);
        }
    }

    private void checkWatchlistStatus() {
        watchlistRepo.isWatched(ticker, new WatchlistRepository.Callback<Boolean>() {
            @Override public void onSuccess(Boolean watched) { updateWatchlistButton(watched); }
            @Override public void onError(String message) {}
        });
    }

    private void toggleWatchlist() {
        watchlistRepo.isWatched(ticker, new WatchlistRepository.Callback<Boolean>() {
            @Override public void onSuccess(Boolean watched) {
                if (watched) {
                    watchlistRepo.remove(ticker, new WatchlistRepository.Callback<Void>() {
                        @Override public void onSuccess(Void d) { updateWatchlistButton(false); }
                        @Override public void onError(String m) { Toast.makeText(StockDetailActivity.this, m, Toast.LENGTH_SHORT).show(); }
                    });
                } else {
                    watchlistRepo.add(ticker, new WatchlistRepository.Callback<Void>() {
                        @Override public void onSuccess(Void d) { updateWatchlistButton(true); }
                        @Override public void onError(String m) { Toast.makeText(StockDetailActivity.this, m, Toast.LENGTH_SHORT).show(); }
                    });
                }
            }
            @Override public void onError(String message) {}
        });
    }

    private void updateWatchlistButton(boolean watched) {
        binding.watchlistBtn.setText(watched ? "Remove from watchlist" : "Add to watchlist");
        binding.watchlistBtn.setIconResource(watched ? R.drawable.ic_star_filled : R.drawable.ic_star_outline);
    }

    private void setupChart() {
        binding.priceChart.getDescription().setEnabled(false);
        binding.priceChart.getLegend().setEnabled(false);
        binding.priceChart.setTouchEnabled(true);
        binding.priceChart.setDragEnabled(true);
        binding.priceChart.setScaleEnabled(true);
        binding.priceChart.setPinchZoom(true);
        binding.priceChart.setDrawGridBackground(false);
        binding.priceChart.getXAxis().setEnabled(false);
        binding.priceChart.getAxisRight().setEnabled(false);
        binding.priceChart.getAxisLeft().setDrawGridLines(false);
        binding.priceChart.getAxisLeft().setDrawAxisLine(false);
        binding.priceChart.getAxisLeft().setTextColor(ContextCompat.getColor(this, R.color.neutral_gray));
    }

    private void updateChartData(List<PriceSnapshot> history) {
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < history.size(); i++) {
            entries.add(new Entry(i, history.get(i).price.floatValue()));
        }
        LineDataSet set = new LineDataSet(entries, "Price");
        set.setColor(ContextCompat.getColor(this, R.color.md_theme_primary));
        set.setLineWidth(2f);
        set.setDrawCircles(false);
        set.setDrawValues(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setDrawFilled(true);
        set.setFillColor(ContextCompat.getColor(this, R.color.md_theme_primary));
        set.setFillAlpha(30);
        binding.priceChart.setData(new LineData(set));
        binding.priceChart.invalidate();
    }

    private String fmt(Double v) {
        return v != null ? String.format(Locale.US, "%,.2f", v) : "—";
    }

    @Override
    public void onTradeCompleted(TradeResult result) {
        viewModel.load();
    }
}