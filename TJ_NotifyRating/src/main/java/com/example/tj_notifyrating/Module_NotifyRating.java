package com.example.tj_notifyrating;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import static com.example.tj_notifyrating.Constants.DEFAULT_DEBUG_MODE;
import static com.example.tj_notifyrating.Constants.DEFAULT_DELAY_BETWEEN_ATTEMPTS_INTERNET_NOT_SURE;
import static com.example.tj_notifyrating.Constants.DEFAULT_DELAY_BETWEEN_NOTIFICATIONS;
import static com.example.tj_notifyrating.Constants.DEFAULT_DELAY_NR_TIMES;

public class Module_NotifyRating {

    /* notification Title */
    private String notificationTitle;
    /* notification Subtitle */
    private String notificationSubtitle;
    /* Name of the class that should be open when user tap on notification */
    private Class<?> className;
    /* milliseconds delay between onCreate() time and first notification*/
    private int millisSecondsDelay = DEFAULT_DELAY_BETWEEN_NOTIFICATIONS;
    /* milliseconds delay between new attempts when internet connection is not sure*/
    private int millisSecondsAttempts = DEFAULT_DELAY_BETWEEN_ATTEMPTS_INTERNET_NOT_SURE;
    /* number of times of notifications */
    private int nrTimes = DEFAULT_DELAY_NR_TIMES;
    /* if set to "true" Log.d will print */
    private Boolean debugMode = DEFAULT_DEBUG_MODE;
    /* The intent for tap on notification action */
    private Intent onTapIntent;
    /* Contains the prefered flags for the onTapIntent; If this is null ...default flags will be set */
    private ArrayList<Integer> intentFlags = null;
    /* Needed for context/activity */
    private Activity activity;
    /* ArrayList that will colect all logs and prind at the end */
    private ArrayList<String> logsCollector;
    /* Name of the ServiceWs */
    private Class<?> serviceName = ServiceNotification.class;
    /* If set, libray will search for all apps that contains given pakageName and if more than 1 are find the Service will never run on this instance*/
    private String packageName = "";
    /* SharePref */
    private SharedPreferences shared;
    private SharedPreferences.Editor sharedEdit;

    /**
     * CONSTRUCTOR_CLASS
     *
     * @param activity       - activity
     * @param disableService - dislable service
     */

    public Module_NotifyRating(Activity activity, Boolean disableService) {
        this.activity = activity;

        shared = PreferenceManager.getDefaultSharedPreferences(activity);
        sharedEdit = PreferenceManager.getDefaultSharedPreferences(activity).edit();

        if (!disableService) {
            disableNotification();
        }
    }

    /**
     * CONSTRUCTOR_CLASS
     *
     * @param activity  - activity
     * @param className - activity name that should be open when user tap on notification
     */
    @SuppressWarnings("unused")
    public Module_NotifyRating(Activity activity, Class<?> className, String packageName) {
        this.activity = activity;
        this.className = className;

        if (packageName != null) {
            this.packageName = packageName;
        }

        shared = PreferenceManager.getDefaultSharedPreferences(activity);
        sharedEdit = PreferenceManager.getDefaultSharedPreferences(activity).edit();
    }

    @SuppressWarnings("unused")
    public Module_NotifyRating set_TextAndIcon(String title, String subtitle, int icon) {

        this.notificationTitle = title;
        this.notificationSubtitle = subtitle;

        sharedEdit.putString(activity.getResources().getString(R.string.pref_key_notification_title), title);
        sharedEdit.putString(activity.getResources().getString(R.string.pref_key_notification_subtitle), subtitle);
        sharedEdit.putInt(activity.getResources().getString(R.string.pref_key_notification_icon), icon);
        sharedEdit.apply();
        return this;
    }

    /**
     * SETTER METHOD
     *
     * @param millisSecondsDelay - delay until notification should appear
     * @param nrTimes            - number of times notification should be send ( hoursDelay between them )
     */
    @SuppressWarnings("unused")
    public Module_NotifyRating set_HoursAndRepeateTimes(int millisSecondsDelay, int nrTimes, int millisSecondsAttempts) {
        this.millisSecondsDelay = millisSecondsDelay;
        this.nrTimes = nrTimes;
        this.millisSecondsAttempts = millisSecondsAttempts;
        return this;
    }

    /**
     * SETTER METHOD
     *
     * @param newIntentFlag - arrayList of new flags to be set for the intent that opens application when tapping notification
     */
    @SuppressWarnings("unused")
    public Module_NotifyRating set_NewFlagsForOnTapIntent(ArrayList<Integer> newIntentFlag) {
        this.intentFlags = newIntentFlag;
        return this;
    }

    /**
     * SETTER METHOD
     *
     * @param debugMode - to show logs
     */
    @SuppressWarnings("unused")
    public Module_NotifyRating set_DebugMode(Boolean debugMode) {
        this.debugMode = debugMode;
        return this;
    }

    /**
     * By this method all starts
     */
    @SuppressLint("CommitPrefEdits")
    public void start() {

        logsCollector = new ArrayList<>();
        logsCollector.add("**********************Start Module Rating**********************");

        if (setUp_searchForFlavors()) {
            logsCollector.add("*MORE THAN ONE FLAVORS ; NO SERVICE ; CLOSE");
            setUp_ShowLogs();
            return;
        }

        shared = PreferenceManager.getDefaultSharedPreferences(activity);
        sharedEdit = PreferenceManager.getDefaultSharedPreferences(activity).edit();


        logsCollector.add("*");
        logsCollector.add("* Service Status [ON(true)/OFF(false)] : " + stateOfNotification());

        if (!wasServiceStarted()) {
            logsCollector.add("* Service Info : was not previously initialized ");
            logsCollector.add("*");
            logsCollector.add("* SetUp :");
            setUp_IntentOfNotification();
            setUp_StartService();
            setUp_SaveSettingsInPref();
            setUp_FlagServiceStarted();
            setUp_ShowVariables();
        } else {
            logsCollector.add("* Service Info :  has been previously initialized");
            logsCollector.add("*");
        }
        setUp_ShowLogs();
    }

