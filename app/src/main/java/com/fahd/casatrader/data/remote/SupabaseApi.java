package com.fahd.casatrader.data.remote;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface SupabaseApi {
    @GET("rest/v1/stocks")
    Call<ResponseBody> pingStocks(@Query("select") String select,
                                  @Query("limit") int limit);
}