package com.fahd.casatrader.data.model;

import com.google.gson.annotations.SerializedName;

public class AuthDtos {

    public static class EmailPasswordRequest {
        @SerializedName("email")    public String email;
        @SerializedName("password") public String password;
        @SerializedName("data")     public Object data;

        public EmailPasswordRequest(String email, String password) {
            this.email = email; this.password = password;
        }
        public EmailPasswordRequest(String email, String password, Object data) {
            this(email, password); this.data = data;
        }
    }

    public static class AuthSession {
        @SerializedName("access_token")  public String accessToken;
        @SerializedName("refresh_token") public String refreshToken;
        @SerializedName("token_type")    public String tokenType;
        @SerializedName("expires_in")    public Long expiresIn;
        @SerializedName("expires_at")    public Long expiresAt;
        @SerializedName("user")          public AuthUser user;
    }

    public static class AuthUser {
        @SerializedName("id")    public String id;
        @SerializedName("email") public String email;
    }

    public static class RefreshRequest {
        @SerializedName("refresh_token") public String refreshToken;
        public RefreshRequest(String refreshToken) { this.refreshToken = refreshToken; }
    }
}