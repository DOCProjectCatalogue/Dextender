package com.dextender.dextender;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import android.os.Environment;
import android.os.SystemClock;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

//------------------------------------------------------------------------------------------
// Author      : http://www.youtube.com/watch?v=Df129IGl31I (aka thenewboston)
//
// Modified by : Mike LiVolsi
// Date        : January 2014
//
// Purpose     : This was tutorial #56 from "thenewboston" with other minor modifications
//               from around the web, since some of his calls are now depracated.
//               This is an added class that we're using the handle the fragments
//               Fragments are "pieces" of screens that we're trying to show.
//               For every change, we need a listener.
//               This class needs to be specified in the manifest.
//
//------------------------------------------------------------------------------------------
public class fragment_preference extends PreferenceActivity  {


    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getFragmentManager().beginTransaction().replace(android.R.id.content, new MyPreferenceFragment()).commit();
    }

    //===========================================================================================
    // Class : MyPreferenceFragement
    // Author: Mike LiVolsi
    //===========================================================================================
    public static class MyPreferenceFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

        //short ALL_ALARMS = 0;
        //short CRITICAL_LOW_ALARM=1;
        //short LOW_ALARM = 2;
        //short HIGH_ALARM = 3;
        short SUSTAINED_ALARM = 4;
        short DATA_ERROR_ALARM=5;
        //short TARGET_HIGH_ALARM=6;
        //short TARGET_LOW_ALARM=7;
        //short TREND_LOW_ALARM=8;
        //short TREND_HIGH_ALARM=9;

        public static final String KEY_PREF_SERVICE = "prefsvc";                                    // This is the service key

        public static final String KEY_PREF_UID = "uid";                                            // the key in the preferences.xml that I want to work with
        public static final String KEY_PREF_HARD_LOW = "listHardLow";                               // a value that can't be snoozed
        public static final String KEY_PREF_LOW = "listLow";
        public static final String KEY_PREF_HIGH = "listHigh";
        public static final String KEY_PREF_HIGH_ALARM_COUNT="pref_highAlarmCount";
        //public static final String KEY_PREF_WIZARD = "pref_wizard";
        public static final String KEY_PREF_ABOUT = "pref_about";
        public static final String KEY_PREF_INTERVAL = "pref_refresh_interval";                     // how often to refresh
        public static final String KEY_PREF_LOW_SNOOZE_INTERVAL="pref_lowSnoozeElapse";
        public static final String KEY_PREF_HIGH_SNOOZE_INTERVAL="pref_highSnoozeElapse";
        public static final String KEY_PREF_RECEIVER_ERROR_SNOOZE_INTERVAL="pref_receiverErrorSnoozeTime";
        public static final String KEY_PREF_SERVICE_SNOOZE_INTERVAL="pref_serviceSnoozeElapse";
        public static final String KEY_PREF_SMARTLIMIT_SWITCH="pref_smartLimit";
        public static final String KEY_PREF_SUS_HIGH="sustainedListHigh";
        public static final String KEY_PREF_TARGET_HIGH="followListHigh";
        public static final String KEY_PREF_TARGET_LOW="followListLow";
        public static final String KEY_PREF_SUS_INTERVAL="sustainedTimeHigh";
        public static final String KEY_PREF_TONE_PLAY_TIME="pref_maxPlayTime";
        public static final String KEY_PREF_DATA_ERROR_SNOOZE="pref_dataErrSnoozeTime";
        public static final String KEY_PREF_DEXTENDER_NAME="pref_dextenderName";
        public static final String KEY_PREF_TTS="pref_tts";


        //==========================================================================================
        // Method: onCreate
        // Type  : Built in
        // NOTE  : We aren't going to do initial key stuff here , except for the listener and intents
        //         All key manipulations will be done in the "On resume" method
        //==========================================================================================
        @Override
        public void onCreate(final Bundle savedInstanceState) {

            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);



            Preference prefLowButton = findPreference(KEY_PREF_LOW);
            prefLowButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference arg0) {

                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
                    String lowBgVal = prefs.getString(KEY_PREF_LOW,"70");
                    Intent openCircleActivity = new Intent(getActivity(), MyCircleActivity.class);
                    openCircleActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    openCircleActivity.putExtra("bgRangeType", "1");
                    openCircleActivity.putExtra("circleTitle", "Low Range");
                    openCircleActivity.putExtra("bgSetting", lowBgVal);


                    startActivity(openCircleActivity);
                    return true;
                }
            });

            Preference prefHighButton = findPreference(KEY_PREF_HIGH);
            prefHighButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference arg0) {
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
                    String highBgVal = prefs.getString(KEY_PREF_HIGH,"180");
                    Intent openCircleActivity = new Intent(getActivity(), MyCircleActivity.class);
                    openCircleActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    openCircleActivity.putExtra("bgRangeType", "2");
                    openCircleActivity.putExtra("circleTitle", "High Range");
                    openCircleActivity.putExtra("bgSetting", highBgVal);

                    startActivity(openCircleActivity);
                    return true;
                }
            });


            Preference prefSustainedHighButton = findPreference(KEY_PREF_SUS_HIGH);
            prefSustainedHighButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference arg0) {

                    MyDatabase myDb = new MyDatabase(getActivity());                                // call the Database class
                    try {
                        myDb.open();
                        myDb.clearAlarm(SUSTAINED_ALARM);                                           // sets the value to 0 in alarm count
                        myDb.close();
                    }
                    catch (Exception e) {
                         e.printStackTrace();
                    }

                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
                    String highBgVal = prefs.getString(KEY_PREF_SUS_HIGH,"180");
                    Intent openCircleActivity = new Intent(getActivity(), MyCircleActivity.class);
                    openCircleActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    openCircleActivity.putExtra("bgRangeType", "3");
                    openCircleActivity.putExtra("circleTitle", "Sustained High");
                    openCircleActivity.putExtra("bgSetting", highBgVal);

                    startActivity(openCircleActivity);
                    return true;
                }
            });

            Preference prefTargetHighButton = findPreference(KEY_PREF_TARGET_HIGH);
            prefTargetHighButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference arg0) {
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
                    String highBgVal = prefs.getString(KEY_PREF_TARGET_HIGH,"180");
                    Intent openCircleActivity = new Intent(getActivity(), MyCircleActivity.class);
                    openCircleActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    openCircleActivity.putExtra("bgRangeType", "4");
                    openCircleActivity.putExtra("circleTitle", "Target High");
                    openCircleActivity.putExtra("bgSetting", highBgVal);

                    startActivity(openCircleActivity);
                    return true;
                }
            });


            Preference prefTargetLowButton = findPreference(KEY_PREF_TARGET_LOW);
            prefTargetLowButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference arg0) {
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
                    String lowBgVal = prefs.getString(KEY_PREF_TARGET_LOW,"70");
                    Intent openCircleActivity = new Intent(getActivity(), MyCircleActivity.class);
                    openCircleActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    openCircleActivity.putExtra("bgRangeType", "5");
                    openCircleActivity.putExtra("circleTitle", "Target Low");
                    openCircleActivity.putExtra("bgSetting", lowBgVal);

                    startActivity(openCircleActivity);
                    return true;
                }
            });


            //------------------------------------------------------------------
            // Act as a button in preference code for the 'about' screen
            // When the "About" button is clicked, start an intent (activity)
            // about the 'about' screen (which is a fragment too)
            //------------------------------------------------------------------
            Preference button = findPreference(KEY_PREF_ABOUT);
            button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference arg0) {
                    Intent openMainActivity =  new Intent("android.intent.action.ABOUT");
                    startActivity(openMainActivity);
                    return true;
                }
            });

            //------------------------------------------------------------------
            // Get account information from snooper file
            //------------------------------------------------------------------
            Preference uidButton = findPreference(KEY_PREF_UID);
            uidButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

                @Override
                public boolean onPreferenceClick(Preference arg0) {
                    MyDatabase myDb = new MyDatabase(getActivity());                          // call the Database class
                    try {
                        myDb.open();
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }

                    //======================================================================
                    // READ FROM FILE (dextender.dat)
                    // NOTE: Same file "stuff" as from 'mysplash' class
                    //======================================================================
                    File[] Dirs = ContextCompat.getExternalFilesDirs(getActivity(), null);

                    String sdcard   = Dirs[0] + File.separator + "authenticate";
                    String fileName = "dextender.dat";

                    //Get the text file
                    File file = new File(sdcard + File.separator + fileName);

                    //----------------------------------------------------------------
                    // try seperate iterators of the file name. The issue is that
                    // the pacture capture app looks like it appends a blank to the
                    // file name
                    //----------------------------------------------------------------
                    if(!file.exists()){
                        fileName = "dextender.dat ";                                                // NOTE THE Trailing blank

                        file = new File(sdcard + file.separator + fileName);
                        if(!file.exists()) {
                            myDb.logIt(1, "" + file);
                            Toast toast = Toast.makeText(getActivity(), fileName + " not found", Toast.LENGTH_LONG);
                            toast.show();
                            myDb.close();
                            return false;
                        }
                    }

                    //Read text from file
                    StringBuilder text = new StringBuilder();
                    String line;
                    String fullRecord=null;
                    String accountId=null;
                    String applicationId=null;
                    String accountPwd=null;
                    boolean rc;


                    try {
                        BufferedReader buffer = new BufferedReader(new FileReader(file));

                        while ((line = buffer.readLine()) != null) {
                            text.append(line);
                            text.append('\n');
                        }
                        buffer.close();
                        fullRecord=text.toString();
                        rc=true;
                    }
                    catch (IOException e) {
                        rc=false;
                    }

                    if(rc) {
                        //----------------------------------------------------
                        // Take the string and populate the values via JSON
                        //----------------------------------------------------
                        try {
                            JSONObject jObject = new JSONObject(fullRecord);
                            accountId = jObject.getString("accountId");
                            applicationId = jObject.getString("applicationId");
                            accountPwd = jObject.getString("password");
                            rc = true;
                        } catch (Exception e) {
                            rc = false;
                        }
                    }
                    else {
                        myDb.logIt(1, file + " ");
                        myDb.logIt(1, "Missing file !! See below");
                        Toast toast = Toast.makeText(getActivity(), "See log for missing file location", Toast.LENGTH_LONG);
                        toast.show();
                        myDb.close();
                        return false;
                    }

                    if(rc) {
                        Context context = getActivity();
                        CharSequence toastText = "Account information retrieved\nAccount ID :" + accountId +
                                "\napplicationId:" + applicationId + "\naccountPwd:" + accountPwd;
                        int duration = Toast.LENGTH_LONG;

                        Toast toast = Toast.makeText(context, toastText, duration);
                        toast.show();

                        try {
                            //Log.d("preference", "Calling database with update");
                            myDb.updateAccountInformation(accountId,applicationId,accountPwd);
                            myDb.close();
                        } catch (Exception e) {
                             e.printStackTrace();
                        }
                    }

                    return true;
                }

            });

            //------------------------------------------------------------------
            //
            //------------------------------------------------------------------
            Preference serviceButton = findPreference(KEY_PREF_SERVICE);
            serviceButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

                @Override
                public boolean onPreferenceClick(Preference arg0) {
                    MyDatabase myDb = new MyDatabase(getActivity());                                // call the Database class
                    try {
                        myDb.open();
                        myDb.clearAlarm(DATA_ERROR_ALARM);                                          // sets the value to 0 in alarm count
                        myDb.close();
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                    return true;
                }

            });

            //------------------------------------------------------------------
            // Clear the alarm when the smart switch is turned on/off
            //------------------------------------------------------------------
            Preference smartSwitch = findPreference(KEY_PREF_SMARTLIMIT_SWITCH);
            smartSwitch.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

                @Override
                public boolean onPreferenceClick(Preference arg0) {
                    MyDatabase myDb = new MyDatabase(getActivity());                                // call the Database class
                    try {
                        myDb.open();
                        myDb.clearAlarm(SUSTAINED_ALARM);                                           // sets the value to 0 in alarm count
                        myDb.close();
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }

                    Toast toast = Toast.makeText(getActivity(), "'Elevated Warnings' start time reset", Toast.LENGTH_SHORT);
                    toast.show();

                    return true;
                }
            });


        }


        //==============================================================================
        // SOMETHING CLICKED
        // Whenever something is changes (ie. on/off switch) we end up here
        //==============================================================================
        @Override
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {

            if (
                key.equals(KEY_PREF_SERVICE) ||
                key.equals(KEY_PREF_UID) ||
                key.equals(KEY_PREF_HARD_LOW) ||
                key.equals(KEY_PREF_LOW) ||
                key.equals(KEY_PREF_HIGH) ||
                key.equals(KEY_PREF_HIGH_ALARM_COUNT) ||
                key.equals(KEY_PREF_SUS_HIGH) ||
                key.equals(KEY_PREF_SUS_INTERVAL) ||
                key.equals(KEY_PREF_LOW_SNOOZE_INTERVAL) ||
                key.equals(KEY_PREF_HIGH_SNOOZE_INTERVAL) ||
                key.equals(KEY_PREF_RECEIVER_ERROR_SNOOZE_INTERVAL) ||
                key.equals(KEY_PREF_SERVICE_SNOOZE_INTERVAL) ||
                key.equals(KEY_PREF_INTERVAL) ||
                key.equals(KEY_PREF_TONE_PLAY_TIME) ||
                key.equals(KEY_PREF_DATA_ERROR_SNOOZE) ||
                key.equals(KEY_PREF_DEXTENDER_NAME)||
                key.equals(KEY_PREF_TARGET_HIGH) ||
                key.equals(KEY_PREF_TARGET_LOW)
               ) {
                Preference exercisesPref = findPreference(key);

                boolean setFlag=false;
                boolean newBoolValue;

                //------------------------------------------------------------
                // Check if the service was changed. If so, then if the
                // service is off, there's a good chance the scheduler is also
                // off. If so, we call the alarm method and schedule a run
                //------------------------------------------------------------
                if(key.equals(KEY_PREF_SERVICE) ) {

                    newBoolValue= prefs.getBoolean(key, false);
                    if(newBoolValue) {
                        setAlarm(getActivity());
                    }
                    setFlag=true;
                }

                if(key.equals(KEY_PREF_SUS_HIGH) ) {
                    exercisesPref.setSummary("If the BG is lower than the high, but higher than " + prefs.getString(key, "") + "...");
                    setFlag=true;
                }
                if(key.equals(KEY_PREF_SUS_INTERVAL) ) {
                    exercisesPref.setSummary("...and it's been high for  " + prefs.getString(key, "") + " minutes...");
                    setFlag=true;
                }

                if( (key.equals(KEY_PREF_INTERVAL) ) ||
                    (key.equals(KEY_PREF_LOW_SNOOZE_INTERVAL) ) ||
                    (key.equals(KEY_PREF_HIGH_SNOOZE_INTERVAL) ) ||
                    (key.equals(KEY_PREF_RECEIVER_ERROR_SNOOZE_INTERVAL)) ||
                    (key.equals(KEY_PREF_SERVICE_SNOOZE_INTERVAL) ) )
                {
                    exercisesPref.setSummary(prefs.getString(key, "") + " Minutes");
                    setFlag=true;
                }

                // Length to play a tone
                if( key.equals(KEY_PREF_TONE_PLAY_TIME) ) {
                    exercisesPref.setSummary(prefs.getString(key, "10") + " seconds");
                    setFlag = true;
                }

                if( key.equals(KEY_PREF_DATA_ERROR_SNOOZE) ) {
                    exercisesPref.setSummary(prefs.getString(key, "5") +
                            " minutes before raising data error alert\nNOTE: This is a repeating alert");
                    setFlag = true;
                }


                if ( key.equals(KEY_PREF_HIGH)) {
                    if(Integer.parseInt(prefs.getString(key, "1000")) == 1000 ) {
                        exercisesPref.setSummary("Off");
                        setFlag = true;
                    }
                }

                if ( key.equals(KEY_PREF_LOW)) {
                    if( (Integer.parseInt(prefs.getString(key, "-1")) == -1 ) || (Integer.parseInt(prefs.getString(key, "0")) ==0) ) {
                        exercisesPref.setSummary("Off");
                        setFlag = true;
                    }
                }

                if ( key.equals(KEY_PREF_TARGET_HIGH)) {
                        exercisesPref.setSummary(prefs.getString(key, "180"));
                        setFlag = true;
                }

                if ( key.equals(KEY_PREF_TARGET_LOW)) {
                    exercisesPref.setSummary(prefs.getString(key, "70"));
                    setFlag = true;
                }


                if( key.equals(KEY_PREF_HIGH_ALARM_COUNT) ) {
                    if (Integer.parseInt(prefs.getString(KEY_PREF_HIGH_ALARM_COUNT, "3")) == 1) {
                        exercisesPref.setSummary("Critical alarm will go off immediately");
                    } else {
                        int tempInt = Integer.parseInt(prefs.getString(KEY_PREF_HIGH_ALARM_COUNT, "3")) - 1;
                        exercisesPref.setSummary("The alarm will go off " + String.valueOf(tempInt) + " times\nbefore going critical");
                        setFlag = true;
                    }
                }

                if ( key.equals(KEY_PREF_DEXTENDER_NAME)) {
                    String acctInfo[]=prefs.getString(key,"not set/no pwd").split("/");
                    if(acctInfo.length != 2) {
                        exercisesPref.setSummary("username: not set correctly");
                        Toast toast = Toast.makeText(getActivity(), "Username/password not set correctly", Toast.LENGTH_SHORT);
                        toast.show();
                    }
                    else {
                        exercisesPref.setSummary("username: " + acctInfo[0]);
                        setFlag = true;
                    }
                }
                //-----------------------------------------------------------------
                // DEFAULT if we didn't set it above
                //-----------------------------------------------------------------
                if (!setFlag) {
                    exercisesPref.setSummary(prefs.getString(key, ""));
                }
            }
        }

        //-----------------------------------------------------------------------------------------
        // Method: onResume
        // This is a Mike Special. I couldn't find anything meaningful on the web re: showing values
        //-----------------------------------------------------------------------------------------
        @Override
        public void onResume() {

            String newValue;
            super.onResume();
            getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

            Preference p2 = findPreference(KEY_PREF_UID);
            if(p2 != null) p2.setSummary(prefs.getString(KEY_PREF_UID, ""));

            p2 = findPreference(KEY_PREF_HARD_LOW);
            if(p2 != null) p2.setSummary(prefs.getString(KEY_PREF_HARD_LOW,"Hard low bg value not set"));

            p2 = findPreference(KEY_PREF_LOW);
            if(p2 != null) {
                if( (Integer.parseInt(prefs.getString(KEY_PREF_LOW, "-1")) == -1) ||
                    (Integer.parseInt(prefs.getString(KEY_PREF_LOW, "0")) == 0) )
                {
                    p2.setSummary("Off");
                }
                else {
                    p2.setSummary(prefs.getString(KEY_PREF_LOW, "Low bg value not set"));
                }
            }

            p2 = findPreference(KEY_PREF_HIGH);
            if(p2 != null) {
                if (Integer.parseInt(prefs.getString(KEY_PREF_HIGH, "1000")) == 1000) {
                    p2.setSummary("Off");
                }
                else {
                    p2.setSummary(prefs.getString(KEY_PREF_HIGH, "High  bg value not set"));
                }
            }

            p2 = findPreference(KEY_PREF_HIGH_ALARM_COUNT);
            if(p2 != null) {
                //Log.d("preferences", "here 1");
                if (Integer.parseInt(prefs.getString(KEY_PREF_HIGH_ALARM_COUNT, "3")) == 1) {
                    p2.setSummary("Critical alarm will go off immediately");
                }
                else {
                    int tempInt=Integer.parseInt(prefs.getString(KEY_PREF_HIGH_ALARM_COUNT, "3"))-1;
                    p2.setSummary("The alarm will go off " + String.valueOf(tempInt) + " times\nbefore going critical");
                }
            }

            p2 = findPreference(KEY_PREF_TARGET_HIGH);
            if(p2 != null) {
                    p2.setSummary(prefs.getString(KEY_PREF_TARGET_HIGH, "180"));
            }

            p2 = findPreference(KEY_PREF_TARGET_LOW);
            if(p2 != null) {
                p2.setSummary(prefs.getString(KEY_PREF_TARGET_LOW, "70"));
            }


            p2 = findPreference(KEY_PREF_INTERVAL);
            if(p2 != null) {
                newValue = prefs.getString(KEY_PREF_INTERVAL,"5") + " minute(s)";
                p2.setSummary(newValue);
            }

            p2 = findPreference(KEY_PREF_LOW_SNOOZE_INTERVAL);
            if(p2 != null)  {
                newValue = prefs.getString(KEY_PREF_LOW_SNOOZE_INTERVAL,"5") + " Minute(s)";
                p2.setSummary(newValue);
            }

            p2 = findPreference(KEY_PREF_HIGH_SNOOZE_INTERVAL);
            if(p2 != null)  {
                newValue = prefs.getString(KEY_PREF_HIGH_SNOOZE_INTERVAL,"5") + " Minute(s)";
                p2.setSummary(newValue);
            }


            //---- service snooze
            p2 = findPreference(KEY_PREF_SERVICE_SNOOZE_INTERVAL);
            if(p2 != null)  {
                String tmpStr = prefs.getString(KEY_PREF_SERVICE_SNOOZE_INTERVAL,"0");
                if (tmpStr.equals("0")) {
                    newValue = "Do not alert";
                }
                else {
                    newValue = prefs.getString(KEY_PREF_SERVICE_SNOOZE_INTERVAL,"5") + " Minute(s)";
                }
                p2.setSummary(newValue);
            }

            p2 = findPreference(KEY_PREF_SUS_HIGH);
            if(p2 != null)  {
                newValue = "If the BG is lower than the HIGH, but higher than " + prefs.getString(KEY_PREF_SUS_HIGH,"this ...");
                p2.setSummary(newValue);
            }

            p2 = findPreference(KEY_PREF_SUS_INTERVAL);
            if(p2 != null)  {
                newValue = "...and it's been high for " + prefs.getString(KEY_PREF_SUS_INTERVAL, "this many") + " minutes...";
                p2.setSummary(newValue);
            }

            p2 = findPreference(KEY_PREF_TONE_PLAY_TIME);
            if(p2 != null)  {
                newValue = prefs.getString(KEY_PREF_TONE_PLAY_TIME, "10") + " seconds";
                p2.setSummary(newValue);
            }

            p2 = findPreference(KEY_PREF_DATA_ERROR_SNOOZE);
            if(p2 != null)  {
                newValue = prefs.getString(KEY_PREF_DATA_ERROR_SNOOZE, "5") +
                        " minutes before raising data error alert\nNOTE: This is a repeating alert";
                p2.setSummary(newValue);
            }

            p2 = findPreference(KEY_PREF_DEXTENDER_NAME);
            if (p2 != null) {
                String acctInfo[] = prefs.getString(KEY_PREF_DEXTENDER_NAME, "not set/no pwd").split("/");
                if (acctInfo.length != 2) {
                    p2.setSummary("username: not set correctly");
                    Toast toast = Toast.makeText(getActivity(), "Username/password not set correctly", Toast.LENGTH_SHORT);
                    toast.show();
                }
                else {
                    p2.setSummary("username: " + acctInfo[0]);
                }
            }


        }

        //--------------------------------------------------------------------------
        // Method:  onPause
        // Actions: default actions
        //--------------------------------------------------------------------------
        @Override
        public void onPause() {
            getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
            super.onPause();
        }
    }


    public static void setAlarm(Context ctx) {
        //===============================================================
        // Clear the alarm just in case we get hosed up
        //===============================================================
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        int minutes = Integer.parseInt(prefs.getString("pref_refresh_interval", "5"));

        // setup the alarm manager
        AlarmManager alarmManager = (AlarmManager) ctx.getSystemService(ALARM_SERVICE);

        // setup the intent to do stuff with the service
        //Intent serviceIntent = new Intent(this, MyService.class);

        //Log.d("MyActivity", "Setting the pending intent to the MyReceiver class" );
        Intent i = new Intent(ctx, MyReceiver.class);

        // Was 'getService' - Changed to 'getBroadcast'
        PendingIntent pendingIntent;
        pendingIntent = PendingIntent.getBroadcast(ctx, 0, i, 0);

        // Kill any stragglers (if any )
        alarmManager.cancel(pendingIntent);
        //-----------------------------------------------------------
        // set the alarm
        // If minutes > 0, then set the alarm
        // NOTE: MyReceiver will handle any calls at this point
        //-----------------------------------------------------------
        if (minutes > 0) {
            Toast toast = Toast.makeText(ctx, "System wake-up set to every " + minutes + " minutes", Toast.LENGTH_SHORT);
            toast.show();

            alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + minutes * 60 * 1000, minutes * 60 * 1000, pendingIntent);
        }
    }
}
