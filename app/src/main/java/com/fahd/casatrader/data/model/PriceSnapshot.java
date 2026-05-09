package com.fahd.casatrader.data.model;

import com.google.gson.annotations.SerializedName;

public class PriceSnapshot {
    @SerializedName("id")             public Long id;
    @SerializedName("ticker")         public String ticker;
    @SerializedName("trade_date")     public String tradeDate;
    @SerializedName("price")          public Double price;
    @SerializedName("open")           public Double open;
    @SerializedName("high")           public Double high;
    @SerializedName("low")            public Double low;
    @SerializedName("volume")         public Double volume;
    @SerializedName("change_percent") public Double changePercent;
    @SerializedName("recorded_at")    public String recordedAt;
}