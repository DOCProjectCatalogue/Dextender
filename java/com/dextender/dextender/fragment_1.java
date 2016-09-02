package com.dextender.dextender;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;


//===========================================================================================
// Created by livolsi on 1/24/2014.
//===========================================================================================
public class fragment_1 extends Fragment implements View.OnClickListener {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        //----------------------------------------------
        // Call the classes
        //----------------------------------------------
        final MyTools    myTools = new MyTools();
        MyDatabase myDb = new MyDatabase(getActivity());
        View view = inflater.inflate(R.layout.fragment_1, container, false);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        //--------------------------------------------------------
        // Get the respective ID's from the XML screens
        //--------------------------------------------------------
        TextView filler1            = (TextView)    view.findViewById(R.id.frag1Filler1);
        TextView messageTop         = (TextView)    view.findViewById(R.id.frag1Message);
        TextView messageBottom      = (TextView)    view.findViewById(R.id.frag1BottomMessage);
        TextView bgVal              = (TextView)    view.findViewById(R.id.bgVal);
        TextView lastServiceRunTime = (TextView)    view.findViewById(R.id.frag1_serviceRunTime);
        TextView serviceTimeAgo     = (TextView)    view.findViewById(R.id.frag1_runTimeAgo);
        TextView bgDexTime          = (TextView)    view.findViewById(R.id.bgDexTime);
        TextView bgScanTime         = (TextView)    view.findViewById(R.id.bgScanTime);
        TextView bgScanTimeAgo      = (TextView)    view.findViewById(R.id.bgScanTimeAgo);
        ImageButton bgImgTrend      = (ImageButton) view.findViewById(R.id.bgImgTrend);


        final boolean  serviceStatus      = prefs.getBoolean("prefsvc", false);
        final boolean  cloudStatus        = prefs.getBoolean("pref_cloud", false);


        bgImgTrend.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if((!cloudStatus) || (!serviceStatus) ){
                    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
                    SharedPreferences.Editor editor1 = settings.edit();
                    if(!cloudStatus) {
                        editor1.putBoolean("pref_cloud", true);
                    }
                    if(!serviceStatus) {
                        editor1.putBoolean("prefsvc", true);
                    }
                    Toast toast;
                    toast = Toast.makeText(getActivity(), "Service restarted", Toast.LENGTH_SHORT);
                    toast.show();
                    editor1.apply();
                    refresh();

                }

