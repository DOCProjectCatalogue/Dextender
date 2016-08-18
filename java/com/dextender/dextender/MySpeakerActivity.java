package com.dextender.dextender;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;


import java.util.HashMap;
import java.util.Locale;

//===============================================================
// Fired up by the service - This needs to be an activity
//===============================================================
public class MySpeakerActivity extends Activity {

    TextToSpeech tts;

    private String bgValue;
    private String trendValue;
    private String politeValue;
    private String otherStuff;
    private String freeform;

    //==================================================================
    // This method automatically gets called first
    //==================================================================
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();

        if (extras != null) {
            politeValue = extras.getString("polite");
            bgValue     = extras.getString("bgValue");
            trendValue  = extras.getString("trendValue");
            otherStuff  = extras.getString("misc");
            freeform    = extras.getString("freeform");
        }

        tts = new TextToSpeech(getBaseContext(), new TextToSpeech.OnInitListener() {

            HashMap<String, String> map = new HashMap<String, String>();
            int timesSpoken=0;
            int maxSpeakingFrags=5;


            @Override
            public void onInit(int status) {                                                         // <--- ON INIT

                if (status == TextToSpeech.SUCCESS) {

                    // Log.d("Speaker", "Status == SUCCESS");
                    tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {


                        //==================================================
                        // NOT IN ANY DOCUMENTATION !!!
                        // NO tutorials discuss this, the Android Docs don't
                        // discuss this. Wasted countless hours on this !
                        //
                        // After each 'speak', the 'onDone' gets called
                        // so if you do: tts.playSilence
                        // that counts as a cycle and "onDone" gets called.
                        // WOW .. 
                        // EOR (End of Rant)
                        //===================================================
                        @Override
                        public void onDone(String utteranceId) {
                            if (utteranceId.equals("utteranceId")) {

                                timesSpoken++;
                                //Log.d("Say", "We said something number" + timesSpoken);

                                if (timesSpoken == maxSpeakingFrags) {
                                    //if (tts != null) {
                                       tts.stop();
                                       tts.shutdown();
                                    //}
                                    onDestroy();
                                }
                            }
                        }

                        @Override
                        public void onError(String utteranceId) {
                        }

                        @Override
                        public void onStart(String utteranceId) {

                            // Log.d("SPEAKER", "ON START");
                        }
                    });


                    tts.setLanguage(Locale.US);
                    map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "utteranceId");

                    if(freeform != null) {
                        maxSpeakingFrags = 2;
                        tts.playSilence(1000, TextToSpeech.QUEUE_FLUSH, map);
                        tts.speak(freeform, TextToSpeech.QUEUE_ADD, null); // Nothing to say
                        tts.playSilence(500, TextToSpeech.QUEUE_ADD, map);
                    }
                    else {

                        if (bgValue.equals("0")) {
                            maxSpeakingFrags = 2;
                            tts.playSilence(1000, TextToSpeech.QUEUE_FLUSH, map);
                            tts.speak("there is no current blood glucose reading", TextToSpeech.QUEUE_ADD, null); // Nothing to say
                            tts.playSilence(500, TextToSpeech.QUEUE_ADD, map);
                        } else {
                            maxSpeakingFrags = 5;

                            tts.playSilence(1000, TextToSpeech.QUEUE_FLUSH, map);
                            //tts.playSilentUtterance(1000,TextToSpeech.QUEUE_FLUSH,"");            // API 21
                            if (politeValue.equals("yes")) {
                                tts.speak("the blood glucose level is", TextToSpeech.QUEUE_ADD, null); // oooh.. fancy talk
                                maxSpeakingFrags = 6;
                            }
                            // Log.d("SPEAKER", "ABOUT TO SPEAK");
                            tts.speak(bgValue, TextToSpeech.QUEUE_ADD, map);
                            tts.playSilence(300, TextToSpeech.QUEUE_ADD, map);
                            tts.speak(trendValue, TextToSpeech.QUEUE_ADD, map);
                            tts.playSilence(500, TextToSpeech.QUEUE_ADD, map);
                            if (!otherStuff.equals("--")) {
                                maxSpeakingFrags = 7;
                                tts.speak("at " + otherStuff, TextToSpeech.QUEUE_ADD, map);
                            }
                        }
                    }
                }
            }
    });


    }


    @Override
    protected void onPause(){
        super.onPause();
    }


    @Override
    protected void onStart(){
       super.onStart();


    }

    @Override
    protected void onResume() {
        super.onResume();
        finish();               // NEW FOR API 23
    }


    @Override
    protected void onDestroy() {
        //Log.d("SPEAKER", "ON DESTROYER");

        super.onDestroy();
        finish();
    }


    @Override
    protected  void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }
}