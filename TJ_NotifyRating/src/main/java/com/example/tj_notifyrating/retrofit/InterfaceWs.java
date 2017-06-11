package com.example.tj_notifyrating.retrofit;

import retrofit2.Call;
import retrofit2.http.GET;

public interface InterfaceWs {
    @GET("imghp")
    Call<Void> getGoogleImages();
}
