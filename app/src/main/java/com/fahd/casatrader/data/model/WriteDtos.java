package com.fahd.casatrader.data.model;

import com.google.gson.annotations.SerializedName;

public class WriteDtos {

    public static class TransactionInsert {
        @SerializedName("user_id") public String userId;
        @SerializedName("ticker")  public String ticker;
        @SerializedName("type")    public String type;
        @SerializedName("shares")  public Integer shares;
        @SerializedName("price")   public Double price;
        @SerializedName("total")   public Double total;
    }

    public static class HoldingUpsert {
        @SerializedName("user_id")       public String userId;
        @SerializedName("ticker")        public String ticker;
        @SerializedName("shares")        public Integer shares;
        @SerializedName("avg_buy_price") public Double avgBuyPrice;
    }

    public static class CashBalanceUpdate {
        @SerializedName("cash_balance") public Double cashBalance;
        public CashBalanceUpdate(Double cashBalance) { this.cashBalance = cashBalance; }
    }

    public static class WatchlistInsert {
        @SerializedName("user_id") public String userId;
        @SerializedName("ticker")  public String ticker;
    }

    public static class BuyRequest {
        @SerializedName("p_ticker") public String ticker;
        @SerializedName("p_shares") public Integer shares;
        @SerializedName("p_price")  public Double price;
        public BuyRequest(String ticker, Integer shares, Double price) {
            this.ticker = ticker; this.shares = shares; this.price = price;
        }
    }

    public static class SellRequest {
        @SerializedName("p_ticker") public String ticker;
        @SerializedName("p_shares") public Integer shares;
        @SerializedName("p_price")  public Double price;
        public SellRequest(String ticker, Integer shares, Double price) {
            this.ticker = ticker; this.shares = shares; this.price = price;
        }
    }

    public static class TradeResult {
        @SerializedName("transaction_id")     public Long transactionId;
        @SerializedName("ticker")             public String ticker;
        @SerializedName("type")               public String type;
        @SerializedName("shares")             public Integer shares;
        @SerializedName("price")              public Double price;
        @SerializedName("gross")              public Double gross;
        @SerializedName("fee")                public Double fee;
        @SerializedName("total_cost")         public Double totalCost;
        @SerializedName("new_cash_balance")   public Double newCashBalance;
        @SerializedName("new_holding_shares") public Integer newHoldingShares;
        @SerializedName("new_avg_buy_price")  public Double newAvgBuyPrice;
        @SerializedName("net_proceeds")       public Double netProceeds;
        @SerializedName("remaining_shares")   public Integer remainingShares;
        @SerializedName("realized_pl")        public Double realizedPl;
    }
}