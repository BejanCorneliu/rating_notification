package com.example.tj_notifyrating.recivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.example.tj_notifyrating.R;
import com.example.tj_notifyrating.utils.Stuff;

import java.util.ArrayList;

import static com.example.tj_notifyrating.utils.Constants.DEFAULT_DEBUG_MODE;

public class ShutdownReceiver extends BroadcastReceiver {

    private Stuff myStuff = new Stuff();
    private ArrayList<String> logsCollector = new ArrayList<>();

    @Override
    public void onReceive(Context context, Intent intent) {

        logsCollector.add("*");
        logsCollector.add("------- ShutdownReceiver");

        Log.d("debugMode_NotifyRating", "------- ShutdownReceiver");
        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(context);
        Boolean pushFlag=shared.getBoolean(context.getResources().getString(R.string.push_notification_flag), false);
        Boolean pushIdle=shared.getBoolean(context.getResources().getString(R.string.push_notification_idle), false);
        Boolean debugMode = shared.getBoolean(context.getResources().getString(R.string.pref_key_debug), DEFAULT_DEBUG_MODE);

        logsCollector.add("*");
        logsCollector.add("* PushFlag : " + pushFlag);
        logsCollector.add("* PushIdle : " + pushIdle);

        myStuff.showLogs(debugMode,logsCollector);
    }
}
