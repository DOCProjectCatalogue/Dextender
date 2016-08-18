package com.dextender.dextender;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.File;
import java.net.URI;

//==============================================================
// https://www.youtube.com/watch?v=-zGS_zrL0rY
//
// Author       : The new boston - Tutorials 11 through 17
// Modifications: So very slight by Mike LiVolsi
// Created by livolsi on 1/6/2015.
//
// Purpose      : Flash a welcome screen, play a little song..
//                Do some light housekeeping
//                   - Check connectivity
//                   - Check preference settings, etc..
//                Then show the main activity
//
//==============================================================
public class MySplash extends Activity {

    final  int CRITICAL_LOW_ALARM=1;

    MediaPlayer mySound;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        final MyTools    myTools = new MyTools();
        MyDatabase myDb = new MyDatabase(getApplicationContext());

        setContentView(R.layout.splash);
        LinearLayout relative = (LinearLayout) findViewById(R.id.splashScreen);
        switch(Integer.parseInt(prefs.getString("pref_backgroundImages", "0"))) {
            case 0: relative.setBackgroundResource(0);
                break;
            case 1: relative.setBackgroundResource(R.mipmap.bg_space);
                break;
            case 2: relative.setBackgroundResource(R.mipmap.space_stars);
                break;
            case 3: relative.setBackgroundResource(R.mipmap.space_nebula);
                break;
            case 4: relative.setBackgroundResource(R.mipmap.space_milkyway);
                break;
            case 5: relative.setBackgroundResource(R.mipmap.summer_night);
                break;
            case 6: relative.setBackgroundResource(R.mipmap.trakai_lake);
                break;
            case 7: relative.setBackgroundResource(R.mipmap.summer_night);
                break;
            case 8: relative.setBackgroundResource(R.mipmap.nature_gloomy_trees);
                break;
            case 9: relative.setBackgroundResource(R.mipmap.fireworks);
                break;
            case 10: relative.setBackgroundResource(R.mipmap.halloween1);
                break;
            case 11: relative.setBackgroundResource(R.mipmap.halloween2);
                break;
            case 12: relative.setBackgroundResource(R.mipmap.halloween3);
                break;
            case 13: relative.setBackgroundResource(R.mipmap.halloween_scary);
                break;
            case 14: relative.setBackgroundResource(R.mipmap.christmas);
                break;
            case 15: relative.setBackgroundResource(R.mipmap.christmas2);
                break;
            case 16: relative.setBackgroundResource(R.mipmap.christmas3);
                break;
            case 17: relative.setBackgroundResource(R.mipmap.xmas_lights);
                break;
            case 18: relative.setBackgroundResource(R.mipmap.blue_ornament);
                break;
            case 19: relative.setBackgroundResource(R.mipmap.snowman);
                break;
            case 20: relative.setBackgroundResource(R.mipmap.hanukkah);
                break;
            case 21: relative.setBackgroundResource(R.mipmap.mountain_sky);
                break;
            case 22: relative.setBackgroundResource(R.mipmap.winter_trees);
                break;
            default: relative.setBackgroundResource(0);
                break;

        }


        Long epochAsLong=(long) 0;
        try {
            myDb.open();
            epochAsLong=myDb.getLastServiceRunTime("Background Service");
            myDb.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }



        ActionBar actionBar = getActionBar();
        if(actionBar != null) {
            actionBar.hide();
        }

        //--------------------------------------------------------
        // Set the spinner and the and progress bar
        //--------------------------------------------------------
        ProgressBar spinner;
        spinner = (ProgressBar) findViewById(R.id.pbHeaderProgress);
        spinner.setVisibility(View.VISIBLE);


        Toast toast;
        //==================================================
        // Make a dextender subdirectory
        //==================================================

        File[] Dirs = ContextCompat.getExternalFilesDirs(this, null);
        //Log.d("SPLASH", "dir 1" + Dirs[0] + "Dir 2 " + Dirs[1]);
        File folder = new File(Dirs[0] + File.separator + "authenticate");
        if (!folder.exists()) {
            if(folder.mkdir()) {
                toast = Toast.makeText(this, "Directory created", Toast.LENGTH_SHORT);
                toast.show();
            }
            else {
                toast = Toast.makeText(this, "Create directory FAILED !!!" + folder, Toast.LENGTH_SHORT);
                toast.show();
            }
        }


        //-------------------------------------------------------------------
        // Get our tune preference , if yes
        //
        //-------------------------------------------------------------------
        boolean introTune = prefs.getBoolean("pref_intro_tune", true);
        if(introTune) {
            mySound = MediaPlayer.create(this, R.raw.blade_small);
            mySound.start();
        }

        Thread timer = new Thread() {
             public void run() {
                 try {
                     sleep(3000);
                 }
                 catch (InterruptedException e) {
                     e.printStackTrace();
                 }
                 finally {
                     Intent openMainActivity =  new Intent("com.dextender.dextender.MYACTIVITY");
                     startActivity(openMainActivity);
                 }
             }
        };

        timer.start();
        //---------------------------------------------------------
        //  .5    = 30 seconds (half a minute)
        // 5      = 5 minutes after that
        // this   = context
        // true  = don't override if alarm is already set
        // epocAsLong = last time we ran
        //---------------------------------------------------------
        myTools.setNextRun(.5, 5, this, true, epochAsLong);

        checkAccountStatus();
        clearCriticalDb();
    }

    @Override
    protected void onPause() {
        super.onPause();
        finish();
    }




    public void checkAccountStatus() {

        final MyDatabase myDb = new MyDatabase(getBaseContext());
        final MyHttpPost http = new MyHttpPost();
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        URI webUrl;

        try {
            myDb.open();
            String rec[] = myDb.getAccountInfo().split("\\|");
            if (!rec[0].equals("0")) {
                String acctInfo[] = prefs.getString("pref_dextenderName","null/null").split("/");


                webUrl = new URI(getString(R.string.http_dextender_url_v02)
                        + "?followerId=" + rec[0].trim()
                        + "&followerAppId=" + rec[1].trim()
                        + "&followerPwd=" + rec[2].trim()
                        + "&extid=" + acctInfo[0]
                        + "&extpwd=" + acctInfo[1]);


                // Call the URL and update our local database with the returned value
                myDb.updateAccountValidateDextender(http.validateDextenderAccount(webUrl));

            }
            myDb.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }


    }

    public void clearCriticalDb() {

        Toast toast = Toast.makeText(this, "Clearing cache", Toast.LENGTH_SHORT);
        toast.show();
        final MyDatabase myDb    = new MyDatabase(getBaseContext());
        boolean dbRc=false;
        try {
            myDb.open();
            dbRc=true;
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        if(dbRc) {
            myDb.clearAlarm(CRITICAL_LOW_ALARM);
            toast = Toast.makeText(this, "Clearing critical alarms", Toast.LENGTH_LONG);
            toast.show();
            myDb.close();
        }
        else {
            toast = Toast.makeText(this, "Could not clear alarm from db", Toast.LENGTH_LONG);
            toast.show();

        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }
}
