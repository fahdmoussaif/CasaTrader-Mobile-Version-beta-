package com.fahd.casatrader.data.model;

import com.google.gson.annotations.SerializedName;

public class HoldingWithStock {
    @SerializedName("id")             public Long id;
    @SerializedName("user_id")        public String userId;
    @SerializedName("ticker")         public String ticker;
    @SerializedName("shares")         public Integer shares;
    @SerializedName("avg_buy_price")  public Double avgBuyPrice;
    @SerializedName("updated_at")     public String updatedAt;

    @SerializedName("stocks")         public Stock stock;

    public double currentValue() {
        if (stock == null || stock.price == null || shares == null) return 0;
        return stock.price * shares;
    }

    public double costBasis() {
        if (avgBuyPrice == null || shares == null) return 0;
        return avgBuyPrice * shares;
    }

    public double unrealizedPl() {
        return currentValue() - costBasis();
    }

    public double unrealizedPlPercent() {
        double cost = costBasis();
        if (cost == 0) return 0;
        return (unrealizedPl() / cost) * 100.0;
    }
}