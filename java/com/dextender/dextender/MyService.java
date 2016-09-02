package com.dextender.dextender;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;


//=================================================================================================
// Class   : MyService
// Author  : Mike LiVolsi
// Date    : Jan 2014 - re-written Jun 2015 (Using dexcom follow, so no need for toggling between master/slave)
//
// Credits : How to create a service: http://www.youtube.com/watch?v=GAOH7XTW7BU
//           Battery Saver          : http://it-ride.blogspot.com/2010/10/android-implementing-notification.html
//
// Notes   : There are 3 (currently) main components to this project.
//            1) The gui front end (what the user sees)
//            2) The back end (ie. services)
//            3) The widget
//           Here is how they are related:
//           - When you click the icon on your phone for "dExtender", it will spawn the services
//           - The "service" grabs and stores info in our local SQLite database. The information
//             in the database is what's displayed on the gui screen (notably , the BG levels and logs)
//             Screen 2 (aka frag2)
//           - The widget is like "frag1", and all it's doing is grabbing some preference info
//             and data from the database to display
//
// Notes:    - See MyReceiver.java for timer updates
//
// Proposed Enhancements: The alarm can get the value from the dexcom and then adjust it's
//                        'next' time closer to when the dex gets invoked
//=================================================================================================
public class MyService extends IntentService {

    final short SERVICE_OFF=-1;
    final short SERVICE_ON=0;
    final short SERVICE_ERROR=1;
    final short SERVICE_UNKNOWN=2;
    final short SERVICE_PAUSE=11;


    final short LOG_LEVEL_ERROR=1;   // red button
    final short LOG_LEVEL_LOW=2;     // down arrow
    final short LOG_LEVEL_HIGH=3;    // high arrow
    final short LOG_LEVEL_INFO=4;    // green button
    final short LOG_LEVEL_WARNING=5; // yellow button
    final short LOG_LEVEL_CRITICAL_LOW=6;


    final short MAX_BG_RECORDS=288;


    public MyService() {
        super("MyService");
    }


    //---------------------------------------------------
    // Class wide values
    //---------------------------------------------------

    @Override
    public IBinder onBind(Intent arg0){
        return null;
    }

