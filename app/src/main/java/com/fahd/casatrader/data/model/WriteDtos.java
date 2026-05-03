package com.fahd.casatrader.data.model;

import com.google.gson.annotations.SerializedName;

public class WriteDtos {

    /** POST /rest/v1/transactions */
    public static class TransactionInsert {
        @SerializedName("user_id") public String userId;
        @SerializedName("ticker")  public String ticker;
        @SerializedName("type")    public String type;     // "BUY" | "SELL"
        @SerializedName("shares")  public Integer shares;
        @SerializedName("price")   public Double price;
        @SerializedName("total")   public Double total;
    }

    /** POST /rest/v1/holdings (upsert) */
    public static class HoldingUpsert {
        @SerializedName("user_id")       public String userId;
        @SerializedName("ticker")        public String ticker;
        @SerializedName("shares")        public Integer shares;
        @SerializedName("avg_buy_price") public Double avgBuyPrice;
    }

    /** PATCH /rest/v1/profiles?id=eq.<uid> — cash balance update after a trade */
    public static class CashBalanceUpdate {
        @SerializedName("cash_balance") public Double cashBalance;
        public CashBalanceUpdate(Double cashBalance) { this.cashBalance = cashBalance; }
    }

    /** POST /rest/v1/watchlist */
    public static class WatchlistInsert {
        @SerializedName("user_id") public String userId;
        @SerializedName("ticker")  public String ticker;
    }
}