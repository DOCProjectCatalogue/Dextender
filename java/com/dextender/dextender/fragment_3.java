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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;


//===============================================================================================
// Created by livolsi on 1/24/2014.
//===============================================================================================
public class fragment_3 extends Fragment implements View.OnClickListener {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {


        MyTools    tools        = new MyTools();                                                // Generic tools class
        View v = inflater.inflate(R.layout.fragment_3, container, false);

        //--------------------------------------------------------
        // Get the respective ID's from the XML screens
        //--------------------------------------------------------
        TextView serverMessage = (TextView) v.findViewById(R.id.frag3serverMsg);                    // Server message (if any)

        TextView trendTitle    = (TextView) v.findViewById(R.id.frag3TrendTitle);
        TextView trendInfo     = (TextView) v.findViewById(R.id.frag3TrendInfo);

        TextView subscriberId  = (TextView) v.findViewById(R.id.subscriberId);                        // What is called Web Post/Receive
        TextView dextenderName = (TextView) v.findViewById(R.id.dextenderName);
        TextView dextWebStatus = (TextView) v.findViewById(R.id.dextWebStatus);
        //-------------------------------------------------------------
        // Fragment 3 column 3 values to be replaced by actual values
        //-------------------------------------------------------------
        ImageView netRealStat     = (ImageView) v.findViewById(R.id.netRealStat);
        ImageView webRealStat     = (ImageView) v.findViewById(R.id.webRealStat);
        ImageView serviceRealStat = (ImageView) v.findViewById(R.id.serviceRealStat);
        ImageView accountRealStat = (ImageView) v.findViewById(R.id.accountRealStat);

        TextView lowSetting      = (TextView) v.findViewById(R.id.frag3LowSetting);
        TextView highSetting     = (TextView) v.findViewById(R.id.frag3HighSetting);
        TextView targetHigh      = (TextView) v.findViewById(R.id.frag3TargetHigh);
        TextView targetLow       = (TextView) v.findViewById(R.id.frag3TargetLow);

        ImageButton frag3Refresh    = (ImageButton) v.findViewById(R.id.frag3_refreshButton);
        frag3Refresh.setOnClickListener(this);

        //--------------------------------------------------------
        // Get the values from the preferences
        //--------------------------------------------------------
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        MyDatabase myDb = new MyDatabase(getActivity());


        try {
            myDb.open();
        } catch (Exception e) {
            e.printStackTrace();
        }
        //--------------------------------------------------------------------------
        // Any server messages ?
        //--------------------------------------------------------------------------
        String tempString;
        try {
             tempString = myDb.getServerMessage();
             if (tempString != null) {
                serverMessage.setText(tempString);
            }
        }
        catch(Exception e) {    
            e.printStackTrace();
        }

        int dbRc;



