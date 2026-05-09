package com.fahd.casatrader.data.remote;

import com.fahd.casatrader.data.model.Profile;
import com.fahd.casatrader.data.model.Stock;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;

public interface SupabaseApi {

    @GET("rest/v1/profiles")
    Call<List<Profile>> getMyProfile(@Query("id") String idEq,             // "eq.<uuid>"
                                     @Query("select") String select);


    @GET("rest/v1/profiles")
    Call<Profile> getMyProfileSingle(@Header("Accept") String accept,
                                     @Query("id") String idEq,
                                     @Query("select") String select);
    @GET("rest/v1/stocks")
    Call<List<Stock>> getAllStocks(@Query("select") String select,
                                   @Query("order") String order);
}