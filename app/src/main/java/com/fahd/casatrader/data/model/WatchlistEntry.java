package com.fahd.casatrader.data.model;

import com.google.gson.annotations.SerializedName;

public class WatchlistEntry {
    @SerializedName("user_id")  public String userId;
    @SerializedName("ticker")   public String ticker;
    @SerializedName("added_at") public String addedAt;

    @SerializedName("stocks")   public Stock stock;
}