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
import com.example.tj_notifyrating.utils.Stuff;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import static com.example.tj_notifyrating.utils.Constants.DEFAULT_DEBUG_MODE;
import static com.example.tj_notifyrating.utils.Constants.DEFAULT_DELAY_BETWEEN_ATTEMPTS_INTERNET_NOT_SURE;
import static com.example.tj_notifyrating.utils.Constants.DEFAULT_DELAY_BETWEEN_NOTIFICATIONS;
import static com.example.tj_notifyrating.utils.Constants.DEFAULT_NR_OF_NOTIFICATIONS;

public class ServiceNotification extends Service {

    /* number of times notification was send */
    private int nrTimesNotficationAppear;
    /* number of time notification should appear one after one by millisSecondsDelay */
    private int nrOfNotifications;
    /* milliseconds delay between onCreate() time and first notification*/
    private int millisSecondsDelay;
    /* milliseconds delay between new attempts when internet connection is not sure*/
    private int millisSecondsAttempts;
    /* if set to "true" Log.d will print */
    private boolean debugMode;
    /* if is set to "false" => no notification and no service alarm*/
    private Boolean pushFlag;
    /* waiting to start ....*/
    private Intent onNotificationTapIntent;
    /* ArrayList that will colect all logs and prind at the end */
    /* utils */
    private Stuff myStuff;

    private String notificationTitle,notificationSubtitle;
    private int notificationIcon;

    private int nextWakeUp;
    private String typeInternetLive = "none";

    private ArrayList<String> logsCollector;
    SharedPreferences.Editor sharedEdit;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        sharedEdit = PreferenceManager.getDefaultSharedPreferences(this).edit();
        myStuff = new Stuff();

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
                    notificationLogic(ServiceNotification.this,onNotificationTapIntent);
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

            logsCollector.add("* Service wake up : true | Next wake up in : "+nextWakeUp+" millis | "+myStuff.millisToTime(nextWakeUp)+"  | "+myStuff.convertDate(((System.currentTimeMillis()+nextWakeUp))));

            sharedEdit.putLong(getResources().getString(R.string.push_notification_wakeup_time), System.currentTimeMillis()+nextWakeUp);
            sharedEdit.commit();


            ((AlarmManager) getSystemService(Context.ALARM_SERVICE)).set(0, System.currentTimeMillis() + nextWakeUp, PendingIntent.getService(this, 0, new Intent(getApplicationContext(), ServiceNotification.class), 0));
        } else {
            logsCollector.add("* Service wake up : false");
        }

        myStuff.showLogs(debugMode,logsCollector);
    }

    private void notificationLogic(Context context,Intent intent) {

        Boolean sercureFlag=false;
        Boolean internetConnectionLive = myStuff.checkForInternetConnection(this);
        Boolean internetConnection = myStuff.haveNetworkConnection(this);

        if (nrTimesNotficationAppear<=nrOfNotifications) {
             sercureFlag = true;
        }

        logsCollector.add("* PushFlag : "+pushFlag);
        logsCollector.add("* SecureFlag : "+sercureFlag +" ("+nrTimesNotficationAppear+"<="+nrOfNotifications+")");
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
            nrOfNotifications = shared.getInt(getResources().getString(R.string.pref_key_nrTimes),DEFAULT_NR_OF_NOTIFICATIONS);
            millisSecondsDelay = shared.getInt(getResources().getString(R.string.pref_key_millisSecondsDelayNotification), DEFAULT_DELAY_BETWEEN_NOTIFICATIONS);
            millisSecondsAttempts = shared.getInt(getResources().getString(R.string.pref_key_millisSecondsDelayAttempts), DEFAULT_DELAY_BETWEEN_ATTEMPTS_INTERNET_NOT_SURE);
            debugMode = shared.getBoolean(getResources().getString(R.string.pref_key_debug), DEFAULT_DEBUG_MODE);
            onNotificationTapIntent = Intent.getIntent(shared.getString(getResources().getString(R.string.pref_key_tapOnIntent), ""));
            pushFlag=shared.getBoolean(getResources().getString(R.string.push_notification_flag), false);

            notificationTitle = shared.getString(getResources().getString(R.string.pref_key_notification_title),getString(R.string.natificationRateTitle));
            notificationSubtitle = shared.getString(getResources().getString(R.string.pref_key_notification_subtitle),getString(R.string.natificationRateTitle));
            notificationIcon = shared.getInt(getResources().getString(R.string.pref_key_notification_icon),R.drawable.ic_launcher);
        } catch (Exception e) {}
    }


}
