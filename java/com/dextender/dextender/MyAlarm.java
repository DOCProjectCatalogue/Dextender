package com.dextender.dextender;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import java.io.IOException;

//=================================================================================================
// Class:  MyAlarm
// Author: MLV
// This is a specific activity.
// When you hit a high or low, then a sound will play on time in the MyService..
// However, when you hit a hard low, the sound will continue to play until there's a confirmation
// from the user
//
// MAJOR HAVOC
// The Sony has a "stamina" mode that will clobber these types of activities. Unless the app
// is put in an excemption list, this will cause nothing but heartbreak
//
//=================================================================================================
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Toast;


public class MyAlarm extends Activity implements OnSeekBarChangeListener {

    //declare variables
    private MediaPlayer mediaPlayer;
    final MyDatabase myDb    = new MyDatabase(this);
    final static short LOW_ALARM = 2;
    final static short HIGH_ALARM = 3;
    final static short CRITICAL_LOW_ALARM=1;

    int sleepCount;                          // This guy seems to get called no matter what..


    SeekBar seekbar1;
    TextView result;
    TextView bgValue;
    int value;


    //set constructor for when activity is first created
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);


        Bundle extras = getIntent().getExtras();
        String sBgValue="--";
        String sAlarmType=String.valueOf(CRITICAL_LOW_ALARM);

        if (extras != null) {
            sBgValue   = extras.getString("bgValue");
            sAlarmType = extras.getString("alarmType");
        }

        boolean dbRc=false;
        try {
            myDb.open();
            dbRc=true;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        if(!dbRc) {
            finish();
        }


        if( myDb.getAlarmCount(CRITICAL_LOW_ALARM) > 0 ) {                                                    // alarm already running
            myDb.close();
            finish();

        }
        else {
            myDb.updateAlarm(CRITICAL_LOW_ALARM, 1);
            myDb.close();

            //================================================================
            //  Do we want to override the sound settings ?
            //================================================================
            boolean override = prefs.getBoolean("pref_override", true);
            String soundFile;
            switch(Integer.parseInt(sAlarmType)) {
                case 1:
                    soundFile=prefs.getString("pref_hardLowAlarmTone", null);                       // get the low sound file
                    break;
                case 2:
                    soundFile=prefs.getString("lowAlarmTone", null);                                // get the low sound file
                    break;
                case 3:
                    soundFile=prefs.getString("highAlarmTone", null);                               // get the low sound file
                    break;
                default:
                    soundFile=prefs.getString("pref_hardLowAlarmTone", null);                       // get the low sound file
                    break;
            }


            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN |                       // Let's wake up the screen
                            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                            WindowManager.LayoutParams.FLAG_FULLSCREEN |
                            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION  |                                  // hide nav bar
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |                                  // hide nav bar
                            View.SYSTEM_UI_FLAG_FULLSCREEN |                                        // hide status bar
                            View.SYSTEM_UI_FLAG_IMMERSIVE);


            ActionBar actionBar = getActionBar();                                                   // hide the action bar
            if (actionBar != null) {
                actionBar.hide();
            }
            setContentView(R.layout.fragment_slider);                                               // display the screen


            //=====================================================================
            // Setup the seekbar
            seekbar1 = (SeekBar)  findViewById(R.id.sbBar);
            result   = (TextView) findViewById(R.id.tvResult);
            bgValue  = (TextView) findViewById(R.id.slider_bg);
            bgValue.setText(sBgValue);

            playSound(this, getAlarmURI(soundFile), override);

            //set change listener
            seekbar1.setOnSeekBarChangeListener(this);
            //=====================================================================

        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        value = progress;
        if(progress < 66) {
            result.setText("Progress: " + progress + "%");
        }
        else {
            result.setTextColor(Color.parseColor("#00FF00"));
            result.setText("Acknowledged");

            mediaPlayer.stop();
            try {
                myDb.open();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            myDb.clearAlarm(CRITICAL_LOW_ALARM);
            myDb.close();
            finish();
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    //------------------------------------------------------
    // Override the back press button so it does nothing
    //------------------------------------------------------
    @Override
    public void onBackPressed() {
    }


    private void playSound(Context context, Uri audioFile, boolean overRide){

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        //---------------------------------------------------------------
        // If vibrate is true in our preferences, then vibrate the device
        //---------------------------------------------------------------
        boolean vibrate         = prefs.getBoolean("pref_vibrate", true);
        if (vibrate) {
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            v.vibrate(1000);
        }

        mediaPlayer= new MediaPlayer();
        try {
            mediaPlayer.setDataSource(context, audioFile);
            final AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if( (audioManager.getStreamVolume(AudioManager.STREAM_ALARM) != 0) || (overRide) ){
                int currentVolume = audioManager.getStreamVolume(audioManager.STREAM_ALARM);
                int maxVol=audioManager.getStreamMaxVolume (audioManager.STREAM_ALARM);
                audioManager.setStreamVolume(audioManager.STREAM_ALARM, maxVol, 0);
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
                mediaPlayer.setLooping(true);
                mediaPlayer.prepare();
                mediaPlayer.start();
                audioManager.setStreamVolume(audioManager.STREAM_ALARM, currentVolume, 0);          //  set volume back to original setting

            }
        }
        catch(IOException e) {
            e.printStackTrace();
        }
    }

    private Uri getAlarmURI(String lowSoundFile){

        Uri alertUri;

        if(lowSoundFile == null) {
            alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alertUri == null) {
                alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                if (alertUri == null) {
                    alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
                }
            }
        }
        else {
            alertUri = Uri.parse(lowSoundFile);
        }
        return alertUri;
    }


    //------------------------------------------------------------------------
    // OnPause
    // The reason I'm adding the "onDestroy" is basically as a catch all
    // If the screen disappears for whatever reason, then what happens
    // is the alarm continuously plays. This appears to be an issue with phones
    // that have a physical home button, and then the only way to turn off
    // the alarm is to either kill the app, or reboot the phone.
    // I haven't tested the power button
    // Anyway I know this will get called if the focus leaves the screen
    // which means, kill the alarming.
    // Not exactly what I intended, but it's probably a good safety measure
    //------------------------------------------------------------------------
    // is playing. It seems like the only
    @Override
    protected void onPause(){


    //    mediaPlayer.stop();
    //    finish();
        super.onPause();
    }

    @Override
    protected void onStop(){

        if(sleepCount > 0) {
            onDestroy();
        }
        sleepCount++;
        super.onStop();
    }


    @Override
    protected void onDestroy() {
        //Log.d("MyAlarm", "In onDestroy");
        if(mediaPlayer != null) {
            mediaPlayer.stop();
        }
        try {
            myDb.open();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        myDb.clearAlarm(CRITICAL_LOW_ALARM);
        myDb.close();
        finish();
        super.onDestroy();
    }
}