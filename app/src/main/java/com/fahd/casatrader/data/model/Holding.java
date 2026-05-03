package com.fahd.casatrader.data.model;

import com.google.gson.annotations.SerializedName;

public class Holding {
    @SerializedName("id")             public Long id;
    @SerializedName("user_id")        public String userId;
    @SerializedName("ticker")         public String ticker;
    @SerializedName("shares")         public Integer shares;
    @SerializedName("avg_buy_price")  public Double avgBuyPrice;
    @SerializedName("updated_at")     public String updatedAt;
}