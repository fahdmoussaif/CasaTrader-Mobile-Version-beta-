package com.fahd.casatrader.data.remote;

import com.fahd.casatrader.data.model.HoldingWithStock;
import com.fahd.casatrader.data.model.PriceSnapshot;
import com.fahd.casatrader.data.model.Profile;
import com.fahd.casatrader.data.model.Stock;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;
import com.fahd.casatrader.data.model.Holding;
import com.fahd.casatrader.data.model.Transaction;
import com.fahd.casatrader.data.model.WatchlistEntry;
import com.fahd.casatrader.data.model.WriteDtos;
import com.fahd.casatrader.data.model.WriteDtos.BuyRequest;
import com.fahd.casatrader.data.model.WriteDtos.SellRequest;
import com.fahd.casatrader.data.model.WriteDtos.TradeResult;

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
    @GET("rest/v1/stocks")
    Call<Stock> getStockSingle(@Header("Accept") String accept,
                               @Query("ticker") String tickerEq,
                               @Query("select") String select);

    @GET("rest/v1/price_snapshots")
    Call<List<PriceSnapshot>> getPriceHistory(@Query("ticker") String tickerEq,
                                              @Query("select") String select,
                                              @Query("order") String order,
                                              @Query("limit") Integer limit);
    @POST("rest/v1/rpc/buy_stock")
    Call<TradeResult> buyStock(@Body BuyRequest body);

    @POST("rest/v1/rpc/sell_stock")
    Call<TradeResult> sellStock(@Body SellRequest body);

    /** Holding for current user + given ticker. Returns 0 or 1 row. */
    @GET("rest/v1/holdings")
    Call<List<Holding>> getMyHolding(@Query("ticker") String tickerEq,
                                     @Query("select") String select);
    @GET("rest/v1/holdings")
    Call<List<HoldingWithStock>> getMyHoldingsWithStock(@Query("select") String select,
                                                        @Query("order") String order);
    @GET("rest/v1/transactions")
    Call<List<Transaction>> getMyTransactions(@Query("select") String select,
                                              @Query("order") String order);
    @GET("rest/v1/watchlist")
    Call<List<WatchlistEntry>> getMyWatchlist(@Query("select") String select,
                                              @Query("order") String order);

    /** Check whether one specific ticker is on the watchlist. Returns 0 or 1 row. */
    @GET("rest/v1/watchlist")
    Call<List<WatchlistEntry>> getWatchlistEntry(@Query("ticker") String tickerEq,
                                                 @Query("select") String select);

    @POST("rest/v1/watchlist")
    Call<Void> addToWatchlist(@Header("Prefer") String prefer,
                              @Body WriteDtos.WatchlistInsert body);

    @DELETE("rest/v1/watchlist")
    Call<Void> removeFromWatchlist(@Query("ticker") String tickerEq);

}