package com.fahd.casatrader.ui.detail;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.webkit.WebView;
import android.webkit.WebViewClient;
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

    private boolean chartReady = false;
    private List<PriceSnapshot> pendingHistory;

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
            binding.chartWebView.setVisibility(View.VISIBLE);
            updateChartData(state.history);
        } else if (state.loadState == StockDetailViewModel.LoadState.SUCCESS || state.loadState == StockDetailViewModel.LoadState.ERROR) {
            binding.chartEmptyState.setVisibility(View.VISIBLE);
            binding.chartWebView.setVisibility(View.GONE);
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

    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
    private void setupChart() {
        WebView wv = binding.chartWebView;
        wv.getSettings().setJavaScriptEnabled(true);
        wv.setBackgroundColor(Color.TRANSPARENT);
        // Horizontal drag / pinch -> chart pan & zoom; vertical drag -> let the page scroll.
        final int touchSlop = ViewConfiguration.get(this).getScaledTouchSlop();
        wv.setOnTouchListener(new View.OnTouchListener() {
            float downX, downY;
            boolean decided, claim;
            @Override public boolean onTouch(View v, MotionEvent e) {
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        downX = e.getX(); downY = e.getY();
                        decided = false; claim = false;
                        v.getParent().requestDisallowInterceptTouchEvent(true);
                        break;
                    case MotionEvent.ACTION_POINTER_DOWN: // second finger -> pinch zoom
                        decided = true; claim = true;
                        v.getParent().requestDisallowInterceptTouchEvent(true);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (!decided && e.getPointerCount() == 1) {
                            float dx = Math.abs(e.getX() - downX);
                            float dy = Math.abs(e.getY() - downY);
                            if (dx > touchSlop || dy > touchSlop) {
                                claim = dx >= dy; // horizontal pan stays with the chart
                                decided = true;
                            }
                        }
                        v.getParent().requestDisallowInterceptTouchEvent(claim || e.getPointerCount() > 1);
                        break;
                }
                return false;
            }
        });
        wv.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String url) {
                chartReady = true;
                if (pendingHistory != null) {
                    injectChartData(pendingHistory);
                    pendingHistory = null;
                }
            }
        });
        wv.loadUrl("file:///android_asset/chart.html");
    }

    private void updateChartData(List<PriceSnapshot> history) {
        if (chartReady) {
            injectChartData(history);
        } else {
            pendingHistory = history;
        }
    }

    private void injectChartData(List<PriceSnapshot> history) {
        StringBuilder json = new StringBuilder("[");
        String lastDate = null;
        boolean first = true;
        for (PriceSnapshot s : history) {
            if (s.tradeDate == null || s.price == null) continue;
            // Lightweight Charts requires strictly ascending, unique time values.
            if (s.tradeDate.equals(lastDate)) continue;
            double close = s.price;
            double open = s.open != null ? s.open : close;
            double high = s.high != null ? s.high : Math.max(open, close);
            double low = s.low != null ? s.low : Math.min(open, close);
            if (!first) json.append(',');
            json.append("{\"time\":\"").append(s.tradeDate)
                    .append("\",\"open\":").append(open)
                    .append(",\"high\":").append(high)
                    .append(",\"low\":").append(low)
                    .append(",\"close\":").append(close).append('}');
            lastDate = s.tradeDate;
            first = false;
        }
        json.append(']');

        String upColor = hex(R.color.gain_green);
        String downColor = hex(R.color.loss_red);
        String textColor = hex(R.color.neutral_gray);
        String js = "loadChart('" + json + "','" + upColor + "','" + downColor + "','" + textColor + "')";
        binding.chartWebView.evaluateJavascript(js, null);
    }

    private String hex(int colorRes) {
        return String.format("#%06X", 0xFFFFFF & ContextCompat.getColor(this, colorRes));
    }

    private String fmt(Double v) {
        return v != null ? String.format(Locale.US, "%,.2f", v) : "—";
    }

    @Override
    public void onTradeCompleted(TradeResult result) {
        viewModel.load();
    }
}