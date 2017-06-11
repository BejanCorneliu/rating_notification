package com.example.tj_notifyrating.retrofit;

import android.content.Context;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class ServiceWs {
    private static ServiceWs instance;
    private InterfaceWs mInterfaceWs;

    public synchronized static ServiceWs getInstance(Context context) {
        if (instance != null) {
            return instance;
        } else {
            return instance = new ServiceWs(context);
        }
    }

    private ServiceWs(Context context) {
        Retrofit retrofit = new Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl("https://www.google.ro")
                .build();

        mInterfaceWs = retrofit.create(InterfaceWs.class);
    }

    public InterfaceWs getInterface() {
        return mInterfaceWs;
    }
}