        //-----------------------------------------------------------------------
        if (prefs.getBoolean("prefsvc", true)) {                                                    // service is turned on

            serviceRealStat.setImageResource(R.mipmap.power_on);                                    // GREEN

            switch(myDb.getServiceStatus("Internet Connection")) {
                case -1:
                    netRealStat.setImageResource(R.mipmap.power_off);                   // BLACK
                    break;
                case 0:
                    netRealStat.setImageResource(R.mipmap.power_on);                    // GREEN
                    break;
                case 1:
                    netRealStat.setImageResource(R.mipmap.power_err);                   // RED
                    break;
                case 2:
                    netRealStat.setImageResource(R.mipmap.power_unknown);               // CLEAR
                    break;
                case 11:
                    netRealStat.setImageResource(R.mipmap.power_pause);                 // YELLOW
            }

            //-----------------------------------------------------------------
            // Common to both
            //-----------------------------------------------------------------
            if (prefs.getBoolean("pref_cloud", true)) {
                boolean cloudStatus=false;
                try {
                    dbRc = myDb.getServiceStatus("Web Handler");
                    switch (dbRc) {
                        case -1:
                            webRealStat.setImageResource(R.mipmap.power_off);                       // BLACK
                            break;
                        case 0:
                            webRealStat.setImageResource(R.mipmap.power_on);                        // GREEN
                            cloudStatus=true;
                            break;
                        case 1:
                            webRealStat.setImageResource(R.mipmap.power_err);                       // RED
                            break;
                        case 2:
                            webRealStat.setImageResource(R.mipmap.power_unknown);                   // RED
                            break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if(cloudStatus) {

                    switch(myDb.getServiceStatus("Cloud Account")) {
                        case -1:
                            accountRealStat.setImageResource(R.mipmap.power_off);                   // BLACK
                            break;
                        case 0:
                            accountRealStat.setImageResource(R.mipmap.power_on);                    // GREEN
                            break;
                        case 1:
                            accountRealStat.setImageResource(R.mipmap.power_err);                   // RED
                            break;
                        case 2:
                            accountRealStat.setImageResource(R.mipmap.power_unknown);               // YELLOW
                            break;
                    }
                }
                else {
                    accountRealStat.setImageResource(R.mipmap.power_unknown);
                }
            }
            else {
                webRealStat.setImageResource(R.mipmap.power_unknown);                                       // CIRCLE
                accountRealStat.setImageResource(R.mipmap.power_unknown);
            }
        }
        else {
            serviceRealStat.setImageResource(R.mipmap.power_off);
            netRealStat.setImageResource(R.mipmap.power_unknown);
            webRealStat.setImageResource(R.mipmap.power_unknown);
            accountRealStat.setImageResource(R.mipmap.power_unknown);
        }



        //------------------------------------------------------------
        // Trending info
        //------------------------------------------------------------
        if(prefs.getBoolean("pref_trend_alert", false)) {

            trendTitle.setText(R.string.frag3_trendTitle);
            switch (Integer.parseInt(prefs.getString("pref_trendType", "1"))) {
                case 1: trendInfo.setText(R.string.frag3_rising);
                    break;
                case -11: trendInfo.setText(R.string.frag3_falling);
                    break;
                default:  trendInfo.setText(R.string.frag3_trendUnknown);
                    break;
            }
        }

        //------------------------------------------------------------
        // Current low and high settings
        //------------------------------------------------------------
        if (Integer.parseInt(prefs.getString("listLow", "70")) == -1) lowSetting.setText(R.string.off);
        else                                                          lowSetting.setText(prefs.getString("listLow", "70"));

        if (Integer.parseInt(prefs.getString("listHigh", "180")) == 1000) highSetting.setText(R.string.off);
        else                                                              highSetting.setText(prefs.getString("listHigh", "180"));

        if (prefs.getBoolean("pref_startTrailingHigh",false)) {
            targetHigh.setText(prefs.getString("followListHigh", "180"));
        }
        else {
            targetHigh.setText(R.string.off);
        }

        if (prefs.getBoolean("pref_startTrailingLow",false)) {
            targetLow.setText(prefs.getString("followListLow", "70"));
        }
        else {
            targetLow.setText(R.string.off);
        }
        //------------------------------------------------------------
        // need the database for the following
        //------------------------------------------------------------
        String rec[] = myDb.getAccountInfo().split("\\|");
        if( rec[0] == null) {
            subscriberId.setText(R.string.frag3_noAccount);
            dextWebStatus.setText("---");
        }
        else {

            if(rec[0].length() > 12) {
                String tmpString;
                tmpString=rec[0].substring(0,12) + "...";
                subscriberId.setText(tmpString);
            }
            else {
                subscriberId.setText(rec[0]);
            }


            if( (rec[3].equals("Active")) || (rec[3].equals("Registered-active")) ) {
                dextWebStatus.setTextColor(Color.WHITE);
                dextWebStatus.setText(rec[3]);
            }
            else {
                dextWebStatus.setText("---");
                dextWebStatus.setTextColor(Color.YELLOW);
            }

        }

        //----------------------------------------------------------------
        // split the username/password in preference to only show the user
        //----------------------------------------------------------------
        String tmpString="";
        rec = prefs.getString("pref_dextenderName", "---").split("/");
        if(rec.length != 2 ){
            dextenderName.setText("---");
        }
        else {
            if(rec[0].length() > 12) {
                tmpString = rec[0].substring(0, 12) + "...";
                dextenderName.setText(tmpString);
            }
            else {
                dextenderName.setText(rec[0]);
            }
        }
        myDb.close();
        return v;
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

    private class MyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            refresh();
        }
    }
}
