package com.fahd.casatrader.ui.portfolio;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.fahd.casatrader.R;
import com.fahd.casatrader.data.model.HoldingWithStock;

import java.util.Locale;
import java.util.Objects;

public class HoldingsAdapter extends ListAdapter<HoldingWithStock, HoldingsAdapter.VH> {

    public interface OnHoldingClickListener { void onHoldingClick(HoldingWithStock h); }

    private final OnHoldingClickListener listener;

    public HoldingsAdapter(OnHoldingClickListener listener) {
        super(DIFF);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<HoldingWithStock> DIFF =
            new DiffUtil.ItemCallback<HoldingWithStock>() {
                @Override public boolean areItemsTheSame(@NonNull HoldingWithStock a, @NonNull HoldingWithStock b) {
                    return Objects.equals(a.ticker, b.ticker);
                }
                @Override public boolean areContentsTheSame(@NonNull HoldingWithStock a, @NonNull HoldingWithStock b) {
                    return Objects.equals(a.shares, b.shares)
                            && Objects.equals(a.avgBuyPrice, b.avgBuyPrice)
                            && a.stock != null && b.stock != null
                            && Objects.equals(a.stock.price, b.stock.price);
                }
            };

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_holding, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        HoldingWithStock item = getItem(pos);
        Context ctx = h.itemView.getContext();

        h.tickerTv.setText(item.ticker);
        h.sharesTv.setText(String.format(Locale.US, "%d shares · avg %.2f",
                item.shares != null ? item.shares : 0,
                item.avgBuyPrice != null ? item.avgBuyPrice : 0));
        h.valueTv.setText(String.format(Locale.US, "%,.2f", item.currentValue()));

        double pl = item.unrealizedPl();
        double plPct = item.unrealizedPlPercent();
        String sign = pl > 0 ? "+" : "";
        h.plTv.setText(String.format(Locale.US, "%s%,.2f (%s%.2f%%)", sign, pl, sign, plPct));
        int colorRes = pl > 0 ? R.color.gain_green
                : pl < 0 ? R.color.loss_red
                : R.color.neutral_gray;
        h.plTv.setTextColor(ContextCompat.getColor(ctx, colorRes));

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onHoldingClick(item);
        });
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tickerTv, sharesTv, valueTv, plTv;
        VH(@NonNull View v) {
            super(v);
            tickerTv = v.findViewById(R.id.tickerTv);
            sharesTv = v.findViewById(R.id.sharesTv);
            valueTv = v.findViewById(R.id.valueTv);
            plTv = v.findViewById(R.id.plTv);
        }
    }
}