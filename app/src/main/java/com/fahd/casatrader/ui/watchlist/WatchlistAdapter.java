package com.fahd.casatrader.ui.watchlist;

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
import com.fahd.casatrader.data.model.WatchlistEntry;
import com.google.android.material.button.MaterialButton;

import java.util.Locale;
import java.util.Objects;

public class WatchlistAdapter extends ListAdapter<WatchlistEntry, WatchlistAdapter.VH> {

    public interface OnEntryActionListener {
        void onEntryClick(WatchlistEntry entry);
        void onRemoveClick(WatchlistEntry entry);
    }

    private final OnEntryActionListener listener;

    public WatchlistAdapter(OnEntryActionListener listener) {
        super(DIFF);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<WatchlistEntry> DIFF =
            new DiffUtil.ItemCallback<WatchlistEntry>() {
                @Override public boolean areItemsTheSame(@NonNull WatchlistEntry a, @NonNull WatchlistEntry b) {
                    return Objects.equals(a.ticker, b.ticker);
                }
                @Override public boolean areContentsTheSame(@NonNull WatchlistEntry a, @NonNull WatchlistEntry b) {
                    return a.stock != null && b.stock != null
                            && Objects.equals(a.stock.price, b.stock.price)
                            && Objects.equals(a.stock.changePercent, b.stock.changePercent);
                }
            };

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_watchlist, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        WatchlistEntry e = getItem(position);
        Context ctx = h.itemView.getContext();

        h.tickerTv.setText(e.ticker);
        h.nameTv.setText(e.stock != null && e.stock.name != null ? e.stock.name : "");
        h.priceTv.setText(e.stock != null && e.stock.price != null
                ? String.format(Locale.US, "%,.2f", e.stock.price)
                : "—");

        if (e.stock != null && e.stock.changePercent != null) {
            double cp = e.stock.changePercent;
            String sign = cp > 0 ? "+" : "";
            h.changeTv.setText(String.format(Locale.US, "%s%.2f%%", sign, cp));
            int colorRes = cp > 0 ? R.color.gain_green
                    : cp < 0 ? R.color.loss_red
                    : R.color.neutral_gray;
            h.changeTv.setTextColor(ContextCompat.getColor(ctx, colorRes));
        } else {
            h.changeTv.setText("—");
        }

        h.itemView.setOnClickListener(v -> { if (listener != null) listener.onEntryClick(e); });
        h.removeBtn.setOnClickListener(v -> { if (listener != null) listener.onRemoveClick(e); });
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tickerTv, nameTv, priceTv, changeTv;
        final MaterialButton removeBtn;
        VH(@NonNull View v) {
            super(v);
            tickerTv  = v.findViewById(R.id.tickerTv);
            nameTv    = v.findViewById(R.id.nameTv);
            priceTv   = v.findViewById(R.id.priceTv);
            changeTv  = v.findViewById(R.id.changeTv);
            removeBtn = v.findViewById(R.id.removeBtn);
        }
    }
}