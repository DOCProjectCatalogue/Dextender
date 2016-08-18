package com.dextender.dextender;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;

//----------------------------------------------------------------------------------------------
// Class      : MySearchableActivity
// Author     : http://android-developers.blogspot.com/2014/10/the-fastest-route-between-voice-search.html
// Modified by: Mike LiVolsi
// Date       : March  2015
//
// Purpose    : To be able to give back the BG values via the 'ok' google command.
//
// To test    : adb shell am start -a com.google.android.gms.actions.SEARCH_ACTION -e query foo
//
// Called by  : ok google
//
// NOTE       : Most of the code is duplication from the speech code (except for the database call)
//-------------------------------------------------------------------------------------------------
public class MySearchableActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String search = getIntent().getStringExtra(SearchManager.QUERY);

        if(search.equals("latest reading")) {
            MyDatabase myDb = new MyDatabase(getApplicationContext());

            boolean dbOpenFlag = false;
            try {
                myDb.open();
                dbOpenFlag = true;
            } catch (Exception e) {
                e.printStackTrace();
                myDb.close();
            }

            //--------------------------------------
            // Get the info from the database
            //--------------------------------------
            String bgRecords = myDb.getBgData(15);                                                  // Hard-coding. If we didn't get a reading from the USB in 15 minutes

            if (bgRecords != null) {
                String[] recordPiece = bgRecords.split("\\|");                                          // Split the record into it's respective parts

                sayIt(Integer.parseInt(recordPiece[1]), Integer.parseInt(recordPiece[2]), Integer.parseInt(recordPiece[3]));
            } else {
                sayIt(0, 0, 0);                                                                     // will handle how to say it in the speech class
            }
            //--------------------------------------
            // Close the database if it's open
            //--------------------------------------
            if (dbOpenFlag) {
                myDb.close();
            }
        }
    }

    private boolean sayIt(int inBg, int inTrend, long inDexTime) {

        MyTools    tools   = new MyTools();
        //-----------------------------------------
        // Convert to strings and verbal output
        //-----------------------------------------
        String strBg=String.valueOf(inBg);
        String strTrend="--";
        String politeness="no";
        String inOtherStuff="--";

        if(inBg != 0) {

            switch (inTrend) {
                case 0:
                    strTrend = "trend unknown";
                    break;
                case 10:
                    strTrend = "question marks";
                    break;
                case 1:
                    strTrend = "double up";
                    break;
                case 2:
                    strTrend = "arrow up";
                    break;
                case 3:
                    strTrend = "angled up";
                    break;
                case 4:
                    strTrend = "level";
                    break;
                case 5:
                    strTrend = "angled down";
                    break;
                case 6:
                    strTrend = "arrow down";
                    break;
                case 7:
                    strTrend = "double down";
                    break;
                default:
                    strTrend = "trend unknown";
                    break;
            }
            politeness="yes";
            inOtherStuff = tools.epoch2FmtTime(inDexTime, "hh:mm a");
        }

        Intent openSpeakerActivity = new Intent(getApplicationContext(), MySpeakerActivity.class);
        openSpeakerActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        openSpeakerActivity.putExtra("polite", politeness);
        openSpeakerActivity.putExtra("bgValue", strBg);
        openSpeakerActivity.putExtra("trendValue", strTrend);
        openSpeakerActivity.putExtra("misc", inOtherStuff);
        startActivity(openSpeakerActivity);

        return true;
    }

}
