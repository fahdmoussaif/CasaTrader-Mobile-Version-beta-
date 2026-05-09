package com.fahd.casatrader.ui.detail;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fahd.casatrader.R;
import com.fahd.casatrader.data.model.Holding;
import com.fahd.casatrader.data.model.Profile;
import com.fahd.casatrader.data.model.WriteDtos.TradeResult;
import com.fahd.casatrader.data.remote.ApiClient;
import com.fahd.casatrader.data.repo.TradeRepository;
import com.fahd.casatrader.databinding.DialogTradeBinding;
import com.fahd.casatrader.util.TokenStore;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.Locale;

public class TradeDialog extends BottomSheetDialogFragment {

    public enum Mode { BUY, SELL }

    public interface OnTradeCompletedListener {
        void onTradeCompleted(TradeResult result);
    }

    private static final String ARG_MODE = "arg_mode";
    private static final String ARG_TICKER = "arg_ticker";
    private static final String ARG_PRICE = "arg_price";
    private static final double FEE_RATE = 0.01;

    public static TradeDialog newInstance(Mode mode, String ticker, double price) {
        TradeDialog d = new TradeDialog();
        Bundle b = new Bundle();
        b.putString(ARG_MODE, mode.name());
        b.putString(ARG_TICKER, ticker);
        b.putDouble(ARG_PRICE, price);
        d.setArguments(b);
        return d;
    }

    private DialogTradeBinding binding;
    private TradeRepository repo;
    private OnTradeCompletedListener listener;

    private Mode mode;
    private String ticker;
    private double price;
    private double availableCash = -1;       // for BUY
    private int sharesOwned = -1;            // for SELL

    @Override
    public void onAttach(@NonNull android.content.Context context) {
        super.onAttach(context);
        if (context instanceof OnTradeCompletedListener) {
            listener = (OnTradeCompletedListener) context;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = DialogTradeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = requireArguments();
        mode = Mode.valueOf(args.getString(ARG_MODE));
        ticker = args.getString(ARG_TICKER);
        price = args.getDouble(ARG_PRICE);

        TokenStore ts = TokenStore.getInstance(requireContext());
        repo = new TradeRepository(ApiClient.getInstance(ts), ts);

        // Header
        binding.headerTv.setText(
                (mode == Mode.BUY ? "Buy " : "Sell ") + ticker);
        binding.subheaderTv.setText(
                "Current price " + fmt(price) + " MAD");

        binding.totalLabelTv.setText(
                mode == Mode.BUY ? "Total cost" : "You receive");

        binding.confirmBtn.setText(
                mode == Mode.BUY ? "Confirm purchase" : "Confirm sale");

        binding.sharesEt.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                onSharesChanged();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        binding.confirmBtn.setOnClickListener(v -> submit());

        // Initial preview (0 shares)
        updatePreview(0);

        // Load context (cash balance for buy, holding for sell)
        loadContext();
    }

    private void loadContext() {
        if (mode == Mode.BUY) {
            repo.getProfile(new TradeRepository.Callback<Profile>() {
                @Override public void onSuccess(Profile p) {
                    availableCash = p.cashBalance != null ? p.cashBalance : 0;
                    if (binding != null) {
                        binding.availableTv.setText(
                                "Available: " + fmt(availableCash) + " MAD");
                        revalidate();
                    }
                }
                @Override public void onError(String message) {
                    if (binding != null) binding.availableTv.setText("Couldn't load balance");
                }
            });
        } else {
            repo.getHolding(ticker, new TradeRepository.Callback<Holding>() {
                @Override public void onSuccess(Holding h) {
                    sharesOwned = (h != null && h.shares != null) ? h.shares : 0;
                    if (binding != null) {
                        binding.availableTv.setText("You own: " + sharesOwned + " shares");
                        revalidate();
                    }
                }
                @Override public void onError(String message) {
                    if (binding != null) binding.availableTv.setText("Couldn't load holdings");
                }
            });
        }
    }

    private void onSharesChanged() {
        binding.dialogErrorTv.setVisibility(View.GONE);
        Integer parsed = parseShares();
        updatePreview(parsed != null ? parsed : 0);
        revalidate();
    }

    private void updatePreview(int shares) {
        double gross = shares * price;
        double fee = gross * FEE_RATE;
        double total = mode == Mode.BUY ? (gross + fee) : (gross - fee);
        binding.grossTv.setText(fmt(gross) + " MAD");
        binding.feeTv.setText(fmt(fee) + " MAD");
        binding.totalTv.setText(fmt(total) + " MAD");
    }

    private void revalidate() {
        Integer shares = parseShares();
        boolean canSubmit = false;
        String error = null;

        if (shares == null || shares <= 0) {
            canSubmit = false;
        } else if (mode == Mode.BUY) {
            double total = shares * price * (1 + FEE_RATE);
            if (availableCash >= 0 && total > availableCash) {
                canSubmit = false;
                error = "You can afford up to " + (int) Math.floor(availableCash / (price * (1 + FEE_RATE))) + " shares.";
            } else {
                canSubmit = true;
            }
        } else { // SELL
            if (sharesOwned >= 0 && shares > sharesOwned) {
                canSubmit = false;
                error = "You only own " + sharesOwned + " shares.";
            } else if (sharesOwned > 0) {
                canSubmit = true;
            }
        }

        binding.confirmBtn.setEnabled(canSubmit);
        if (error != null) {
            binding.dialogErrorTv.setText(error);
            binding.dialogErrorTv.setVisibility(View.VISIBLE);
        } else {
            binding.dialogErrorTv.setVisibility(View.GONE);
        }
    }

    private Integer parseShares() {
        String text = binding.sharesEt.getText() == null ? "" : binding.sharesEt.getText().toString();
        if (text.isEmpty()) return null;
        try { return Integer.parseInt(text); } catch (NumberFormatException e) { return null; }
    }

    private void submit() {
        Integer shares = parseShares();
        if (shares == null || shares <= 0) return;

        binding.confirmBtn.setEnabled(false);
        binding.dialogLoadingPb.setVisibility(View.VISIBLE);
        binding.dialogErrorTv.setVisibility(View.GONE);

        TradeRepository.Callback<TradeResult> cb = new TradeRepository.Callback<TradeResult>() {
            @Override public void onSuccess(TradeResult result) {
                if (binding == null) return;
                binding.dialogLoadingPb.setVisibility(View.GONE);

                String msg = mode == Mode.BUY
                        ? "Bought " + result.shares + " " + result.ticker
                        + " for " + fmt(result.totalCost) + " MAD"
                        : "Sold " + result.shares + " " + result.ticker
                        + ", received " + fmt(result.netProceeds) + " MAD";
                Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show();

                if (listener != null) listener.onTradeCompleted(result);
                dismiss();
            }

            @Override public void onError(String message) {
                if (binding == null) return;
                binding.dialogLoadingPb.setVisibility(View.GONE);
                binding.confirmBtn.setEnabled(true);
                binding.dialogErrorTv.setText(message);
                binding.dialogErrorTv.setVisibility(View.VISIBLE);
            }
        };

        if (mode == Mode.BUY) repo.buy(ticker, shares, price, cb);
        else                  repo.sell(ticker, shares, price, cb);
    }

    private static String fmt(double value) {
        return String.format(Locale.US, "%,.2f", value);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}