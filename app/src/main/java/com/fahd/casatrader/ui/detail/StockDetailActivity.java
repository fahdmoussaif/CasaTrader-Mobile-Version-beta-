package com.fahd.casatrader.ui.detail;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.fahd.casatrader.R;
import com.fahd.casatrader.data.model.PriceSnapshot;
import com.fahd.casatrader.data.model.Stock;
import com.fahd.casatrader.data.remote.ApiClient;
import com.fahd.casatrader.data.repo.WatchlistRepository;
import com.fahd.casatrader.databinding.ActivityStockDetailBinding;
import com.fahd.casatrader.util.TokenStore;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.fahd.casatrader.data.model.WriteDtos.TradeResult;


import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StockDetailActivity extends AppCompatActivity
        implements TradeDialog.OnTradeCompletedListener {

    public static final String EXTRA_TICKER = "extra_ticker";

    public static Intent newIntent(Context ctx, String ticker) {
        return new Intent(ctx, StockDetailActivity.class).putExtra(EXTRA_TICKER, ticker);
    }

    private ActivityStockDetailBinding binding;
    private StockDetailViewModel viewModel;
    private boolean isWatched = false;
    private boolean watchlistBusy = false;
    private WatchlistRepository watchlistRepo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStockDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String ticker = getIntent().getStringExtra(EXTRA_TICKER);
        if (ticker == null) { finish(); return; }

        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setTitle(ticker);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        viewModel = new ViewModelProvider(this, new StockDetailViewModelFactory(this, ticker))
                .get(StockDetailViewModel.class);
        TokenStore ts = TokenStore.getInstance(this);
        watchlistRepo = new WatchlistRepository(ApiClient.getInstance(ts), ts);
        binding.watchlistBtn.setOnClickListener(v -> toggleWatchlist(ticker));
        refreshWatchlistState(ticker);

        configureChart();

        binding.buyBtn.setOnClickListener(v -> openTradeDialog(TradeDialog.Mode.BUY));
        binding.sellBtn.setOnClickListener(v -> openTradeDialog(TradeDialog.Mode.SELL));
        binding.watchlistBtn.setOnClickListener(v -> toggleWatchlist(ticker));
        refreshWatchlistState(ticker);

        // ... rest of onCreate unchanged ...
        viewModel.getStockState().observe(this, this::renderStock);
        viewModel.getHistoryState().observe(this, this::renderHistory);

        if (savedInstanceState == null) viewModel.load();
    }

    private void configureChart() {
        binding.priceChart.setNoDataText("");
        binding.priceChart.getDescription().setEnabled(false);
        binding.priceChart.getLegend().setEnabled(false);
        binding.priceChart.setScaleYEnabled(false);
        binding.priceChart.setDoubleTapToZoomEnabled(false);
        binding.priceChart.setExtraOffsets(8f, 8f, 8f, 8f);

        int onSurface = com.google.android.material.color.MaterialColors.getColor(
                binding.priceChart, com.google.android.material.R.attr.colorOnSurface);
        int outline = com.google.android.material.color.MaterialColors.getColor(
                binding.priceChart, com.google.android.material.R.attr.colorOutlineVariant);

        XAxis x = binding.priceChart.getXAxis();
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        x.setDrawGridLines(false);
        x.setDrawAxisLine(false);
        x.setLabelCount(4, true);
        x.setTextColor(onSurface);
        x.setTextSize(10f);

        binding.priceChart.getAxisRight().setEnabled(false);
        com.github.mikephil.charting.components.YAxis y = binding.priceChart.getAxisLeft();
        y.setDrawAxisLine(false);
        y.setGridColor(outline);
        y.setGridLineWidth(0.5f);
        y.setTextColor(onSurface);
        y.setTextSize(10f);
    }

    private void renderStock(StockDetailViewModel.StockState state) {
        switch (state.loadState) {
            case SUCCESS:
                Stock s = state.stock;
                binding.tickerTv.setText(s.ticker);
                binding.nameTv.setText(s.name != null ? s.name : "");
                binding.priceTv.setText(s.price != null
                        ? String.format(Locale.US, "%,.2f MAD", s.price) : "—");

                if (s.changePercent != null) {
                    double cp = s.changePercent;
                    String sign = cp > 0 ? "+" : "";
                    binding.changeTv.setText(String.format(Locale.US, "%s%.2f%%", sign, cp));
                    int colorRes = cp > 0 ? R.color.gain_green
                            : cp < 0 ? R.color.loss_red
                            : R.color.neutral_gray;
                    binding.changeTv.setTextColor(ContextCompat.getColor(this, colorRes));
                } else {
                    binding.changeTv.setText("—");
                }

                binding.openValueTv.setText(s.open != null ? fmt(s.open) : "—");
                binding.highValueTv.setText(s.high != null ? fmt(s.high) : "—");
                binding.lowValueTv.setText(s.low != null ? fmt(s.low) : "—");
                binding.prevCloseValueTv.setText(s.previousClose != null ? fmt(s.previousClose) : "—");
                binding.volumeValueTv.setText(s.volume != null ? fmtCompact(s.volume) : "—");
                binding.tradesValueTv.setText(s.tradesCount != null ? s.tradesCount.toString() : "—");
                binding.sectorValueTv.setText(s.sector != null ? s.sector : "—");
                binding.marketCapValueTv.setText(s.marketCap != null ? fmtCompact(s.marketCap) : "—");
                break;
            case ERROR:
                Toast.makeText(this, state.errorMessage, Toast.LENGTH_LONG).show();
                break;
            case LOADING:
            default:
                break;
        }
    }


    private void renderHistory(StockDetailViewModel.HistoryState state) {
        switch (state.loadState) {
            case LOADING:
                binding.chartLoadingPb.setVisibility(View.VISIBLE);
                binding.chartEmptyTv.setVisibility(View.GONE);
                break;
            case SUCCESS:
                binding.chartLoadingPb.setVisibility(View.GONE);
                if (state.history.isEmpty()) {
                    binding.priceChart.setVisibility(View.INVISIBLE);
                    binding.chartEmptyTv.setVisibility(View.VISIBLE);
                } else {
                    binding.priceChart.setVisibility(View.VISIBLE);
                    binding.chartEmptyTv.setVisibility(View.GONE);
                    drawChart(state.history);
                }
                break;
            case ERROR:
                binding.chartLoadingPb.setVisibility(View.GONE);
                binding.priceChart.setVisibility(View.INVISIBLE);
                binding.chartEmptyTv.setText("Couldn't load chart");
                binding.chartEmptyTv.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void drawChart(List<PriceSnapshot> snaps) {
        List<Entry> entries = new ArrayList<>(snaps.size());
        final List<String> dates = new ArrayList<>(snaps.size());
        for (int i = 0; i < snaps.size(); i++) {
            PriceSnapshot snap = snaps.get(i);
            if (snap.price == null) continue;
            entries.add(new Entry(i, snap.price.floatValue()));
            dates.add(snap.tradeDate != null ? snap.tradeDate : "");
        }
        if (entries.isEmpty()) {
            binding.priceChart.setVisibility(View.INVISIBLE);
            binding.chartEmptyTv.setVisibility(View.VISIBLE);
            return;
        }

        // Color the line based on first vs. last price (overall trend)
        float firstPrice = entries.get(0).getY();
        float lastPrice = entries.get(entries.size() - 1).getY();
        int lineColor = ContextCompat.getColor(this,
                lastPrice > firstPrice ? R.color.gain_green
                        : lastPrice < firstPrice ? R.color.loss_red
                        : R.color.neutral_gray);

        LineDataSet set = new LineDataSet(entries, "Price");
        set.setColor(lineColor);
        set.setLineWidth(2f);
        set.setDrawCircles(false);
        set.setDrawValues(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setCubicIntensity(0.1f);
        set.setDrawFilled(true);
        set.setFillColor(lineColor);
        set.setFillAlpha(40);
        set.setHighLightColor(Color.GRAY);

        binding.priceChart.setData(new LineData(set));

        // Format the X axis as the actual trade date (truncated to "MM-DD")
        binding.priceChart.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, com.github.mikephil.charting.components.AxisBase axis) {
                int idx = (int) value;
                if (idx < 0 || idx >= dates.size()) return "";
                String date = dates.get(idx);
                return date.length() >= 10 ? date.substring(5) : date;     // "MM-DD"
            }
        });

        binding.priceChart.invalidate();
        binding.priceChart.animateX(400);
    }

    private static String fmt(double value) {
        return String.format(Locale.US, "%,.2f", value);
    }

    private static String fmtCompact(double value) {
        if (value >= 1_000_000_000) return String.format(Locale.US, "%.2fB", value / 1_000_000_000);
        if (value >= 1_000_000)     return String.format(Locale.US, "%.2fM", value / 1_000_000);
        if (value >= 1_000)         return String.format(Locale.US, "%.2fK", value / 1_000);
        return String.format(Locale.US, "%,.0f", value);
    }
    @Override
    public void onTradeCompleted(TradeResult result) {
        viewModel.loadStock();
    }

    private void openTradeDialog(TradeDialog.Mode mode) {
        StockDetailViewModel.StockState state = viewModel.getStockState().getValue();
        if (state == null || state.stock == null || state.stock.price == null) {
            Toast.makeText(this, "Price unavailable, try again in a moment", Toast.LENGTH_SHORT).show();
            return;
        }
        TradeDialog.newInstance(mode, state.stock.ticker, state.stock.price)
                .show(getSupportFragmentManager(), "trade");
    }

    private void refreshWatchlistState(String ticker) {
        watchlistRepo.isWatched(ticker, new WatchlistRepository.Callback<Boolean>() {
            @Override public void onSuccess(Boolean watched) {
                isWatched = watched;
                updateWatchlistButton();
            }
            @Override public void onError(String message) { /* leave as-is */ }
        });
    }

    private void updateWatchlistButton() {
        if (isWatched) {
            binding.watchlistBtn.setIconResource(R.drawable.ic_star_filled);
            binding.watchlistBtn.setText("On your watchlist");
        } else {
            binding.watchlistBtn.setIconResource(R.drawable.ic_star_outline);
            binding.watchlistBtn.setText("Add to watchlist");
        }
    }

    private void toggleWatchlist(String ticker) {
        if (watchlistBusy) return;
        watchlistBusy = true;
        boolean wasWatched = isWatched;

        // Optimistic flip
        isWatched = !wasWatched;
        updateWatchlistButton();

        WatchlistRepository.Callback<Void> cb = new WatchlistRepository.Callback<Void>() {
            @Override public void onSuccess(Void data) { watchlistBusy = false; }
            @Override public void onError(String message) {
                // Roll back
                isWatched = wasWatched;
                updateWatchlistButton();
                Toast.makeText(StockDetailActivity.this, message, Toast.LENGTH_LONG).show();
                watchlistBusy = false;
            }
        };

        if (wasWatched) watchlistRepo.remove(ticker, cb);
        else            watchlistRepo.add(ticker, cb);
    }


}