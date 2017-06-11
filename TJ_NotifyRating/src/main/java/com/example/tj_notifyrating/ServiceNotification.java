package com.example.tj_notifyrating;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;


import com.example.tj_notifyrating.retrofit.ServiceWs;

import java.io.IOException;
import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Response;

import static com.example.tj_notifyrating.Constants.DEFAULT_DEBUG_MODE;
import static com.example.tj_notifyrating.Constants.DEFAULT_DELAY_BETWEEN_ATTEMPTS_INTERNET_NOT_SURE;
import static com.example.tj_notifyrating.Constants.DEFAULT_DELAY_BETWEEN_NOTIFICATIONS;
import static com.example.tj_notifyrating.Constants.DEFAULT_DELAY_NR_TIMES;

public class ServiceNotification extends Service {

    /* number of times notification was send */
    private int nrTimesNotficationAppear;
    /* number of time notification should appear one after one by millisSecondsDelay */
    private int nrTimesNotficationShouldAppear;
    /* milliseconds delay between onCreate() time and first notification*/
    private int millisSecondsDelay;
    /* milliseconds delay between new attempts when internet connection is not sure*/
    private int millisSecondsAttempts;
    /* number of times of notifications */
    private int nrTimes;
    /* if set to "true" Log.d will print */
    private boolean debugMode;
    /* if is set to "false" => no notification and no service alarm*/
    private Boolean pushFlag;
    /* waiting to start ....*/
    private Intent onTapIntent,restart;
    /* ArrayList that will colect all logs and prind at the end */

    private String notificationTitle,notificationSubtitle;
    private int notificationIcon;

    private int nextWakeUp;
    private String typeInternetLive = "none";

    private ArrayList<String> logsCollector;
    SharedPreferences.Editor sharedEdit;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        getSettingsFromPref();
        logsCollector = new ArrayList<>();

