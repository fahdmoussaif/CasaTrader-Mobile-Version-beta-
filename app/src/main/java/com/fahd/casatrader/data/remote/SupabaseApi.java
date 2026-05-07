package com.fahd.casatrader.data.remote;

import com.fahd.casatrader.data.model.Profile;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;

public interface SupabaseApi {

    /** Fetch current user's profile. RLS scopes this to auth.uid() automatically. */
    @GET("rest/v1/profiles")
    Call<List<Profile>> getMyProfile(@Query("id") String idEq,             // "eq.<uuid>"
                                     @Query("select") String select);

    /**
     * Variant that returns a single object instead of a list.
     * PostgREST honors Accept: application/vnd.pgrst.object+json when exactly one row matches.
     */
    @GET("rest/v1/profiles")
    Call<Profile> getMyProfileSingle(@Header("Accept") String accept,
                                     @Query("id") String idEq,
                                     @Query("select") String select);
}