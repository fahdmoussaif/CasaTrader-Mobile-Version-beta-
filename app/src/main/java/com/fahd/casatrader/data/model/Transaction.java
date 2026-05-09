package com.fahd.casatrader.data.model;

import com.google.gson.annotations.SerializedName;

public class Transaction {
    public static final String TYPE_BUY  = "BUY";
    public static final String TYPE_SELL = "SELL";

    @SerializedName("id")         public Long id;
    @SerializedName("user_id")    public String userId;
    @SerializedName("ticker")     public String ticker;
    @SerializedName("type")       public String type;
    @SerializedName("shares")     public Integer shares;
    @SerializedName("price")      public Double price;
    @SerializedName("total")      public Double total;
    @SerializedName("created_at") public String createdAt;
}