        if (intent.getExtras().getString(getResources().getString(R.string.intent_key_first_launch)) != null) {
            stopSelf();
        } else {
            logsCollector.add("*");
            logsCollector.add("------- Notification no: "+nrTimesNotficationAppear);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    notificationLogin(ServiceNotification.this,onTapIntent);
                }
            }).start();
        }
        return Service.START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void onDestroy() {
        if (pushFlag) {

            if (nextWakeUp==0) {
                nextWakeUp = millisSecondsDelay; //corection
            }

            logsCollector.add("* Service wake up : true | Next wake up in : "+nextWakeUp+" millis");
            ((AlarmManager) getSystemService(Context.ALARM_SERVICE)).set(0, System.currentTimeMillis() + nextWakeUp, PendingIntent.getService(this, 0, new Intent(getApplicationContext(), ServiceNotification.class), 0));
        } else {
            logsCollector.add("* Service wake up : false");
        }

        setUp_ShowLogs();
    }

    private void notificationLogin(Context context,Intent intent) {

        Boolean sercureFlag=false;
        Boolean internetConnectionLive = checkForInternetConnection();
        Boolean internetConnection = haveNetworkConnection();

        sharedEdit = PreferenceManager.getDefaultSharedPreferences(this).edit();

        if (nrTimesNotficationAppear<=nrTimesNotficationShouldAppear) {
             sercureFlag = true;
        }

        logsCollector.add("* PushFlag : "+pushFlag);
        logsCollector.add("* SecureFlag : "+sercureFlag +" ("+nrTimesNotficationAppear+"<="+nrTimesNotficationShouldAppear+")");
        logsCollector.add("* InternetConection : "+internetConnection);
        logsCollector.add("* InternetConnectionLive : "+internetConnectionLive+" ("+typeInternetLive+")");
        logsCollector.add("*");

        if (internetConnection) {
            if (pushFlag && internetConnectionLive && sercureFlag) {
                // all good normal shit
                logsCollector.add("* Notification send : true");
                logsCollector.add("* Next wake up set : "+millisSecondsDelay+" (millis)");
                nextWakeUp = millisSecondsDelay;

                nrTimesNotficationAppear++;
                sharedEdit.putInt(getResources().getString(R.string.pref_key_timeNotificationAppears), nrTimesNotficationAppear);
                sharedEdit.apply();

                sendPushNotification(context, intent);
            } else if (pushFlag && !internetConnectionLive && sercureFlag) {
                // all good but not LiveInternet
                logsCollector.add("* Notification send : false");
                logsCollector.add("* Next wake up set : "+millisSecondsAttempts+" (millis)");
                nextWakeUp = millisSecondsAttempts;
            } else {
                // more stuff not good : Shut Down Service
                logsCollector.add("* Notification send : false");
                sharedEdit.putBoolean(getResources().getString(R.string.push_notification_flag), false);
                sharedEdit.putBoolean(getResources().getString(R.string.push_notification_idle), false);
                pushFlag=false;
                sharedEdit.apply();
            }
        } else {
            logsCollector.add("* Notification send : false (will wakeUp by broadcast)");
            sharedEdit.putBoolean(getResources().getString(R.string.push_notification_flag), false);
            sharedEdit.putBoolean(getResources().getString(R.string.push_notification_idle), true);
            pushFlag=false;
            sharedEdit.apply();
        }
        stopSelf();
    }

    private boolean checkForInternetConnection() {
        Boolean isInternetConnection;
        Call<Void> mCall = ServiceWs.getInstance(this).getInterface().getGoogleImages();
        try
        {
            mCall.execute();
            isInternetConnection = true;
        } catch (IOException e) {
            isInternetConnection = false;
        }
        return isInternetConnection;
    }

    private void sendPushNotification(Context context,Intent myIntent) {

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, myIntent, 0);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(notificationIcon)
                .setContentTitle(notificationTitle)
                .setWhen(System.currentTimeMillis())
                .setContentText(notificationSubtitle)
                .setAutoCancel(true);

        mBuilder.setContentIntent(contentIntent);
        Notification note = mBuilder.build();
        note.defaults |= Notification.DEFAULT_LIGHTS;

        mNotificationManager.notify(12345, note);
    }

    private void getSettingsFromPref()  {

        try {
            SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(this);

            nrTimesNotficationAppear = shared.getInt(getResources().getString(R.string.pref_key_timeNotificationAppears),1);
            nrTimesNotficationShouldAppear = shared.getInt(getResources().getString(R.string.pref_key_nrTimes),DEFAULT_DELAY_NR_TIMES);
            millisSecondsDelay = shared.getInt(getResources().getString(R.string.pref_key_millisSecondsDelayNotification), DEFAULT_DELAY_BETWEEN_NOTIFICATIONS);
            millisSecondsAttempts = shared.getInt(getResources().getString(R.string.pref_key_millisSecondsDelayAttempts), DEFAULT_DELAY_BETWEEN_ATTEMPTS_INTERNET_NOT_SURE);
            nrTimes = shared.getInt(getResources().getString(R.string.pref_key_nrTimes), DEFAULT_DELAY_NR_TIMES);
            debugMode = shared.getBoolean(getResources().getString(R.string.pref_key_debug), DEFAULT_DEBUG_MODE);
            onTapIntent = Intent.getIntent(shared.getString(getResources().getString(R.string.pref_key_tapOnIntent), ""));
            pushFlag=shared.getBoolean(getResources().getString(R.string.push_notification_flag), false);


            notificationTitle = shared.getString(getResources().getString(R.string.pref_key_notification_title),getString(R.string.natificationRateTitle));
            notificationSubtitle = shared.getString(getResources().getString(R.string.pref_key_notification_subtitle),getString(R.string.natificationRateTitle));
            notificationIcon = shared.getInt(getResources().getString(R.string.pref_key_notification_icon),R.drawable.ic_launcher);


        } catch (Exception e) {}
    }

    /**
     * if "debugMode" == "true" print the collected logs
     */
    private void setUp_ShowLogs() {
        if (debugMode) {
            for (String log : logsCollector) {
                Log.d("debugMode_NotifyRating", "" + log);
            }
            logsCollector.add("*");
        }
    }

    private boolean haveNetworkConnection() {
        boolean haveConnectedWifi = false;
        boolean haveConnectedMobile = false;

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo[] netInfo = cm.getAllNetworkInfo();
        for (NetworkInfo ni : netInfo) {
            if (ni.getTypeName().equalsIgnoreCase("WIFI"))
                if (ni.isConnected())
                    haveConnectedWifi = true;
                    typeInternetLive = "wifi";
            if (ni.getTypeName().equalsIgnoreCase("MOBILE"))
                if (ni.isConnected())
                    haveConnectedMobile = true;
                    typeInternetLive = "mobile";
        }
        return haveConnectedWifi || haveConnectedMobile;
    }
}
