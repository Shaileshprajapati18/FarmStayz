package com.example.farmstayz.Networks;

import com.example.farmstayz.Model.BookingRequest;
import com.example.farmstayz.Model.FarmhouseListResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface ApiService {
    @GET("/api/farmhouses")
    Call<FarmhouseListResponse> getAllFarmhouses();

    @POST("api/bookings")
    Call<FarmhouseListResponse> createBooking(@Body BookingRequest bookingRequest);
}