package com.fahd.casatrader.data.model;

import com.google.gson.annotations.SerializedName;

public class Stock {
    @SerializedName("ticker")          public String ticker;
    @SerializedName("symbol_id")       public Integer symbolId;
    @SerializedName("name")            public String name;
    @SerializedName("sector")          public String sector;
    @SerializedName("status")          public String status;

    @SerializedName("price")           public Double price;
    @SerializedName("open")            public Double open;
    @SerializedName("high")            public Double high;
    @SerializedName("low")             public Double low;
    @SerializedName("previous_close")  public Double previousClose;
    @SerializedName("change_percent")  public Double changePercent;

    @SerializedName("volume")          public Double volume;
    @SerializedName("shares_traded")   public Long sharesTraded;
    @SerializedName("trades_count")    public Integer tradesCount;
    @SerializedName("market_cap")      public Double marketCap;

    @SerializedName("best_bid")        public Double bestBid;
    @SerializedName("best_bid_size")   public Integer bestBidSize;
    @SerializedName("best_ask")        public Double bestAsk;
    @SerializedName("best_ask_size")   public Integer bestAskSize;

    @SerializedName("updated_at")      public String updatedAt;
}