                // Set Alarm .5 = 30 seconds | 5 is default 5 minutes | context | override | last time we ran
                myTools.setNextRun(.5, 5, getActivity(), true, 0);
                return false;
            }
        });

        ImageButton frag1Refresh    = (ImageButton) view.findViewById(R.id.frag1_refreshButton);
        frag1Refresh.setOnClickListener(this);


        //---------------------------------------------------------------------------------
        // See if preferences are set to prohibit us from accessing the network
        //---------------------------------------------------------------------------------
        if ( (!serviceStatus) || (!cloudStatus) ) {
            messageTop.setBackgroundColor(getResources().getColor(R.color.red));
            filler1.setBackgroundColor(getResources().getColor(R.color.red));
            messageTop.setText(getString(R.string.frag1_serviceWarning));
            messageBottom.setText(getString(R.string.frag1_serviceHint));
        }


        //==========================
        // APP PLAY AREA
        //==========================

        // ERASE WHEN DONE  - Used to test something and dont want to wait for 5 minutes

        //int[] bgArray= new int[6];
        //int[] projectedValues = new int[3];
        //bgArray[0]=90;                       // most recent
        //bgArray[1]=100;                       // most recent
        //bgArray[2]=112;
        //bgArray[3]=123;                       // oldest

        //bgArray[0]=95;                       // most recent
        //bgArray[1]=101;                       // most recent
        //bgArray[2]=108;
        //bgArray[3]=112;


        //---------------------------------------------------------------------------------
        // Get the 'offset' aka last record we got from the DB and display any new records
        //---------------------------------------------------------------------------------
        try {
            myDb.open();                                                                            // open the database

            // MyTools.projectedBgValue(bgArray, 4, projectedValues);                         // calculate the predicted BG value


            Long epochAsLong=myDb.getLastServiceRunTime("Background Service");
            lastServiceRunTime.setText(myTools.epoch2FmtTime(epochAsLong, "hh:mm:ss a"));
            serviceTimeAgo.setText(myTools.fuzzyTimeDiff(System.currentTimeMillis()/1000, epochAsLong));

            String bgRecords = myDb.getBgData(15);                                                  // Hard-coding. If we didn't get a reading from the USB in 15 minutes

            if(bgRecords != null) {

                String[] recordPiece = bgRecords.split("\\|");                                      // Split the record into it's respective parts
                String calcColor=myTools.bgToColor(Integer.parseInt(recordPiece[1]),
                                             Integer.parseInt(prefs.getString("listLow",   "70")),
                                             Integer.parseInt(prefs.getString("listHigh", "180")));

                bgVal.setTextColor(Color.parseColor(calcColor));
                if (Integer.parseInt(recordPiece[1]) < 40) {
                    //bgVal.setBackgroundColor(Color.YELLOW);
                    bgVal.setText(R.string.low);
                } else {
                    bgVal.setText(recordPiece[1]);                                                      // BG value
                }

                //-------------------------------------------------------------
                // Take the epoch date and convert into something more 'normal'
                // DexDate, subtracting the offset
                // row piece 3 is the BG date from the receiver
                // row piece 4 is when the record was locally created
                //-------------------------------------------------------------
                epochAsLong = Long.parseLong(recordPiece[3]);
                bgDexTime.setText(myTools.epoch2FmtTime(epochAsLong, "hh:mm:ss a"));

                //-------------------------------------------------------------
                // Take the epoch date and convert into something more 'normal'
                // System Scan time
                //-------------------------------------------------------------
                epochAsLong = Long.parseLong(recordPiece[4]);
                bgScanTime.setText(myTools.epoch2FmtTime(epochAsLong, "hh:mm:ss a"));
                bgScanTimeAgo.setText(myTools.fuzzyTimeDiff(System.currentTimeMillis()/1000, epochAsLong));


                switch (Integer.parseInt(recordPiece[2])) {                       // which arrow to display based on trend
                    case 0:
                        bgImgTrend.setBackgroundResource(R.mipmap.i0);
                        break;
                    case 10:
                        bgImgTrend.setBackgroundResource(R.mipmap.i10);
                        break;
                    case 1:
                        bgImgTrend.setBackgroundResource(R.mipmap.i90);
                        break;
                    case 2:
                        bgImgTrend.setBackgroundResource(R.mipmap.i110);
                        break;
                    case 3:
                        bgImgTrend.setBackgroundResource(R.mipmap.i135);
                        break;
                    case 4:
                        bgImgTrend.setBackgroundResource(R.mipmap.i180);
                        break;
                    case 5:
                        bgImgTrend.setBackgroundResource(R.mipmap.i225);
                        break;
                    case 6:
                        bgImgTrend.setBackgroundResource(R.mipmap.i250);
                        break;
                    case 7:
                        bgImgTrend.setBackgroundResource(R.mipmap.i270);
                        break;
                    default:
                        bgImgTrend.setBackgroundResource(R.mipmap.i0);
                        break;
                }
            }
            else {
                bgVal.setText("---");
                bgImgTrend.setBackgroundResource(R.mipmap.i0);
                epochAsLong = myDb.getLastRunTime();
                if(epochAsLong != 0) {
                    bgScanTime.setText(myTools.epoch2FmtTime(epochAsLong, "hh:mm:ss a"));
                }
                else {
                    bgScanTime.setText("...");
                }
            }
        } catch (Exception e) {
            //String error = e.toString();
            e.printStackTrace();
            myDb.close();
        }

        myDb.close();
        return view;
    }

    @Override
    public void onClick(View v) {
        refresh();
    }

    //==========================================================================
    // The code below is to refresh this fragment when you swipe to a different
    // fragment. This is what is pissing me off fragment_about android. You would think
    // that some screens need to be refreshed (ala windows) and yet you have
    // to come up with all this code by scouring the web, since they don't explain
    // this shit at all in their tutorials
    //==========================================================================

    MyReceiver r;

    public void refresh() {
        //This is called every time you swipe a tab. To force a refresh, you have to run the following

        Fragment        fragment = this;
        FragmentManager manager  = getActivity().getFragmentManager();
        FragmentTransaction ft = manager.beginTransaction();
                ft.detach(fragment);
                ft.attach(fragment);
                ft.commit();
    }


    public void onPause() {
        super.onPause();

        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(r);
    }

    public void onResume() {
        super.onResume();

        r = new MyReceiver();
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(r,
                new IntentFilter("TAG_REFRESH"));
    }

    public void onStart() {
        super.onStart();
    }

    private class MyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            refresh();
        }
    }


}