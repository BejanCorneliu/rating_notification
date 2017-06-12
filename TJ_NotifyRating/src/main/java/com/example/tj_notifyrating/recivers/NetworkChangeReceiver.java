package com.example.tj_notifyrating.recivers;

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

import com.example.tj_notifyrating.R;
import com.example.tj_notifyrating.ServiceNotification;
import com.example.tj_notifyrating.utils.Stuff;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import static com.example.tj_notifyrating.utils.Constants.DEFAULT_DEBUG_MODE;

public class NetworkChangeReceiver extends BroadcastReceiver {

    private Stuff myStuff = new Stuff();
    private ArrayList<String> logsCollector = new ArrayList<>();

    @Override
    public void onReceive(final Context context, final Intent intent) {

        logsCollector.add("*");
        logsCollector.add("------- NetworkChangeReceiver");

        try {
            if (isConnect(context)) {
                logsCollector.add("* InternetConnection : TRUE");
                wakeUpService(context);
            } else {
                logsCollector.add("* InternetConnection : FALSE");
            }
        } catch (Exception e) {
            logsCollector.add("* InternetConnection : FALSE!!");
        }
    }

    private void wakeUpService(Context context) {
        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(context);
        Boolean pushFlag = shared.getBoolean(context.getResources().getString(R.string.push_notification_flag), false);
        Boolean pushIdle = shared.getBoolean(context.getResources().getString(R.string.push_notification_idle), false);
        Boolean debugMode = shared.getBoolean(context.getResources().getString(R.string.pref_key_debug), DEFAULT_DEBUG_MODE);

        myStuff = new Stuff();

        logsCollector.add("*");
        logsCollector.add("* PushFlag : " + pushFlag);
        logsCollector.add("* PushIdle : " + pushIdle);

        if (pushFlag != pushIdle) {

            SharedPreferences.Editor sharedEdit = PreferenceManager.getDefaultSharedPreferences(context).edit();
            sharedEdit.putBoolean(context.getString(R.string.push_notification_flag), true);
            sharedEdit.putBoolean(context.getString(R.string.push_notification_idle), false);
            sharedEdit.commit();

            shared = PreferenceManager.getDefaultSharedPreferences(context);
            Long nextWakeUp = shared.getLong(context.getResources().getString(R.string.push_notification_wakeup_time), 0);

            if (nextWakeUp == 0) {
                nextWakeUp = System.currentTimeMillis() + 10000;
                logsCollector.add("* The notification was not set to appear");
            } else if (System.currentTimeMillis() >= nextWakeUp) {
                logsCollector.add("* The notification was set to appearis set to appear at : " + myStuff.convertDate(nextWakeUp));
                nextWakeUp = System.currentTimeMillis() + 10000;
                logsCollector.add("* So ... we need to hurry and show the notification ASAP : " + myStuff.convertDate(nextWakeUp));
            } else if (System.currentTimeMillis() <= nextWakeUp) {
                nextWakeUp = 10000 + nextWakeUp;
                logsCollector.add("* The notification is set to appear at : " + myStuff.convertDate(nextWakeUp));
            }

            ((AlarmManager) context.getSystemService(Context.ALARM_SERVICE)).set(0, nextWakeUp, PendingIntent.getService(context, 0, new Intent(context, ServiceNotification.class), 0));
            logsCollector.add("* WakeUp Service with alarm : true | Arround : " + myStuff.convertDate(((nextWakeUp))));
        } else {
            logsCollector.add("* WakeUp Service with alarm : false");
        }

        myStuff.showLogs(debugMode,logsCollector);
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


