package com.example.tj_notifyrating.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.example.tj_notifyrating.retrofit.ServiceWs;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;

public class Stuff {

    /**
     * if "debugMode" == "true" print the collected logs
     */
    public void showLogs(Boolean debugMode, ArrayList<String> logsCollector) {
        if (debugMode) {
            for (String log : logsCollector) {
                Log.d("debugMode_NotifyRating", "" + log);
            }
            logsCollector.add("*");
        }
    }

    /**
     * Transform milliseconds to hour:minut:second
     *
     * @param millis - milliseconds to transfor; Ex : 20000 millis = 00:00:20
     * @return
     */
    @SuppressLint("DefaultLocale")
    public String millisToTime(int millis) {
        return String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(millis), TimeUnit.MILLISECONDS.toMinutes(millis) -
                TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)), TimeUnit.MILLISECONDS.toSeconds(millis) -
                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));
    }

    /**
     * By this method using Retrofit we can check if there is live internet or not
     *
     * @param context - context
     * @return true if there is live internet
     */
    public boolean checkForInternetConnection(Context context) {
        Boolean isInternetConnection;
        Call<Void> mCall = ServiceWs.getInstance(context).getInterface().getGoogleImages();
        try {
            mCall.execute();
            isInternetConnection = true;
        } catch (IOException e) {
            isInternetConnection = false;
        }
        return isInternetConnection;
    }

    /**
     * Check if there is any open connection; By wifi or mobile
     *
     * @param context
     * @return true if at least one conection is open ( this not guarantee that there is live internet connection )
     */

    public boolean haveNetworkConnection(Context context) {
        boolean haveConnectedWifi = false;
        boolean haveConnectedMobile = false;

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo[] netInfo = cm.getAllNetworkInfo();
        for (NetworkInfo ni : netInfo) {
            if (ni.getTypeName().equalsIgnoreCase("WIFI"))
                if (ni.isConnected())
                    haveConnectedWifi = true;
            if (ni.getTypeName().equalsIgnoreCase("MOBILE"))
                if (ni.isConnected())
                    haveConnectedMobile = true;
        }
        return haveConnectedWifi || haveConnectedMobile;
    }

    /**
     * Convert milliseconds in date format
     *
     * @param dateInMilliseconds - milliseconds to transfor
     * @return date
     */
    public String convertDate(long dateInMilliseconds) {
        String dateFormat = "dd/MM/yyyy hh:mm:ss";
        SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
        Date resultdate = new Date(dateInMilliseconds);
        return sdf.format(resultdate);
    }
}
