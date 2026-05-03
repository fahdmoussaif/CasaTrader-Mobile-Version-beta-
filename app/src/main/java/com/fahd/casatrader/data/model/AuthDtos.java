package com.fahd.casatrader.data.model;

import com.google.gson.annotations.SerializedName;

public class AuthDtos {

    /** Body for /auth/v1/signup and /auth/v1/token?grant_type=password */
    public static class EmailPasswordRequest {
        @SerializedName("email")    public String email;
        @SerializedName("password") public String password;
        @SerializedName("data")     public Object data;   // optional metadata, e.g. {"username": "..."}

        public EmailPasswordRequest(String email, String password) {
            this.email = email; this.password = password;
        }
        public EmailPasswordRequest(String email, String password, Object data) {
            this(email, password); this.data = data;
        }
    }

    /** Response from /auth/v1/token?grant_type=password (login) */
    public static class AuthSession {
        @SerializedName("access_token")  public String accessToken;
        @SerializedName("refresh_token") public String refreshToken;
        @SerializedName("token_type")    public String tokenType;
        @SerializedName("expires_in")    public Long expiresIn;     // seconds from now
        @SerializedName("expires_at")    public Long expiresAt;     // epoch seconds
        @SerializedName("user")          public AuthUser user;
    }

    /** The "user" sub-object inside AuthSession (and signup response). */
    public static class AuthUser {
        @SerializedName("id")    public String id;
        @SerializedName("email") public String email;
    }

    /** Body for /auth/v1/token?grant_type=refresh_token */
    public static class RefreshRequest {
        @SerializedName("refresh_token") public String refreshToken;
        public RefreshRequest(String refreshToken) { this.refreshToken = refreshToken; }
    }
}