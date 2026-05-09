package com.fahd.casatrader.data.model;

import com.google.gson.annotations.SerializedName;

public class Profile {
    @SerializedName("id")           public String id;
    @SerializedName("username")     public String username;
    @SerializedName("email")        public String email;
    @SerializedName("cash_balance") public Double cashBalance;
    @SerializedName("created_at")   public String createdAt;
}