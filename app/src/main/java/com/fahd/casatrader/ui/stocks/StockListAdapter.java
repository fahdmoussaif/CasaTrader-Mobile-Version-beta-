package com.fahd.casatrader.ui.stocks;

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
import com.fahd.casatrader.data.model.Stock;

import java.util.Locale;
import java.util.Objects;

public class StockListAdapter extends ListAdapter<Stock, StockListAdapter.VH> {

    public interface OnStockClickListener {
        void onStockClick(Stock stock);
    }

    private final OnStockClickListener listener;

    public StockListAdapter(OnStockClickListener listener) {
        super(DIFF);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<Stock> DIFF = new DiffUtil.ItemCallback<Stock>() {
        @Override public boolean areItemsTheSame(@NonNull Stock a, @NonNull Stock b) {
            return Objects.equals(a.ticker, b.ticker);
        }
        @Override public boolean areContentsTheSame(@NonNull Stock a, @NonNull Stock b) {
            return Objects.equals(a.price, b.price)
                    && Objects.equals(a.changePercent, b.changePercent)
                    && Objects.equals(a.name, b.name);
        }
    };

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_stock, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Stock s = getItem(position);
        Context ctx = h.itemView.getContext();

        h.tickerTv.setText(s.ticker != null ? s.ticker : "—");
        h.nameTv.setText(s.name != null ? s.name : "");

        h.priceTv.setText(s.price != null
                ? String.format(Locale.US, "%,.2f", s.price)
                : "—");

        if (s.changePercent != null) {
            double cp = s.changePercent;
            String sign = cp > 0 ? "+" : "";
            h.changeTv.setText(String.format(Locale.US, "%s%.2f%%", sign, cp));
            int colorRes = cp > 0 ? R.color.gain_green
                    : cp < 0 ? R.color.loss_red
                    : R.color.neutral_gray;
            h.changeTv.setTextColor(ContextCompat.getColor(ctx, colorRes));
        } else {
            h.changeTv.setText("—");
            h.changeTv.setTextColor(ContextCompat.getColor(ctx, R.color.neutral_gray));
        }

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onStockClick(s);
        });
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tickerTv, nameTv, priceTv, changeTv;
        VH(@NonNull View v) {
            super(v);
            tickerTv = v.findViewById(R.id.tickerTv);
            nameTv = v.findViewById(R.id.nameTv);
            priceTv = v.findViewById(R.id.priceTv);
            changeTv = v.findViewById(R.id.changeTv);
        }
    }
}