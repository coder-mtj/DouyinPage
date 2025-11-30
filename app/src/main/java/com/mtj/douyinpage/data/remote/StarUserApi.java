package com.mtj.douyinpage.data.remote;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface StarUserApi {

    @GET("api/stars")
    Call<StarUserPageResponse> list(@Query("page") int page, @Query("size") int size);

    @PATCH("api/stars/{id}")
    Call<Void> updateUser(@Path("id") int id, @Body UpdateStarUserRequestBody body);
}