    protected void onHandleIntent(Intent intent) {

        //---------------------------------------------------------------------------------
        // check the global background data setting and see if we have network connectivity
        // NOTE: Other calls have been depracated. This is using the new call.
        //---------------------------------------------------------------------------------
        MyTools    tools        = new MyTools();                                                    // Generic tools class
        MyDatabase myDb         = new MyDatabase(this);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        short      connectFlag;                                                                     // 1=on 0=off -1=internet err
        String     logMsg       = "";

        if(prefs.getBoolean("prefsvc", false)) {
            if (tools.isNetworkAvailable(getApplicationContext())) {
                if (!tools.internetConnectionAvailable(2000)) {
                    // Try a second time
                    if (!tools.internetConnectionAvailable(2000)) {
                        logMsg = "Internet connection is not stable";
                        connectFlag = -1;
                    }
                    else {
                        connectFlag=1;
                    }
                }
                else {
                    connectFlag=1;
                }
            } else {
                logMsg = "Please check your network settings";
                connectFlag = 0;
            }

            //----------------------------------------------------------------------------------
            // Checking connections to the internet seems flaky no matter what method I choose,
            // so I can't rely on it to make a determination of my internet connection. So instead
            // of quiting altogether, I warn the user of what MAY possibly be wrong if they don't
            // get any values
            //----------------------------------------------------------------------------------
            if (connectFlag != 1) {
                try {
                    myDb.open();
                    myDb.logIt(LOG_LEVEL_WARNING, logMsg);
                    if (!prefs.getBoolean("pref_netIgnore", true)) {
                        soundAlarm(prefs.getString("networkErrorAlarmTone", null), false, false, Integer.parseInt(prefs.getString("pref_maxPlayTime", "5")), false, false, null, null, false, null);
                    }
                    if (connectFlag == 0) {
                        myDb.updateServiceStatus("Internet Connection", SERVICE_ERROR);
                        myDb.updateServiceStatus("Web Handler", SERVICE_UNKNOWN);// aka cloud server
                    } else {
                        myDb.updateServiceStatus("Internet Connection", SERVICE_PAUSE);
                        myDb.updateServiceStatus("Web Handler", SERVICE_UNKNOWN);// aka cloud server

                    }
                    myDb.updateServiceStatus("Background Service", SERVICE_UNKNOWN);
                    myDb.updateServiceStatus("Cloud Account", SERVICE_UNKNOWN);
                    myDb.close();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (connectFlag == 0) {                          // There is no hope.. just quit
                setAlarm(5,0);                               // try again in 5 minutes
                stopSelf();
                return;
            }
        }

        //-------------------------------------------------------------
        // Meat and potatoes time. Do the work in a separate thread !
        // See comments in the PollTask class
        //-------------------------------------------------------------
        new PollTask().execute();
        MyReceiver.completeWakefulIntent(intent);
    }

    private class PollTask extends AsyncTask<Void, Void, Void> {

        //--------------------------------------------------------------------
        // This is where the work is done.
        // All database calls, httpd posts, etc.. are done here
        // This work is all done in a seperate thread (which is good)
        //--------------------------------------------------------------------
        @Override
        protected Void doInBackground(Void... params) {

            PowerManager.WakeLock mWakeLock;

            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "dexTag");                   // Obtain the wakelock
            mWakeLock.acquire();

            //----------------------------------------------------------------
            // Call all our different classes
            //----------------------------------------------------------------
            MyHttpPost http         = new MyHttpPost();                                             // Call the httpd class
            MyDatabase myDb         = new MyDatabase(getBaseContext());                             // call the Database class
            MyTools    tools        = new MyTools();                                                // Generic tools class
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

            //----------------------------------------------------------------
            // set some variables that will be used throughout this class
            //----------------------------------------------------------------
            Boolean Rc;                                                                             // we will use this to control flow of logic
            int recordsRead=0;
            int previousBgValue=0;
            int lastBgValue=0;
            int lastTrendValue=0;
            int newHighLimit=0;
            int newLowLimit=0;

            String[] timeArrayStr  = new String[MAX_BG_RECORDS];                                               // 12 readings in an hour 24 hours in a day
            String[] bgArrayStr    = new String[MAX_BG_RECORDS];
            String[] trendArrayStr = new String[MAX_BG_RECORDS];

            String[] message       = new String[1];

            boolean alarmFlag=false;
            boolean safeTrend=true;                                                                 // used with smart alarming. If we get a trend that isn't level, then do not hibernate

            //-------------------------------------------------
            // These values are also in myDb and MyAlarm
            // alarmLevel gets set
            //-------------------------------------------------
            short alarmLevel=0;
            final short ALL_ALARMS = 0;
            final short CRITICAL_LOW_ALARM=1;
            final short LOW_ALARM = 2;
            final short HIGH_ALARM = 3;
            final short SUSTAINED_ALARM = 4;
            final short DATA_ERROR_ALARM=5;    // If there is no data coming in from the network
            final short TARGET_HIGH_ALARM=6;
            final short TARGET_LOW_ALARM=7;
            final short TREND_LOW_ALARM=8;
            final short TREND_HIGH_ALARM=9;
            final short DATA_ERROR_ALARM2=10;  // if the data from the database is all screwed up



            final short MAX_PROJECTIONS = 6;  // No way we are going to project for more than 30 minutes
            final short MAX_LOOKBACK    = 12; // how many readings back do we use as a sample for projecting bg numbers

            //---------------------------
            String accountId;
            String subscriptionId;
            String applicationId;
            String accountPwd;

            int fuzzySeconds=-2;                                                                    // used for alarming

            //---------------------------
            // Get our preferences
            //--------------------------
            // Log.d("Service", "In service var declare");

            boolean pref_serviceMode        = prefs.getBoolean("prefsvc", false);                                    // should the service do anything ?

            int pref_warnHigh               = Integer.parseInt(prefs.getString("listHigh", "180"));                      // Get the prefs as an int
            int pref_warnLow                = Integer.parseInt(prefs.getString("listLow", "70"));                        // Get the prefs as an int
            int pref_hardLow                = Integer.parseInt(prefs.getString("listHardLow", "60"));                    // Get the prefs as an int

            int pref_highTarget             = Integer.parseInt(prefs.getString("followListHigh", "180"));                // Get the prefs as an int
            int pref_lowTarget              = Integer.parseInt(prefs.getString("followListLow", "70"));                  // Get the prefs as an int
            int lowSnoozeTime               = Integer.parseInt(prefs.getString("pref_lowSnoozeElapse", "5"))*60;         // Number of minutes to snooze, convert to seconds
            int highSnoozeTime              = Integer.parseInt( prefs.getString("pref_highSnoozeElapse", "5"))*60;       // Number of minutes to snooze, convert to seconds
            int trendSnoozeTime             = Integer.parseInt( prefs.getString("pref_trendSnoozeElapse", "5"))*60;      // Number of minutes to snooze, convert to seconds
            int dataErrSnoozeTimeInSecs     = 300;                                                                       // Number of minutes to snooze, convert to seconds

            int pref_sustained              = Integer.parseInt(prefs.getString("sustainedListHigh", "140"));
            int warnSustainedTime           = Integer.parseInt(prefs.getString("sustainedTimeHigh","45"))*60;

            int pref_highAlarmCount         = Integer.parseInt(prefs.getString("pref_highAlarmCount", "3"));
            int pref_refreshInterval        = Integer.parseInt(prefs.getString("pref_refresh_interval", "5"));

            final boolean pref_smartRefresh       = prefs.getBoolean("pref_smart_refresh", false);                      // do we want to use smart refresh ?
            final boolean pref_sustainedHighFlag  = prefs.getBoolean("pref_smartLimit", false);                         // do we want to use the sustained high alarm ?
            final boolean pref_trendLow           = prefs.getBoolean("pref_trendLow", false);
            final boolean pref_trendHigh          = prefs.getBoolean("pref_trendHigh", false);
            final boolean pref_startTrailingHigh  = prefs.getBoolean("pref_startTrailingHigh", false);
            final boolean pref_startTrailingLow   = prefs.getBoolean("pref_startTrailingLow", false);
            final boolean pref_hardLowBehavior    = prefs.getBoolean("pref_hardLowBehavior", false);      // Get the prefs as an bool


            int pref_predictSampleCount=0;
            int pref_predictLookAhead=0;
            if(prefs.getBoolean("pref_predictive", false)) {
                pref_predictSampleCount = Integer.parseInt(prefs.getString("pref_predictiveSampleCount", "0"));
                pref_predictLookAhead = Integer.parseInt(prefs.getString("pref_predictiveLookAhead", "0"));
            }

            final String hardLowSoundFile         = prefs.getString("pref_hardLowAlarmTone", null);       // get the low sound file
            final String lowSoundFile             = prefs.getString("lowAlarmTone", null);                // get the low sound file
            final String highSoundFile            = prefs.getString("highAlarmTone", null);               // get the high sound file
            final String shighSoundFile           = prefs.getString("sustainedAlarmTone", null);          // get the sustained high alarm sound
            final String dataErrSoundFile         = prefs.getString("dataErrAlarmTone", null);            // data error tone
            final String autoOffSoundFile         = prefs.getString("autoOffAlarmTone", null);            // data error tone
            final String trailingHighAlarmTone    = prefs.getString("trailingHighAlarmTone", null);       // Did the high threshold change
            final String trailingLowAlarmTone     = prefs.getString("trailingLowAlarmTone", null);        // Did the high threshold change
            final String lowTrendAlarmTone        = prefs.getString("lowTrendAlarmTone", null);           // Did the high threshold change
            final String highTrendAlarmTone       = prefs.getString("highTrendAlarmTone", null);          // Did the high threshold change
            final boolean vibrate                 = prefs.getBoolean("pref_alert_vibrate", true);
            final boolean override                = prefs.getBoolean("pref_alert_override", true);
            final boolean annoying                = prefs.getBoolean("pref_annoying", false);
            final int maxPlayTime                 = Integer.parseInt(prefs.getString("pref_maxPlayTime", "5"));

            //==========================
            // TTS Prefs
            //==========================
            boolean pref_tts                = prefs.getBoolean("pref_tts", false);
            boolean pref_tts_muzzle         = prefs.getBoolean("pref_tts_muzzle", false);
            boolean pref_ttsPolite          = prefs.getBoolean("pref_tts_polite", false);

            //==========================
            // Razer Pref
            //==========================
            boolean pref_razerNotifFmt      = prefs.getBoolean("pref_razer_format", false);             // format notifications for the razer nabu
            boolean pref_razerCriticalNotif = prefs.getBoolean("pref_razer_critical", false);       // If you choose, this, you can set the band to vibrate..

            //-----------------------------------------------------------------
            // Open the database, because we will need it.
            //-----------------------------------------------------------------
            boolean dbRc=false;
            try {
                myDb.open();
                dbRc=true;
            } catch (Exception e) {
                e.printStackTrace();
            }
            //--------------------------------------------------------------------------
            // Are we authorized ?
            // Commented out below. To be used at a later date
            //--------------------------------------------------------------------------
            //boolean authorizedUser=false;
            //String tempRec[]=myDb.getAccountInfo().split("\\|");
            //if(tempRec.length == 4) {
            //    if ( (tempRec[3].equals("Active")) || (tempRec[3].equals("Registered-active")) ){
            //        authorizedUser=true;
            //    }
            //}

            //--------------------------------------------------------------------------
            // TEST
            // Add stuff here if you want to test the service
            //--------------------------------------------------------------------------
            //soundAlarm(lowSoundFile, vibrate, override, 4, true, 100, 4, false, null);
            //criticalAlarm(50, CRITICAL_LOW_ALARM);
            // Log.d("My Service", "RUNNING");


            //======================================================================================
            // Our SERVICE is UP (toggle switch)
            // Our DATABASE is GOOD (we have a connection)
            // We  are AUTHORIZED on Dextender.com
            //======================================================================================
            //if( (pref_serviceMode) && (dbRc) && (authorizedUser) ) {                              // yes, do something

            if( (pref_serviceMode) && (dbRc) ) {                                                    // yes, do something

                myDb.updateServiceStatus("Background Service", SERVICE_ON);                         // now that the DB is open, I can update the service

                //---------------------------------------------------------
                // Get the alarm information from the last run
                // This will let us know when to sleep, and how many times
                // NOTE: A low under 50 gets alarmed every run !!
                //---------------------------------------------------------
                String[]  tmpStringArr = new String[1];
                String[]  tmpStringArr2 = new String[1];
                Integer[] tmpIntArr   = new Integer[1];

                myDb.getAlarm(ALL_ALARMS, tmpStringArr, tmpIntArr, tmpStringArr2);
                Long lastAlarmTime= Long.parseLong(tmpStringArr[0]);
                //Long firstAllAlarmTime = Long.parseLong(tmpStringArr2[0]);

                myDb.getAlarm(LOW_ALARM, tmpStringArr, tmpIntArr, tmpStringArr2);
                Long lastLowAlarmTime= Long.parseLong(tmpStringArr[0]);
                //Long firstLowAlarmTime = Long.parseLong(tmpStringArr2[0]);

                myDb.getAlarm(HIGH_ALARM, tmpStringArr, tmpIntArr, tmpStringArr2);
                Long lastHighAlarmTime=Long.parseLong(tmpStringArr[0]);
                //Long firstHighAlarmTime = Long.parseLong(tmpStringArr2[0]);
                int highBgAlarmCount = tmpIntArr[0];

                myDb.getAlarm(SUSTAINED_ALARM, tmpStringArr, tmpIntArr, tmpStringArr2);
                Long lastSustainedTime=Long.parseLong(tmpStringArr[0]);
                //Long firstSustainedAlarmTime = Long.parseLong(tmpStringArr2[0]);

                myDb.getAlarm(DATA_ERROR_ALARM, tmpStringArr, tmpIntArr, tmpStringArr2);
                long lastDataErrAlarmTime=Long.parseLong(tmpStringArr[0]);
                Long firstDataErrAlarmTime = Long.parseLong(tmpStringArr2[0]);
                int dataErrAlarmCount = tmpIntArr[0];

                myDb.getAlarm(TREND_LOW_ALARM, tmpStringArr, tmpIntArr, tmpStringArr2);
                Long lastLowTrendAlarmTime= Long.parseLong(tmpStringArr[0]);
                //Long firstLowTrendAlarmTime = Long.parseLong(tmpStringArr2[0]);

                myDb.getAlarm(TREND_HIGH_ALARM, tmpStringArr, tmpIntArr, tmpStringArr2);
                Long lastHighTrendAlarmTime= Long.parseLong(tmpStringArr[0]);
                //Long firstHighTrendAlarmTime = Long.parseLong(tmpStringArr2[0]);

                //==================================================================================
                // Our DEXCOM SHARE (toggle switch) is ON
                //==================================================================================
                if (prefs.getBoolean("pref_cloud", true)) {                                         // we have privs to get the information from the cloud

                    //------------------------------------------------------------------------------
                    // Crap design warning : Dexcom requires 3 calls before you get the
                    // BG information. Like that makes a whole lot of sense *rolling eyes*
                    // First thing: set all the URL strings
                    //------------------------------------------------------------------------------
                    URI webUrl_4bg = null;
                    URI webUrl_4login = null;
                    URI webUrl_4subId = null;
                    //URI webUrl_forwardBg=null;
                    try {
                        webUrl_4bg = new URI(getString(R.string.http_bg_url));
                        webUrl_4login = new URI(getString(R.string.http_login_url));
                        webUrl_4subId = new URI(getString(R.string.http_subId_url));
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                    }


                    //--------------------------------------------------------------
                    // Get records from the web
                    // NOTE: bgMinutes is only used to calculate how many records
                    // request to send to dexcom
                    //--------------------------------------------------------------

                    int lastDexTime = (int) (((System.currentTimeMillis() / 1000) - myDb.getLastDexBgTime() ) / 60); // epoc now - epoc then (convert to minutes)
                    //Log.d("SERVICE", "Difference between dextime and current epoc - " + ((System.currentTimeMillis() / 1000) - myDb.getLastDexBgTime() ));

                    String bgMinutes;                                                               // Minutes back we need to look for bg values
                    lastDexTime = tools.roundUp(lastDexTime, 5) -5;
                    if (lastDexTime <= 5) bgMinutes = "5";
                    else                  bgMinutes = String.valueOf(lastDexTime);


                    //------------------------------------------------------------------------------
                    // WHY THE ABOVE IS A COMPLETE HACK !!
                    // If my last record was 7:00 AM and it's now 7:10, I want records from the last
                    // 10 minutes - But DEXCOM will hand back 3 records | 7:00 7:05 and 7:10
                    // If I say, instead, 9 minutes, then DEXCOM should give me back 7:05 and 7:10
                    // NOTE: Even if we get the same record back, this will cause the 'auto-off' function
                    //       to fail, because we consistently receive 1 record back
                    //------------------------------------------------------------------------------


                    //Log.d("MyService", "BG minutes -->" + bgMinutes);
                    //======================================================
                    // get account information from the database
                    //======================================================
                    String[] accountRec = myDb.getAccountInfo().split("\\|");
                    accountId = accountRec[0];
                    applicationId = accountRec[1];
                    accountPwd = accountRec[2];

                    String[] webResponse;

                    boolean attemptReconnect=false;                                                 // did we attempt to reconnect
                    boolean looper = true;
                    while (looper) {
                        recordsRead = 0;
                        String[] sessionInfo = myDb.getSessionInfo().replaceAll("^\"|\"$", "").split("\\|"); // get last info from DB                               // get the latest session from the database

                        //----------------------------------------------------------------------
                        // DB records are ok - which they should always be.. meaning, they have
                        // info, not that the latest info will be correct
                        //
                        // NOTE: we will attempt to get BG records from the last dexcom session
                        //       ID. If no records, we will re-connect and try again
                        //       if still no records, then abandon ship
                        //----------------------------------------------------------------------
                        if (sessionInfo[0] != null && !sessionInfo[0].isEmpty()) {
                            if (!accountId.equals("0")) {
                                try {

                                    webUrl_4bg = new URI(getString(R.string.http_bg_url) +
                                            "?sessionId="      + sessionInfo[0] +
                                            "&subscriptionId=" + sessionInfo[1] +
                                            "&minutes="        + bgMinutes      +
                                            "&maxCount="       + String.valueOf(MAX_BG_RECORDS)
                                    );
                                    //Log.d("MyService", "URL-->" + webUrl_4bg);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                                webResponse = http.webGetBg(webUrl_4bg, message).split("\\|");
                                //Log.d("MyService", "Response -->" + webResponse[1]);
                                //------------------------------------------
                                // Remember, it will be one giant string
                                //------------------------------------------
                                if (webResponse[0].equals("11000000")) {
                                    if (!webResponse[1].equals("[]")) {
                                        looper = false;                                                 // we got records, so quit while
                                        recordsRead = tools.processCloudBgRecords(webResponse[1], timeArrayStr, bgArrayStr, trendArrayStr);
                                    }
                                    else {                                                          // everything is cool, except..
                                        attemptReconnect=true;                                      // .. there's no data. so just stop
                                    }
                                }
                            }
                        }

                        //------------------------------------------------------------------
                        // Getting the BG records failed. which most likely means our account
                        // info isn't correct - so let's get the ACCOUNT information
                        //------------------------------------------------------------------
                        // Log.d("MyService", "Records read " + recordsRead + " attempReconnect" + attemptReconnect);
                        if( (recordsRead == 0) && (!attemptReconnect)){

                            attemptReconnect=true;                                                  // we only want to try to reconnect once
                            //------------------
                            // Get a session ID
                            //------------------
                            webResponse = http.webLogin2Dex(webUrl_4login, message, accountId, applicationId, accountPwd).split("\\|");  // calling the web with to get the latest BG value from the last sequence we have

                            if (webResponse[0].equals("11000000")) {                                // good response code 0 - we have a sessionID
                                myDb.updateServiceStatus("Web Handler", SERVICE_ON);
                                myDb.updateServiceStatus("Internet Connection", SERVICE_ON);


                                webResponse[1] = webResponse[1].replaceAll("^\"|\"$", "");          // strip any quote shit
                                myDb.updateAccountSession(webResponse[1]);                          // update the database with session ID

                                //-------------------------
                                // Get the subscription ID
                                // A. First , build the URL
                                //-------------------------
                                try {
                                    webUrl_4subId = new URI(getString(R.string.http_subId_url) + "?sessionId=" + webResponse[1]);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                                //-------------------------------------------------------
                                // B. Call the dexcom server with URL and session ID
                                //-------------------------------------------------------
                                webResponse = http.webGetSubscriberId(webUrl_4subId, message).split("\\|");
                                //Log.d("MyService", " webResponse" + webResponse[0] + " ------" + webResponse[1]);
                                if (webResponse[0].equals("11000000")) {
                                    myDb.updateServiceStatus("Cloud Account", SERVICE_ON);
                                    webResponse[1] = webResponse[1].replaceAll("^\"|\"$", "");

                                    //--------------------------------------------------
                                    // C. Take the web response and get the JSON object
                                    //--------------------------------------------------
                                    subscriptionId = "0";

                                    try {
                                        JSONArray jArray = new JSONArray(webResponse[1]);
                                        for (int i = 0; i < jArray.length(); i++) {
                                            JSONObject jObject = jArray.getJSONObject(i);
                                            subscriptionId = jObject.getString("SubscriptionId");
                                        }
                                    } catch (Exception e) {
                                        looper=false;
                                        e.printStackTrace();
                                    }
                                    //------------------------------------------------------
                                    // D. Update our locale database
                                    //    So now, we have the SESSION ID and SUB ID
                                    //------------------------------------------------------
                                    if (!subscriptionId.equals("0")) {
                                        myDb.updateAccountSubscriptionId(subscriptionId);
                                    }
                                }
                                else {
                                    myDb.updateServiceStatus("Cloud Account", SERVICE_ERROR);
                                    myDb.logIt(LOG_LEVEL_ERROR, webResponse[1]);
                                    looper=false;
                                }
                            }
                            else {
                                if (webResponse[0].equals("10000000")) {                        // share is up | account is invalid
                                    myDb.updateServiceStatus("Web Handler", SERVICE_ON);
                                    myDb.updateServiceStatus("Internet Connection", SERVICE_ON);
                                    myDb.updateServiceStatus("Cloud Account", SERVICE_ERROR);
                                    myDb.logIt(LOG_LEVEL_ERROR, "Share is up, account is invalid");
                                    looper=false;
                                } else {
                                    myDb.updateServiceStatus("Cloud Account", SERVICE_UNKNOWN);
                                    if(tools.isNetworkAvailable(getApplicationContext())) {
                                            if (tools.internetConnectionAvailable(1000)) {
                                            //if(tools.isOnline()) {
                                            myDb.logIt(LOG_LEVEL_ERROR, "It appears that share is not responding");
                                        }
                                        else {
                                            myDb.updateServiceStatus("Internet Connection", SERVICE_UNKNOWN);     // Couldn't connect to share
                                            myDb.logIt(LOG_LEVEL_ERROR, "Network is up, but no connectivity to the public Internet");
                                        }
                                    }
                                    else {
                                        myDb.updateServiceStatus("Internet Connection", SERVICE_ERROR);     // Couldn't connect to share
                                        myDb.updateServiceStatus("Web Handler", SERVICE_UNKNOWN);     // Couldn't connect to share
                                        myDb.logIt(LOG_LEVEL_ERROR, "Lost WiFi/Cell connection");
                                    }
                                    looper=false;
                                }
                                myDb.logIt(LOG_LEVEL_ERROR, webResponse[1]);
                            }
                        }
                        else {
                            if (attemptReconnect) looper=false;                                     // we already tried to reconnect once, so quit
                        }
                    }
                    //Log.d("MyService", "We should be here.. records read -->" + recordsRead);
                    //======================================
                    // END WHILE LOOP TO GET BG RECORDS
                    // IF WE HAVE NO RECORDS, SET ALARM
                    // OTHERWISE, WE SHOULD HAVE BG RECORDS
                    //======================================


                    //----------------------------------------------------------------------
                    // TEST
                    // Short cicuit testing
                    //----------------------------------------------------------------------
                    //recordsRead=1;
                    //timeArrayStr[0] = String.valueOf(System.currentTimeMillis()/1000 - 300);
                    //bgArrayStr[0] = "39";
                    //trendArrayStr[0]="4";

                    //--------------------------------------------------------------------------
                    // Take the long string and start processing the records and put into
                    // an array
                    //--------------------------------------------------------------------------
                    if (recordsRead > 0) {

                        myDb.updateServiceStatus("Web Handler",         SERVICE_ON);
                        myDb.updateServiceStatus("Internet Connection", SERVICE_ON);
                        myDb.updateServiceStatus("Cloud Account",       SERVICE_ON);

                        long maxDexBgTime = myDb.getLastDexBgTime();                            // if the new record is "newuser" than what we have
                        for (int i = 0; i < recordsRead; i++) {
                            if ((Long.parseLong(timeArrayStr[i]) > maxDexBgTime) && (Integer.parseInt(bgArrayStr[i]) > 15)) {  // for some reason, if we're doing a reboot, it's showing up as a '5'

                                if (i == (recordsRead - 1)) {
                                    //------------------------------------------------------------------
                                    // Note: no 'else' logic here on purpose
                                    //------------------------------------------------------------------
                                    if (Integer.parseInt(bgArrayStr[i]) <= pref_hardLow) {
                                        alarmFlag = true;
                                        alarmLevel = CRITICAL_LOW_ALARM;
                                    }
                                    //Log.d("MyService", "BG is -->" + bgArrayStr[i] + " and warning level" + pref_warnLow);
                                    if (Integer.parseInt(bgArrayStr[i]) <= pref_warnLow) {                                  // if the bg value is lower than we what we have as our limit
                                        alarmFlag = true;
                                        if (alarmLevel == 0) alarmLevel = LOW_ALARM;
                                    }
                                    if (Integer.parseInt(bgArrayStr[i]) >= pref_warnHigh) {                                 // else, if the bg value is higher than our warning limit
                                        alarmFlag = true;
                                        if (alarmLevel == 0) alarmLevel = HIGH_ALARM;
                                    }
                                    //-----------------------------------------------------------------
                                    // Sustained check - That was cleared in the preference section
                                    // we should set it first upon encoutering a systained high
                                    // and then compare times
                                    //-----------------------------------------------------------------
                                    // If the value of bg > sustained, and we're alarming on sustained
                                    //-----------------------------------------------------------------
                                    //Log.d("service", "last sustained time " + lastSustainedTime);
                                    //Log.d("service", "warn sustained time " + warnSustainedTime);
                                    if (pref_sustainedHighFlag) {                                  // we are monitoring for sustained highs
                                        if ((Integer.parseInt(bgArrayStr[i]) >= pref_sustained)) { // and we are higher than the setting
                                            if (lastSustainedTime == 0) {                          // we've never alarmed before, so start the ball rolling
                                                myDb.updateAlarm(SUSTAINED_ALARM, 1);
                                            } else {
                                                if ((warnSustainedTime + lastSustainedTime + fuzzySeconds) <= (System.currentTimeMillis() / 1000)) {
                                                    alarmFlag = true;
                                                    if (alarmLevel == 0)
                                                        alarmLevel = SUSTAINED_ALARM;
                                                }
                                            }
                                        } else {
                                            if (lastSustainedTime != 0) {                       // we've alarmed, but have fallen below the threshold
                                                myDb.clearAlarm(SUSTAINED_ALARM);
                                            }
                                        }
                                    }

                                    if ((Integer.parseInt(trendArrayStr[i]) <= 2) && (pref_trendLow)) {
                                        alarmFlag = true;
                                        if (alarmLevel == 0) alarmLevel = TREND_LOW_ALARM;
                                    }

                                    if ((Integer.parseInt(trendArrayStr[i]) >= 6) && (pref_trendHigh)) {
                                        alarmFlag = true;
                                        if (alarmLevel == 0) alarmLevel = TREND_HIGH_ALARM;
                                    }


                                    //----------------------------------------------------------
                                    // Data error from database ?
                                    // if BG == 0 || bg == 10 || bg=255)
                                    //----------------------------------------------------------
                                    if ((Integer.parseInt(bgArrayStr[i]) == 0) || (Integer.parseInt(bgArrayStr[i]) == 10) || (Integer.parseInt(bgArrayStr[i]) == 255)) {
                                        alarmFlag = true;
                                        //Log.d("MyService", "In Data error section");
                                        if (alarmLevel == 0) alarmLevel = DATA_ERROR_ALARM2;
                                    }

                                }

                                //--------------------------------------------------------
                                // For every record, insert into our local DB
                                // Insert into bg the sequence | dexdate | bs | trend
                                //--------------------------------------------------------
                                Rc = false;
                                try {
                                    if (lastBgValue != 0) {                                         // If we got a whole bunch of BG's, and we're looping through
                                        previousBgValue = lastBgValue;                              // Then set the last to the current before assigning a new one to curent
                                    } else {
                                        previousBgValue = myDb.getLastBg();                         // else, let's get it from what we recorded the last time we ran
                                    }
                                    //Log.d("Service", "Recording bg value of " + bgArrayStr[i] + " time " + timeArrayStr[i]);
                                    myDb.bgIt(timeArrayStr[i], bgArrayStr[i], trendArrayStr[i]);    // insert into our local database
                                    lastBgValue = Integer.parseInt(bgArrayStr[i]);                  // last bg value - This will be used for various reasons (ie comparison)
                                    lastTrendValue = Integer.parseInt(trendArrayStr[i]);
                                    Rc = true;
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                if (Rc) {
                                    //===========================================================================================================
                                    // We are updating the WEB TRACKER - Which handled where we are on what we received from the web (cloud)
                                    //===========================================================================================================
                                    if ((!pref_razerNotifFmt) ||                  // not for razer
                                            ((pref_razerNotifFmt) && (pref_razerCriticalNotif) && (alarmFlag)) ||    // it is for razer, but only critical and the alarm went off
                                            ((pref_razerNotifFmt) && (!pref_razerCriticalNotif)) &&                  // is is for razer, but we want every one
                                                    (i == (recordsRead - 1))                                         // Don't hit the notification all the time, let's just to send the last one
                                            ) {
                                        updateNotification(Integer.parseInt(bgArrayStr[i]),
                                                Integer.parseInt(trendArrayStr[i]),
                                                tools.epoch2FmtTime(System.currentTimeMillis() / 1000, "hh:mm a"),
                                                pref_razerNotifFmt, true, null); // update with the latest value (last two args are bgNotification (bool) and other string arg
                                    }

                                    //---------------------------------------------------------------------------
                                    // let's see if I can speak. Depending if speech is turned on, I can either
                                    // be muzzled, which means only say something if it's important
                                    // or I'm not muzzled
                                    //---------------------------------------------------------------------------
                                    if ((pref_tts) && (!alarmFlag) && (i == (recordsRead - 1))) {                                                         // don't want to speak over an alarm
                                        if (
                                                ((pref_tts_muzzle) && ((lastBgValue - 15 <= pref_warnLow) || (lastBgValue + 15 >= pref_warnHigh))) ||  // I'm muzzled but I hit the thresholds
                                                        (!pref_tts_muzzle)                                                                                        // I'm not muzzled
                                                ) {

                                            boolean lowWarning = false;
                                            if (lastBgValue - 15 <= pref_warnLow) {
                                                lowWarning = true;
                                            }

                                            sayIt(Integer.parseInt(bgArrayStr[i]), Integer.parseInt(trendArrayStr[i]), lowWarning, pref_ttsPolite, null);

                                        }
                                    }
                                    //----------------------------------------------------------------------------------------
                                    // This is used for smart alarming. If we are trending, then don't go into hibernate mode
                                    //----------------------------------------------------------------------------------------
                                    if (Integer.parseInt(trendArrayStr[i]) != 4) {
                                        safeTrend = false;                            // our trending isn't cool for doing smart alarms
                                    }
                                    //----------------------------------------------------------------------------------------
                                    // we were able to save the BG in our DB..so see if we should adjust our high
                                    //----------------------------------------------------------------------------------------
                                    if (pref_startTrailingHigh) {
                                        newHighLimit = trailingHighThreshold(getApplicationContext(), lastBgValue, previousBgValue, pref_warnHigh, pref_highTarget);
                                        if (newHighLimit != -1) {
                                            alarmFlag = true;
                                            alarmLevel = TARGET_HIGH_ALARM;
                                        }

                                    }

                                    if (pref_startTrailingLow) {
                                        newLowLimit = trailingLowThreshold(getApplicationContext(), lastBgValue, previousBgValue, pref_warnLow, pref_lowTarget);
                                        if (newLowLimit != -1) {
                                            alarmFlag = true;
                                            alarmLevel = TARGET_LOW_ALARM;
                                        }

                                    }
                                }
                            }
                        }
                        //------------------------------------------------------
                        // let's see if we should look ahead
                        //------------------------------------------------------

                        if (prefs.getBoolean("pref_predictive", false)) {
                            int dbRecords;
                            int[] bgArray= new int[MAX_LOOKBACK];
                            int[] projectedValues = new int[MAX_PROJECTIONS];
                            //long pastTime=(System.currentTimeMillis() / 1000) - (60*pref_predictSampleCount);
                            dbRecords=(myDb.getLastBgDataAsArray(bgArray, (System.currentTimeMillis() / 1000) - (60*10*pref_predictSampleCount), pref_predictSampleCount));
                            if(dbRecords == pref_predictSampleCount) {
                                tools.projectedBgValue(bgArray, pref_predictSampleCount, projectedValues);                         // calculate the predicted BG value
                                if( projectedValues[0] != -1 && !alarmFlag) {                                        // this trumps nothing !!
                                    for (int i = 0; i < pref_predictLookAhead; i++) {
                                        if (projectedValues[i] <= pref_warnLow) {
                                            alarmFlag = true;
                                            alarmLevel = LOW_ALARM;
                                            myDb.logIt(LOG_LEVEL_LOW, "Early warning for possible low - Predicted BG of " + projectedValues[i]);
                                            break;
                                        } else {
                                            if (projectedValues[i] >= pref_warnHigh) {
                                                alarmFlag = true;
                                                alarmLevel = HIGH_ALARM;
                                                myDb.logIt(LOG_LEVEL_HIGH, "Early warning for possible high - Predicted BG of " + projectedValues[i]);
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    else {
                        // Log.d("MyService", "Setting Alarm for DATA");
                        alarmFlag = true;
                        alarmLevel = DATA_ERROR_ALARM;                                              // 5 - data error
                    }
                }
                // The toggle is set to 'off', so everything down the line should be off
                else {
                    myDb.updateServiceStatus("Internet Connection",SERVICE_UNKNOWN);
                    myDb.updateServiceStatus("Web Handler",        SERVICE_UNKNOWN);// aka cloud server
                    myDb.updateServiceStatus("Background Service", SERVICE_UNKNOWN);
                    myDb.updateServiceStatus("Cloud Account",      SERVICE_UNKNOWN);
                }

                //----------------------------------------------------------------------------------
                // On the pecking order  of alarms
                // 1. Hard Lows
                // 2. Regular lows
                // 3. Regular highs
                // 4. sustained high
                // 5. Data error
                // 6. High setting changed
                // 7. Low setting changed
                // 8. trend up
                // 9. trend down
                //
                //
                // For lows and highs we can use 'if-else' logic, since you are either low or high
                // but during those alarms, we can also have service failures. We don't want to
                // lose that information, but in the meantime, we also don't want to spawn an alarm
                // if the low or high have been set, hence the reason for the 'alarmSounded' flag
                //
                // NOTE: In the low section, don't clear out the 'high' if it exists, in the rare
                // case we will hit a high within 5 minutes of hitting a low
                //----------------------------------------------------------------------------------
                if (alarmFlag) {                                                                    // alarm has been set
                    myDb.updateAlarm(ALL_ALARMS, 1);                                                // set the flag that one of the alarms went off
                    if (    (alarmLevel != CRITICAL_LOW_ALARM) &&
                            (alarmLevel != DATA_ERROR_ALARM)) {

                            setAlarm(pref_refreshInterval, myDb.getLastDexBgTime());                // if we alarmed, next refreshes are 5 minutes.. unless
                    }



                    switch (alarmLevel) {
                        case CRITICAL_LOW_ALARM:
                            setAlarm(pref_refreshInterval, myDb.getLastDexBgTime());
                            if(pref_hardLowBehavior) {                                              // yes, we want annoying
                                if (!criticalAlarm(lastBgValue, CRITICAL_LOW_ALARM))
                                    myDb.logIt(LOG_LEVEL_ERROR, "Could not invoke the critical alarm");
                                else
                                    myDb.logIt(LOG_LEVEL_CRITICAL_LOW, "Critical hard low alarm set - value: " + lastBgValue);
                            }
                            else {
                                if(hardLowSoundFile != null) {
                                    myDb.logIt(LOG_LEVEL_CRITICAL_LOW, "Critical hard low alarm set - value: " + lastBgValue);

                                    if(pref_tts)
                                        soundAlarm(hardLowSoundFile, vibrate, override, maxPlayTime, true, pref_ttsPolite,
                                                   lastBgValue, lastTrendValue, true, null);
                                    else
                                        soundAlarm(hardLowSoundFile, vibrate, override, maxPlayTime, false, false,
                                                   null, null, true, null);
                                }
                            }

                            myDb.updateAlarm(CRITICAL_LOW_ALARM, 1);
                            if(lastDataErrAlarmTime != 0) myDb.clearAlarm(DATA_ERROR_ALARM);

                            break;
                        case LOW_ALARM: // regular low
                            if(lowSoundFile != null) {
                                if ((lowSnoozeTime + lastLowAlarmTime + fuzzySeconds) <= (System.currentTimeMillis() / 1000)) {
                                    myDb.logIt(LOG_LEVEL_LOW, "Low alarm set - value: " + lastBgValue);


                                    boolean lowWarning = false;
                                    if (lastBgValue - 15 <= pref_warnLow) {
                                        lowWarning = true;
                                    }

                                    if(pref_tts)
                                        soundAlarm(lowSoundFile, vibrate, override, maxPlayTime, true, pref_ttsPolite,
                                                    lastBgValue, lastTrendValue, lowWarning, null);
                                    else
                                        soundAlarm(lowSoundFile, vibrate, override, maxPlayTime, false, false,
                                                null, null, lowWarning, null);


                                    myDb.updateAlarm(LOW_ALARM, 1);                                     // updating current low
                                }
                            }
                            if(lastDataErrAlarmTime != 0) myDb.clearAlarm(DATA_ERROR_ALARM);
                            myDb.clearAlarm(CRITICAL_LOW_ALARM);
                            break;
                        case HIGH_ALARM: // regular high
                            if (highSoundFile != null) {       // we are running high
                                if (((highSnoozeTime + lastHighAlarmTime + fuzzySeconds) <= (System.currentTimeMillis() / 1000)) || (lastHighAlarmTime == 0)) {
                                    myDb.logIt(LOG_LEVEL_HIGH, "High alarm set - value: " + lastBgValue);
                                    highBgAlarmCount++;

                                    //Log.d("MyService ", "Alarm count -->" + highBgAlarmCount);

                                    if( (highBgAlarmCount >= pref_highAlarmCount)&& (annoying) ){
                                        criticalAlarm(lastBgValue, HIGH_ALARM);
                                    }
                                    else {
                                        if (pref_tts)
                                            soundAlarm(highSoundFile, vibrate, override, maxPlayTime, true, pref_ttsPolite,
                                                    lastBgValue, lastTrendValue, false, null);
                                        else
                                            soundAlarm(highSoundFile, vibrate, override, maxPlayTime, false, false,
                                                    null, null, false, null);
                                    }

                                    myDb.updateAlarm(HIGH_ALARM, highBgAlarmCount);
                                }
                            }

                            // Not needed, since we must go through normal ranges before we hit highs
                            //myDb.clearAlarm(CRITICAL_LOW_ALARM);
                            //myDb.clearAlarm(LOW_ALARM);
                            if(lastDataErrAlarmTime != 0) myDb.clearAlarm(DATA_ERROR_ALARM);
                            break;
                        case SUSTAINED_ALARM: // sustained high
                            if (shighSoundFile != null) {       // we are running high
                                myDb.logIt(LOG_LEVEL_HIGH, "Sustained high alarm set");
                                if(pref_tts)
                                    soundAlarm(shighSoundFile, vibrate, override, maxPlayTime, true, pref_ttsPolite,
                                            lastBgValue, lastTrendValue, false, null);
                                else
                                    soundAlarm(shighSoundFile, vibrate, override, maxPlayTime, false, false,
                                            null, null, false, null);

                                //myDb.updateAlarm(SUSTAINED_ALARM, 1);

                            }

                            if(lastDataErrAlarmTime != 0) myDb.clearAlarm(DATA_ERROR_ALARM);
                            break;
                        case DATA_ERROR_ALARM2:
                        case DATA_ERROR_ALARM: // data error

                            //Log.d("SERVICE", "in data alarm switch section - setting alarm for 2.0 minutes");
                            setAlarm(2.5, myDb.getLastDexBgTime());                                 // Refresh in 2.5 minutes on data issues

                            //-------------------------------------------------------------------------
                            // we don't alarm on the first data error. But we do set the alarm count
                            //-------------------------------------------------------------------------
                            if (lastDataErrAlarmTime == 0) {
                                myDb.updateAlarm(DATA_ERROR_ALARM, 1);
                                //Log.d("SERVICE", "This was our first data error");

                            }
                            else {
                                if ( ( (dataErrSnoozeTimeInSecs + lastDataErrAlarmTime + fuzzySeconds) <= (System.currentTimeMillis() / 1000)) ) {
                                    if(alarmLevel == DATA_ERROR_ALARM) myDb.logIt(LOG_LEVEL_ERROR, "Data retrieval error from the internet");
                                    else                               myDb.logIt(LOG_LEVEL_ERROR, "Malformed data retrieved from the database");

                                    boolean soundAlarmFlag=false;
                                    dataErrAlarmCount++;

                                    myDb.updateAlarm(DATA_ERROR_ALARM, dataErrAlarmCount);
                                    //Log.d("SERVICE", "Data error count is (even count does nothing) -->" + dataErrAlarmCount);

                                    //--------------------------------------------------------------
                                    // some special logic for this guy
                                    // if we've hit a threshold, then turn off services
                                    // automatically
                                    // (First time + (ie. 30)
                                    //--------------------------------------------------------------
                                    if(dataErrAlarmCount % 2 == 1) {                                // since we wakeup every 2.5 minutes, the first error is free, the next we have an issue
                                        // Log.d("MyService[case 5]", "dataErrAlarmCount:" + dataErrAlarmCount + " - Prefs:" + prefs.getString("pref_autoOffTimeOut", "60"));
                                        if (prefs.getBoolean("pref_autoOff", true)) {
                                            if (firstDataErrAlarmTime + (Integer.parseInt(prefs.getString("pref_autoOffTimeOut", "30")) * 60) <= System.currentTimeMillis() / 1000) {
                                                myDb.clearAlarm(DATA_ERROR_ALARM);
                                                turnOffServices(getApplicationContext());
                                                if (autoOffSoundFile != null) {

                                                    soundAlarm(autoOffSoundFile, vibrate, override, maxPlayTime, false, false, null, null, false, null);
                                                    soundAlarmFlag = true;
                                                }
                                                myDb.logIt(LOG_LEVEL_WARNING, "Services have been turned off, please see settings");
                                                updateNotification(0, 0, tools.epoch2FmtTime(System.currentTimeMillis() / 1000, "hh:mm a"), false, false, "services have been automatically turned off");
                                            }
                                        }

                                        //Log.d("MyService", "Soundfile" + dataErrSoundFile + " alarmFlag" + soundAlarmFlag);

                                        if (dataErrSoundFile != null && !soundAlarmFlag) {              // we didn't play the "I'm turning off services notification sound
                                            soundAlarm(dataErrSoundFile, vibrate, override, maxPlayTime, false, false, null, null, false, null);
                                        }
                                    }
                                }
                            }

                            break;
                        case TARGET_HIGH_ALARM: // trailing high
                            if(trailingHighAlarmTone != null) {
                                if(newHighLimit == pref_highTarget) {
                                    say("The target high is now " + pref_highTarget);
                                    myDb.logIt(LOG_LEVEL_INFO, "High warning limit has reached it's target of " + pref_highTarget);
                                }
                                else {
                                    soundAlarm(trailingHighAlarmTone, false, false, maxPlayTime, false, false,
                                            null, null, false, null);
                                    myDb.logIt(LOG_LEVEL_INFO, "The high warning limit has been reset to " + String.valueOf(newHighLimit));
                                }

                            }
                            if(lastDataErrAlarmTime != 0) myDb.clearAlarm(DATA_ERROR_ALARM);
                            break;
                        case TARGET_LOW_ALARM: // trailing low
                            if(trailingLowAlarmTone != null) {
                                if(newLowLimit == pref_lowTarget) {
                                    say("The target low is now " + pref_lowTarget);
                                    myDb.logIt(LOG_LEVEL_INFO, "Low warning limit has reached it's target of " + pref_lowTarget);
                                }
                                else {
                                    soundAlarm(trailingLowAlarmTone, false, false, maxPlayTime, false, false,
                                            null, null, false, null);
                                    myDb.logIt(LOG_LEVEL_INFO, "The low warning limit has been reset to " + String.valueOf(newLowLimit));
                                }
                            }
                            if(lastDataErrAlarmTime != 0) myDb.clearAlarm(DATA_ERROR_ALARM);
                            break;
                        case TREND_LOW_ALARM:
                            if(lowTrendAlarmTone != null) {
                                //Log.d("MyService " , "trendsnoozetime" + trendSnoozeTime + " lastLowTrendAlarmTime" + lastLowTrendAlarmTime);
                                //Log.d("MyService ", "system current time"  + System.currentTimeMillis()/1000);

                                if ((trendSnoozeTime + lastLowTrendAlarmTime + fuzzySeconds) <= (System.currentTimeMillis() / 1000)) {
                                    soundAlarm(lowTrendAlarmTone, false, false, maxPlayTime, false, false,
                                            null, null, false, null);
                                    myDb.logIt(LOG_LEVEL_WARNING, "Low trend alarm set");
                                    myDb.updateAlarm(TREND_LOW_ALARM, 1);
                                }
                            }
                            if(lastDataErrAlarmTime != 0) myDb.clearAlarm(DATA_ERROR_ALARM);
                            break;
                        case TREND_HIGH_ALARM:
                            if(highTrendAlarmTone != null) {
                                if ((trendSnoozeTime + lastHighTrendAlarmTime + fuzzySeconds) <= (System.currentTimeMillis() / 1000)) {
                                    soundAlarm(highTrendAlarmTone, false, false, maxPlayTime, false, false,
                                            null, null, false, null);
                                    myDb.logIt(LOG_LEVEL_WARNING, "High trend alarm set");
                                    myDb.updateAlarm(TREND_HIGH_ALARM, 1);
                                }
                            }
                            if(lastDataErrAlarmTime != 0) myDb.clearAlarm(DATA_ERROR_ALARM);
                            break;


                    }
                }
                //----------------------------------------------------------------------------------
                // No alarming this run through. Clear all alarms that might exist
                //----------------------------------------------------------------------------------
                else {
                    if( pref_smartRefresh ) {                                                       // we have smart alarming and out trend is currently stable
                        if(safeTrend)     setAlarm(15, myDb.getLastDexBgTime());
                        else              setAlarm(pref_refreshInterval,  myDb.getLastDexBgTime());
                    }
                    else {
                                          setAlarm(pref_refreshInterval,  myDb.getLastDexBgTime());
                    }
                    if(lastAlarmTime != 0 )       myDb.clearAlarm(ALL_ALARMS);
                    if(lastLowAlarmTime != 0)     myDb.clearAlarm(LOW_ALARM);
                    if(lastHighAlarmTime != 0)    myDb.clearAlarm(HIGH_ALARM);
                    if(lastLowTrendAlarmTime!=0)  myDb.clearAlarm(TREND_LOW_ALARM);
                    if(lastHighTrendAlarmTime!=0) myDb.clearAlarm(TREND_HIGH_ALARM);
                                                  myDb.clearAlarm(DATA_ERROR_ALARM);                // if no error at all, clear alarm for data
                }

            }                                                                                       // end of service mode
            else {                                                                                  // our services are off
                //Log.d("SERVICE", "Database RC:" + dbRc);
                if(dbRc) {
                    myDb.logIt(LOG_LEVEL_WARNING, "Terminating the background service");
                    myDb.updateServiceStatus("Background Service", SERVICE_OFF);
                    myDb.updateServiceStatus("Web Handler", SERVICE_UNKNOWN);
                    myDb.updateServiceStatus("Internet Connection", SERVICE_UNKNOWN);
                    myDb.updateServiceStatus("Cloud Account", SERVICE_UNKNOWN);
                    setAlarm(0, 0);
                    stopSelf();
                }
                //----------------------------------------------------------------------------------
                // Sole purpose of this is to see if the device is attached
                // if there's no device attached, no test mode, and the service is turned off
                // then why would we run ?
                // we are going to kill any scheduling of this service
                //----------------------------------------------------------------------------------
            }

            //===============================================================================
            // APP WIDGET
            // Move App widget code here
            //===============================================================================
            String dbBgVal="---";                                                                   // display of bg
            int dbBgTrend=0;                                                                        // display trend                                                                                 // the line from the database
            int dbBgNumValue=0;
            String stringDate="--:--:--";

            String rec = myDb.getBgData(15);                                                        // Split the record into it's respective parts
            if(rec != null) {
                String recordPiece[] = rec.split("\\|");                                            // Split the record into it's respective parts
                dbBgVal = recordPiece[1];                                                           // otherwise, it's going to be 0, so it's mg/dl (default value)
                dbBgNumValue=Integer.parseInt(recordPiece[1]);
                dbBgTrend = Integer.parseInt(recordPiece[2]);
                stringDate = tools.epoch2FmtTime(Long.parseLong(recordPiece[3]), "MMM d yyyy h:mm a");
            }

            //---------------------------------------------------------
            // Get the real data from the database
            //---------------------------------------------------------

            AppWidgetManager mgr= AppWidgetManager.getInstance(getApplicationContext());
            ComponentName provider=new ComponentName(getApplicationContext(), MyWidget.class);

            int[] allWidgetIds = mgr.getAppWidgetIds(provider);
            for (int widgetId : allWidgetIds) {
                RemoteViews remoteViews = new RemoteViews(getApplicationContext().getPackageName(), R.layout.widget);

                if (rec != null) {
                    remoteViews.setTextViewText(R.id.widget_usbTime, stringDate);
                    remoteViews.setTextColor(R.id.widget_bg, Color.parseColor(tools.bgToColor(dbBgNumValue, pref_warnLow, pref_warnHigh)));
                    remoteViews.setTextViewText(R.id.widget_bg, dbBgVal);

                    switch (dbBgTrend) {
                        case 0:
                            remoteViews.setImageViewResource(R.id.widget_trend, R.mipmap.s0);
                            break;
                        case 10:
                            remoteViews.setImageViewResource(R.id.widget_trend, R.mipmap.s10);
                            break;
                        case 1:
                            remoteViews.setImageViewResource(R.id.widget_trend, R.mipmap.s90);
                            break;
                        case 2:
                            remoteViews.setImageViewResource(R.id.widget_trend, R.mipmap.s110);
                            break;
                        case 3:
                            remoteViews.setImageViewResource(R.id.widget_trend, R.mipmap.s135);
                            break;
                        case 4:
                            remoteViews.setImageViewResource(R.id.widget_trend, R.mipmap.s180);
                            break;
                        case 5:
                            remoteViews.setImageViewResource(R.id.widget_trend, R.mipmap.s225);
                            break;
                        case 6:
                            remoteViews.setImageViewResource(R.id.widget_trend, R.mipmap.s250);
                            break;
                        case 7:
                            remoteViews.setImageViewResource(R.id.widget_trend, R.mipmap.s270);
                            break;
                        default:
                            remoteViews.setImageViewResource(R.id.widget_trend, R.mipmap.s0);
                            break;
                    }
                }
                else {
                    long epochAsLong = myDb.getLastRunTime();
                    if(epochAsLong != 0) {
                        remoteViews.setTextViewText(R.id.widget_usbTime, tools.now());
                    }
                    else {
                        remoteViews.setTextViewText(R.id.widget_usbTime, "--:--:--");
                    }
                    remoteViews.setTextViewText(R.id.widget_bg, "");
                    remoteViews.setImageViewResource(R.id.widget_trend, R.mipmap.s0);
                }

                // Register an onClickListener
                Intent intent = new Intent(getApplicationContext(), MyWidget.class);

                intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, allWidgetIds);

                PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                remoteViews.setOnClickPendingIntent(R.id.widget_bg, pendingIntent);
                mgr.updateAppWidget(widgetId, remoteViews);

            }

            //===============================================================================
            // End APP WIDGET
            //===============================================================================
            if(dbRc) {
                myDb.close();
            }

            mWakeLock.release();
            stopSelf();
            return null;
        }

        /**
         * In here you should interpret whatever you fetched in doInBackground
         * and push any notifications you need to the status bar, using the
         * NotificationManager. I will not cover this here, go check the docs on
         * NotificationManager. *
         * What you HAVE to do is call stopSelf() after you've pushed your
         * notification(s). This will:
         * 1) Kill the service so it doesn't waste precious resources
         * 2) Call onDestroy() which will release the wake lock, so the device
         * can go to sleep again and save precious battery. */

        protected void onPostExecute(Void result) {
            // update notification
            stopSelf();
        }
    }


    public void soundAlarm(String soundFile, boolean vibrateFlag, final boolean override, final int maxPlayTime,
                           final boolean playTTS, final boolean ttsPolite, final Integer lastBgValue, final Integer lastTrendValue,
                           final boolean lowWarning, final String otherStuff) {

        Uri myUri1 = Uri.parse(soundFile);
        final MediaPlayer mp1 = new MediaPlayer();
        final AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        if(vibrateFlag) {
            Vibrator v = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
            //---------------------------------------------------------------------------------------------
            // http://android.konreu.com/developer-how-to/vibration-examples-for-android-phone-development/
            //---------------------------------------------------------------------------------------------
            int dot        = 200;   // Length of a Morse Code "dot" in milliseconds
            int dash       = 500;   // Length of a Morse Code "dash" in milliseconds
            int short_gap  = 200;   // Length of Gap Between dots/dashes
            int medium_gap = 500;   // Length of Gap Between Letters
            int long_gap   = 1000;  // Length of Gap Between Words
            long[] pattern = {
                    0,  // Start immediately
                    dot, short_gap, dot, short_gap, dot,    // s
                    medium_gap,
                    dash, short_gap, dash, short_gap, dash, // o                    medium_gap,
                    dot, short_gap, dot, short_gap, dot,    // s
                    long_gap
            };
            v.vibrate(pattern, -1);
        }

        mp1.setAudioStreamType(AudioManager.STREAM_MUSIC);

        //=================================================================================
        // I know this works, since if I call the same URI1 multiple times, it works.
        // I think the issue is with the setting up of the tts..
        // CONCLUSION : LEAVE THIS CODE ALONE
        //=================================================================================
        if( (audioManager.getStreamVolume(AudioManager.STREAM_ALARM) != 0) || (override) ){
            try {
                mp1.setDataSource(getApplicationContext(), myUri1);
                mp1.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mp1.setVolume(1f, 1f);

                mp1.prepare();
                mp1.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mediaPlayer) {
                        if(playTTS) {
                            //Log.d("MyService", "In on complete listener !!");
                            mp1.stop();
                            mp1.release();
                            mp1.reset();
                            sayIt(lastBgValue, lastTrendValue, lowWarning, ttsPolite, otherStuff);
                        }
                    }
                });
                mp1.start();
            }
            catch(IOException e) {
                e.printStackTrace();
            }
        }

        //-----------------------------------------------------------------------------------
        // I can't trust the user to pick a short tune. Imagine in the middle of the night
        // "Hey Jude" by the Beatles starts playing during a low BS.. and the only way to stop it
        // is either throwing the phone against the wall, or shutting it off, then cursing me
        // that this program sucks... so instead, we kick off a timer, and kill the song after
        // 'X' seconds. For the HARD low, we're going to present a button, since the tune will
        // continue to play until someone answers.. which is a real drawback to the dexcom
        //-----------------------------------------------------------------------------------
        Thread timer = new Thread() {
           public void run() {
                try {
                    sleep(maxPlayTime*1000);
                    if(mp1.isPlaying()) {
                        mp1.stop();
                        //Log.d("MyService", "Ended up killing it");
                        if(playTTS) {
                            sayIt(lastBgValue, lastTrendValue, lowWarning, ttsPolite, otherStuff);
                        }
                    }
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        timer.start();
    }


    //-------------------------------------------------------------------------------------
    // Routine  : onStartCommand
    // Class    : MyService
    // Called by: Class->fragment_preference | onPreferenceChange (listener)
    // Author   : Mike LiVolsi / Originally by various (lots of examples on the web)
    // Date     : Oct. 2014
    //
    // Purpose  : To have an icon and notifications in the notification area and notification
    //            tray, that this service is running
    //-------------------------------------------------------------------------------------
    @Override
    public int onStartCommand(Intent intent, int flags, int startid) {

        onHandleIntent(intent);
        return START_NOT_STICKY;
    }

    //-------------------------------------------------------------------------------------
    // Routine  : onDestroy
    // Class    : MyService
    // Called by: Class->fragment_preference | onPreferenceChange (listener)
    // Author   : Mike LiVolsi / Originally by various (lots of examples on the web)
    // Date     : Oct. 2014
    //
    // Purpose  : To have an icon and notifications in the notification area and notification
    //            tray, that this service is running
    //-------------------------------------------------------------------------------------
    @Override
    public void onDestroy()
    {
        super.onDestroy();
    }

    //-------------------------------------------------------------------------
    // Routine: updateNotication
    // Author : Mike LiVolsi
    // Date   : October 2014
    // Purpose: If there's a notification in the "notification tray", then this routine
    //          updates it
    //
    // http://developer.android.com/reference/android/support/v4/app/NotificationCompat.Builder.html
    //
    //-------------------------------------------------------------------------
    private void updateNotification(int inValue, int inTrend, String inTime, boolean smartWatchFormat, boolean bgNotificationFlag, String miscNotificationString){

        int notificationID=2112;

        String tmpBgString;
        String notificationTitle;
        String notificationText;
        int bgIcon;

        if(bgNotificationFlag) {

            if (inValue < 40)
                tmpBgString = "nilow";                                                                // What value to display way up top
            else {
                if (inValue > 400) {
                    tmpBgString = "nihigh";
                } else {
                    tmpBgString = "ni" + Integer.toString(inValue);                                     // ie: ni120.png (see mipmaps)
                }
            }

            //======================================================================================
            // For codes and their values see:
            // http://character-code.com/arrows-html-codes.php
            //======================================================================================
            if(smartWatchFormat) notificationTitle = "dExtender ";
            else                 notificationTitle = "dExtender " +  inTime;

            switch (inTrend) {
                case 0:   notificationText=Integer.toString(inValue) + " \u291b";
                    break;
                case 10:  notificationText=Integer.toString(inValue) + " ???";
                    break;
                case 1:  notificationText=Integer.toString(inValue) + " \u2191 \u2191";
                    break;
                case 2: notificationText=Integer.toString(inValue) + " \u2191";
                    break;
                case 3: notificationText=Integer.toString(inValue) + " \u2197";
                    break;
                case 4: notificationText=Integer.toString(inValue) + " \u2192";
                    break;
                case 5: notificationText=Integer.toString(inValue) + " \u2198";
                    break;
                case 6: notificationText=Integer.toString(inValue) + " \u2193";
                    break;
                case 7: notificationText=Integer.toString(inValue) + " \u2193 \u2193";
                    break;
                default:  notificationText=Integer.toString(inValue) + " \u219b";
                    break;
            }
                bgIcon = this.getResources().getIdentifier(tmpBgString, "mipmap", this.getPackageName());      // instead of R.mipmap.s<whatever>
        }
        else {
                tmpBgString="tapir3";
                bgIcon = this.getResources().getIdentifier(tmpBgString, "mipmap", this.getPackageName());      // instead of R.mipmap.s<whatever>
                notificationTitle="dExtender";
                notificationText=miscNotificationString;
        }

        //Log.d("MyService[notification]", notificationTitle + " " + notificationText);

        NotificationManager mNotificationManager =  (NotificationManager) getApplication().getSystemService(Context.NOTIFICATION_SERVICE);

        // Creates an explicit intent for an Activity in your app
        Intent notificationIntent = new Intent(getApplicationContext(), MyActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP); // new - Jan 2015

        PendingIntent resultPendingIntent = PendingIntent.getActivity(getApplicationContext(), (int) System.currentTimeMillis(), notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Bitmap bm = BitmapFactory.decodeResource(getResources(), bgIcon);                           // the large icon needs to be converted into a bitmap (weird)

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(MyActivity.class);
        stackBuilder.addNextIntent(notificationIntent);


        NotificationCompat.Builder mbuilder = new NotificationCompat.Builder(getApplicationContext())
                .setSmallIcon(bgIcon)                                                               // smallIcon - what shows up in the tippy top
                .setContentTitle(notificationTitle)                                                 // The larger text
                .setContentText(notificationText)                                                   // the smaller text under the title
                .setLargeIcon(bm)
                .setPriority(1)                                                                     // Priority min = -2 | max = 2
                .setContentIntent(resultPendingIntent)

                ;

        mNotificationManager.notify(notificationID, mbuilder.build());
    }



    //----------------------------------------------------------------------------
    // If we are running without a connection for a while, turn off services
    //----------------------------------------------------------------------------
    private void turnOffServices(Context inContext) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(inContext);
        SharedPreferences.Editor editor1 = settings.edit();
        editor1.putBoolean("prefsvc", false);
        editor1.apply();
    }

    //----------------------------------------------------------------------------
    // If trailing threshold is enabled, reset it until we hit our target.
    // If target is reached, then disable the trailing threshold
    //
    // In order to understand, I'll use an example. Our current BG is 123, our
    // current setttings is 140 and our target is 120
    //----------------------------------------------------------------------------
    private int trailingHighThreshold(Context inContext, Integer inCurrentBg, Integer inLastBg,
                                      Integer inCurrentSetting, Integer inHighTarget) {

        //Log.d("MyService|trailingHigh", "Current BG -->" + inCurrentBg + " Last BG-->" + inLastBg);
        int roundedBg = roundUp(inCurrentBg);                                                       // 123 now becomes 130
        int outNewValue=-1;

        //------------------------------------------------------------------------------------------
        // if the BG is going higher, then don't adjust squat (as Seinfeld would say "It makes no sense")
        // If the bg is going lower, then odds are ("odds" being the key word), we are heading down
        // as a trend
        //------------------------------------------------------------------------------------------
        if(inCurrentBg < inLastBg) {
            if ((roundedBg < inCurrentSetting)) {                                                   // 130 is less than 140
                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(inContext);
                SharedPreferences.Editor editor1 = settings.edit();

                if (roundedBg <= inHighTarget) {                                                    // 130 is not less than or equal to 120
                    editor1.putString("listHigh", String.valueOf(inHighTarget));                    // if it was, we'd set it to our target bg
                    editor1.putBoolean("pref_startTrailingHigh", false);                            // and turn this feature off
                    outNewValue = inHighTarget;
                } else {
                    editor1.putString("listHigh", String.valueOf(roundedBg));                       // otherwise, just lower our setting to new value
                    outNewValue = roundedBg;
                }
                editor1.apply();
            }
        }

        return outNewValue;
    }

    //----------------------------------------------------------------------------
    // In order to understand, I'll use an example. Our current BG is 71, our
    // current setttings is 60 and our target is 80
    //----------------------------------------------------------------------------
    private int trailingLowThreshold(Context inContext, Integer inCurrentBg, Integer inLastBg,
                                     Integer inCurrentSetting, Integer inLowTarget) {

        int roundedBg = roundDown(inCurrentBg);                                                 // 71 becomes 70
        int outNewValue=-1;

        //------------------------------------------------------------------------------------------
        // if the BG is going lower, then don't adjust squat
        // If the bg is going higher, then odds are ("odds" being the key word), we are heading higher
        // as a trend
        //------------------------------------------------------------------------------------------
        if (inCurrentBg > inLastBg) {
            if ((roundedBg > inCurrentSetting)) {                                                      // 70 is greater than 60
                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(inContext);
                SharedPreferences.Editor editor1 = settings.edit();

                if (roundedBg >= inLowTarget) {                                                          // 70 is not >= to 80
                    editor1.putString("listLow", String.valueOf(inLowTarget));                          // if it was, we'd set it to our target bg
                    editor1.putBoolean("pref_startTrailingLow", false);                                 // and turn this feature off
                    outNewValue = inLowTarget;
                } else {
                    editor1.putString("listLow", String.valueOf(roundedBg));                           // otherwise, just lower our setting to new value
                    outNewValue = roundedBg;
                }
                editor1.apply();
            }
        }
        return outNewValue;
    }

    private int roundUp(double n) {
        return (int) Math.ceil(n / 10)*10;
    }

    private int roundDown(double n){
        return (int) Math.floor(n / 5)*5;
    }

    //------------------------------------------------------------------
    //
    //------------------------------------------------------------------
    private boolean criticalAlarm(int inBg, int inAlarmType) {
        Intent openAlarmActivity = new Intent(getApplicationContext(), MyAlarm.class);
        openAlarmActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        openAlarmActivity.putExtra("bgValue",   String.valueOf(inBg));
        openAlarmActivity.putExtra("alarmType", String.valueOf(inAlarmType));

        startActivity(openAlarmActivity);
        return true;
    }

    //------------------------------------------------------------------
    // Method : SayIt
    // Author : MLV
    // Purpose: To fire up the speaker activity just to say what the
    //          bg value is and the trend
    // Note   : we are going to format the values so they 'sound' right
    //          when read.
    //------------------------------------------------------------------
    private boolean sayIt(int inBg, int inTrend, boolean inLowWarning, boolean inTtsPolite, String inOtherStuff) {

        //-----------------------------------------
        // Convert to strings and verbal output
        //-----------------------------------------
        String strBg=String.valueOf(inBg);
        String strTrend;

        switch (inTrend) {
            case 0:   strTrend = "trend unknown";
                break;
            case 10:  strTrend = "question marks";
                break;
            case 1:  strTrend = "double arrow up";
                break;
            case 2: strTrend = "single arrow up";
                break;
            case 3: strTrend = "angled up";
                break;
            case 4: strTrend = "level";
                break;
            case 5: if(inLowWarning) strBg = "attention " + strBg;
                    strTrend = "angled down";
                break;
            case 6: if(inLowWarning) strBg = "warning "   + strBg;
                    strTrend = "single arrow down";
                break;
            case 7: if(inLowWarning) strBg = "danger "    + strBg;
                    strTrend = "double arrow down";
                break;
            default:  strTrend = "trend unknown";
                break;
        }

        //----------------------------------------------------------------
        // Setup the intent
        //----------------------------------------------------------------
        Intent openSpeakerActivity = new Intent(getApplicationContext(), MySpeakerActivity.class);
        openSpeakerActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if(inTtsPolite)  openSpeakerActivity.putExtra("polite", "yes");                             // fancy schmancy
        else             openSpeakerActivity.putExtra("polite", "no");
        openSpeakerActivity.putExtra("bgValue", strBg);
        openSpeakerActivity.putExtra("trendValue", strTrend);

        if (inOtherStuff != null) openSpeakerActivity.putExtra("misc", inOtherStuff);
        else openSpeakerActivity.putExtra("misc", "--");

        try {
            startActivity(openSpeakerActivity);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        //----------------------------------------------------------------
        // End Intent
        //----------------------------------------------------------------
        return true;
    }

    //-----------------------------------------
    // Convert to strings and verbal output
    //-----------------------------------------
    private boolean say(String whatToSay) {

        Intent openSpeakerActivity = new Intent(getApplicationContext(), MySpeakerActivity.class);
        openSpeakerActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        openSpeakerActivity.putExtra("freeform", whatToSay);


        startActivity(openSpeakerActivity);
        //----------------------------------------------------------------
        // End Intent
        //----------------------------------------------------------------
        return true;
    }


    //--------------------------------------------------------------------------------------------
    // Method : SetAlarm
    // inMinutes is our preference
    // inDexTime is the time the receiver recorded data (as per share)
    //--------------------------------------------------------------------------------------------
    public boolean setAlarm(double inMinutes, long inEpochLastDexTime) {
        //===============================================================
        // Clear the alarm just in case we get hosed up
        //===============================================================
        Log.d("SERVICE", "set alarm called with inminutes=" + inMinutes);

        //---------------------------------------------------------------
        // Since the alarm is inexact, we can be off by a few seconds
        // on either side (a few seconds early or late)
        // Let's make it 10 (total overkill) just to be safe
        // - dexLagTime is the lag between what the receiver reads and
        //   the time it's posted to the web site
        //---------------------------------------------------------------
        int fuzzyTime=10;
        int dexLagTime=60;                                        // let's do a 2 minute lag time

        // setup the alarm manager
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        // setup the intent to do stuff with the service
        Intent i = new Intent(this, MyReceiver.class);

        // Was 'getService' - Changed to 'getBroadcast'
        PendingIntent pendingIntent;
        pendingIntent = PendingIntent.getBroadcast(this, 0, i, 0);

        // Kill any stragglers (if any )
        alarmManager.cancel(pendingIntent);

        //----------------------------------------------------------------
        // Because above kills the next run, bolting at this point leaves
        // the alarm manager with nothing.. which is what we want
        //----------------------------------------------------------------
        if (inMinutes == 0) {
            stopSelf();
            return false;
        }
        else {

            //-----------------------------------------------------------
            // set the alarm
            // If minutes > 0, then set the alarm
            // NOTE: MyReceiver will handle any calls at this point
            //-----------------------------------------------------------
            double calcSeconds;
            // convert to millis - will get killed before firing

            //======================================================================================
            // Explanation for people like me with a touch of ADD.
            // If the current time is 10AM and the last time we got data was 9:53, then default to
            // the standard refresh. Why not just do some math to figure it out ?
            // Because, what if there is no data ? I don't think it's wise firing every 30 seconds
            // to get data that may not be there.
            // But what about no data during the interval. Well.. thats why we are going to do some
            // math for that..

            //Log.d("service", "Dextime is   : " + inEpochLastDexTime);
            //Log.d("service", "current time : " + System.currentTimeMillis()/1000);
            //Log.d("service", "difference   : " + ((System.currentTimeMillis()/1000) - inEpochLastDexTime));
            //Log.d("service", "inSeconds    : " + (inMinutes*60));

            if (inEpochLastDexTime != 0) {                                                          // there is a value

                if (((System.currentTimeMillis() / 1000) - inEpochLastDexTime) >= ((inMinutes * 60)+ fuzzyTime)) { // we are late to refresh
                    calcSeconds = inMinutes * 60;                                                   // ie . make it 5 minutes
                } else {
                    calcSeconds = ((inMinutes*60) - ((System.currentTimeMillis() / 1000) - inEpochLastDexTime));   //
                    calcSeconds += dexLagTime;
                }


            } else {                                                                                // we are explicitly told to refresh
                calcSeconds = inMinutes * 60;
            }

            long whenToFire = SystemClock.elapsedRealtime() + ((long) calcSeconds * 1000);
            long whenToFireAfterThat = (long) inMinutes * 60 * 1000;


            Log.d("service", "Alarm will fire in  : " + calcSeconds + " seconds");
            Log.d("service", "..and after that    : " + whenToFireAfterThat/1000);

            alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,                      // wakeup and fire
                    whenToFire,   // when (in milliseconds)
                    whenToFireAfterThat,                                       // and after that ?
                    pendingIntent);                                            // what am I going to run

            return true;
        }
    }
}