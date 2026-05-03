package com.fahd.casatrader.data.model;

import com.google.gson.annotations.SerializedName;

public class WatchlistEntry {
    @SerializedName("user_id")  public String userId;
    @SerializedName("ticker")   public String ticker;
    @SerializedName("added_at") public String addedAt;

    // Useful when fetching watchlist with a join: ?select=ticker,added_at,stocks(*)
    @SerializedName("stocks")   public Stock stock;
}