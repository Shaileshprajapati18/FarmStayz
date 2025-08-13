package com.example.farmstayz.Networks;

import com.example.farmstayz.Model.FarmhouseListResponse;
import retrofit2.Call;
import retrofit2.http.GET;

public interface ApiService {
    @GET("/api/farmhouses")
    Call<FarmhouseListResponse> getAllFarmhouses();
}