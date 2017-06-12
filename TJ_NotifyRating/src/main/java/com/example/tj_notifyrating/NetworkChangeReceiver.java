package com.example.tj_notifyrating;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

public class NetworkChangeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, final Intent intent) {

        Log.d("debugMode_NotifyRating", "*");
        Log.d("debugMode_NotifyRating", "------- NetworkChangeReceiver");

        try {
            if(isConnect(context))
            {
                Log.d("debugMode_NotifyRating", "* InternetConnection : TRUE");
                wakeUpService(context);
            } else {
                Log.d("debugMode_NotifyRating", "* InternetConnection : FALSE");
            }
        } catch (Exception e) {
            Log.d("debugMode_NotifyRating", "* InternetConnection : FALSE!!");
        }
    }

    private void wakeUpService(Context context) {
        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(context);
        Boolean pushFlag=shared.getBoolean(context.getResources().getString(R.string.push_notification_flag), false);
        Boolean pushIdle=shared.getBoolean(context.getResources().getString(R.string.push_notification_idle), false);

        Log.d("debugMode_NotifyRating","*");
        Log.d("debugMode_NotifyRating","* PushFlag : "+pushFlag);
        Log.d("debugMode_NotifyRating","* PushIdle : "+pushIdle);

        if (!pushFlag && pushIdle) {

            SharedPreferences.Editor sharedEdit = PreferenceManager.getDefaultSharedPreferences(context).edit();
            sharedEdit.putBoolean(context.getString(R.string.push_notification_flag), true);
            sharedEdit.commit();

            ((AlarmManager) context.getSystemService(Context.ALARM_SERVICE)).set(0, System.currentTimeMillis()+10000, PendingIntent.getService(context, 0, new Intent(context, ServiceNotification.class), 0));
            Log.d("debugMode_NotifyRating", "* WakeUp Service with alarm : true");
        } else {
            Log.d("debugMode_NotifyRating", "* WakeUp Service with alarm : false");
        }
    }

    private boolean isConnect(Context context) {

        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Network[] netArray = connectivityManager.getAllNetworks();
            NetworkInfo netInfo;
            for (Network net : netArray) {
                netInfo = connectivityManager.getNetworkInfo(net);
                if ((netInfo.getTypeName().equalsIgnoreCase("WIFI") || netInfo.getTypeName().equalsIgnoreCase("MOBILE")) && netInfo.isConnected() && netInfo.isAvailable()) {
                    return true;
                }
            }
        } else {
            if (connectivityManager != null) {
                @SuppressWarnings("deprecation")
                NetworkInfo[] netInfoArray = connectivityManager.getAllNetworkInfo();
                if (netInfoArray != null) {
                    for (NetworkInfo netInfo : netInfoArray) {
                        if ((netInfo.getTypeName().equalsIgnoreCase("WIFI") || netInfo.getTypeName().equalsIgnoreCase("MOBILE")) && netInfo.isConnected() && netInfo.isAvailable()) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}