    private boolean setUp_searchForFlavors() {

        if (packageName.length() < 3) {
            logsCollector.add("*");
            logsCollector.add("* No packageName provided ; Service will run : true");
            return false;
        }

        int flavorsCount = 0;
        PackageManager manager = activity.getPackageManager();

        Intent i = new Intent(Intent.ACTION_MAIN, null);
        i.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> availableActivities = manager.queryIntentActivities(i, 0);
        for (ResolveInfo ri : availableActivities) {
            if (ri.activityInfo.packageName.contains(packageName)) {
                flavorsCount++;
            }
        }

        if (flavorsCount < 2) {
            logsCollector.add("*");
            logsCollector.add("* Less than 2 flavors for entered packageNme("+packageName+"); Service will run : true");
            return false;
        } else {
            logsCollector.add("*");
            logsCollector.add("* Mote than 1 flavors for entered packageNme("+packageName+") ; Service will run : false");
            return true;
        }
    }

    private void setUp_ShowVariables() {
        logsCollector.add("*");
        logsCollector.add("* Variables : ");
        logsCollector.add("* Delay Between any 2 notifications : " + millisSecondsDelay + " (millis)");
        logsCollector.add("* Delay Between attempts internetNoSure : " + millisSecondsAttempts + " (millis)");
        logsCollector.add("* Number of notification to be set : " + nrTimes);
        logsCollector.add("* Notification Title : " + shared.getString(activity.getResources().getString(R.string.pref_key_notification_title), activity.getResources().getString(R.string.natificationRateTitle)));
        logsCollector.add("* Notification Subtitle : " + shared.getString(activity.getResources().getString(R.string.pref_key_notification_subtitle), activity.getResources().getString(R.string.natificationRateMessage)));
        logsCollector.add("*");
    }

    private void setUp_SaveSettingsInPref() {
        sharedEdit.putString(activity.getResources().getString(R.string.pref_key_tapOnIntent), onTapIntent.toURI());
        sharedEdit.putInt(activity.getResources().getString(R.string.pref_key_millisSecondsDelayNotification), millisSecondsDelay);
        sharedEdit.putInt(activity.getResources().getString(R.string.pref_key_millisSecondsDelayAttempts), millisSecondsAttempts);
        sharedEdit.putInt(activity.getResources().getString(R.string.pref_key_nrTimes), nrTimes);
        sharedEdit.putBoolean(activity.getResources().getString(R.string.pref_key_debug), debugMode);
        sharedEdit.apply();

        logsCollector.add("* 3) Save Settings in Pref");
    }

    /**
     * Prepare and set the intent for on tap notification
     */
    private void setUp_IntentOfNotification() {

        if (intentFlags == null) {
            intentFlags = new ArrayList<>();
            intentFlags.add(Intent.FLAG_ACTIVITY_NEW_TASK);
            intentFlags.add(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intentFlags.add(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intentFlags.add(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            logsCollector.add("* 1) Set onTapNotificationIntent flags : Default (" + intentFlags.size() + " flags)");
        } else {
            logsCollector.add("* 1) Set onTapNotificationIntent flags : Custom (" + intentFlags.size() + " flags)");
        }

        onTapIntent = new Intent();
        onTapIntent.setClass(activity, className);
        for (Integer flag : intentFlags) {
            onTapIntent.setFlags(flag);
        }
        onTapIntent.putExtra(activity.getResources().getString(R.string.intent_key_notification), "click");
    }

    /**
     * Prepare and start the service that will sleep and wake up to send notification
     */
    private void setUp_StartService() {
        Intent i = new Intent(activity, serviceName);
        i.putExtra(activity.getResources().getString(R.string.intent_key_intent), onTapIntent);
        i.putExtra(activity.getResources().getString(R.string.intent_key_first_launch), "yes");
        activity.startService(i);

        logsCollector.add("* 2) Start Service");
    }


    /**
     * Save to pref that ServiceWs was started
     */
    private void setUp_FlagServiceStarted() {
        SharedPreferences.Editor shared = PreferenceManager.getDefaultSharedPreferences(activity).edit();
        shared.putBoolean(activity.getResources().getString(R.string.push_notification_was_service_started), true);
        shared.putBoolean(activity.getResources().getString(R.string.push_notification_flag), true);
        shared.apply();
    }

    /**
     * Save to pref that :
     * - no future notification should be send
     * - ServiceWs should be stopped when possible
     */
    private void disableNotification() {
        sharedEdit.putBoolean(activity.getResources().getString(R.string.push_notification_flag), false);
        sharedEdit.putBoolean(activity.getResources().getString(R.string.push_notification_idle), false);
        sharedEdit.apply();
    }

    /**
     * Check if service was started to prevet future startUps
     *
     * @return ture if was started
     */
    private boolean wasServiceStarted() {
        return shared.getBoolean(activity.getResources().getString(R.string.push_notification_was_service_started), false);
    }

    /**
     * Check if notification service is ON(false)/OFF(true)
     *
     * @return ture if was started
     */
    private boolean stateOfNotification() {
        return shared.getBoolean(activity.getResources().getString(R.string.push_notification_flag), true);
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
}
