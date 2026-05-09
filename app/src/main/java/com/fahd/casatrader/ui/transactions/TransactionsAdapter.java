package com.fahd.casatrader.ui.transactions;

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
import com.fahd.casatrader.data.model.Transaction;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

public class TransactionsAdapter extends ListAdapter<Transaction, TransactionsAdapter.VH> {

    public interface OnTransactionClickListener { void onTransactionClick(Transaction t); }

    private final OnTransactionClickListener listener;

    public TransactionsAdapter(OnTransactionClickListener listener) {
        super(DIFF);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<Transaction> DIFF =
            new DiffUtil.ItemCallback<Transaction>() {
                @Override public boolean areItemsTheSame(@NonNull Transaction a, @NonNull Transaction b) {
                    return Objects.equals(a.id, b.id);
                }
                @Override public boolean areContentsTheSame(@NonNull Transaction a, @NonNull Transaction b) {
                    return Objects.equals(a.id, b.id)
                            && Objects.equals(a.shares, b.shares)
                            && Objects.equals(a.price, b.price)
                            && Objects.equals(a.total, b.total);
                }
            };

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("MMM d, HH:mm", Locale.US);

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transaction, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Transaction t = getItem(position);
        Context ctx = h.itemView.getContext();
        boolean isBuy = Transaction.TYPE_BUY.equals(t.type);

        h.typePillTv.setText(t.type);
        h.typePillTv.setBackgroundResource(isBuy
                ? R.drawable.bg_pill_buy
                : R.drawable.bg_pill_sell);
        h.typePillTv.setTextColor(ContextCompat.getColor(ctx, isBuy
                ? R.color.pill_loss_text
                : R.color.pill_gain_text));

        h.tickerTv.setText(t.ticker);

        h.sharesTv.setText(String.format(Locale.US, "%d shares @ %.2f MAD",
                t.shares != null ? t.shares : 0,
                t.price != null ? t.price : 0));

        double cashImpact = t.total != null ? t.total : 0;
        String sign = isBuy ? "−" : "+";
        h.totalTv.setText(String.format(Locale.US, "%s%,.2f", sign, cashImpact));
        h.totalTv.setTextColor(ContextCompat.getColor(ctx,
                isBuy ? R.color.loss_red : R.color.gain_green));

        h.dateTv.setText(formatDate(t.createdAt));

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onTransactionClick(t);
        });
    }

    private static String formatDate(String iso) {
        if (iso == null) return "";
        try {
            OffsetDateTime odt = OffsetDateTime.parse(iso);
            return odt.atZoneSameInstant(ZoneId.systemDefault()).format(DATE_FMT);
        } catch (Exception e) {
            return iso.length() >= 10 ? iso.substring(0, 10) : iso;
        }
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView typePillTv, tickerTv, sharesTv, dateTv, totalTv;
        VH(@NonNull View v) {
            super(v);
            typePillTv = v.findViewById(R.id.typePillTv);
            tickerTv   = v.findViewById(R.id.tickerTv);
            sharesTv   = v.findViewById(R.id.sharesTv);
            dateTv     = v.findViewById(R.id.dateTv);
            totalTv    = v.findViewById(R.id.totalTv);
        }
    }
}