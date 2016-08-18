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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.achartengine.GraphicalView;

//========================================================================
// Class: Fragment 2
// Purpose: The purpose of this class is to show the second tabbed screen
//
// Classes Called: MyDatabase (get)
//                 MyScatterGraph (put)
// This class calls the USB class and grabs info from the DB and passes
// it to the graphing class
//========================================================================
public class fragment_2 extends Fragment implements View.OnClickListener {

    int hoursBack=12;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        SharedPreferences prefs   = PreferenceManager.getDefaultSharedPreferences(getActivity());
        MyTools           myTools = new MyTools();

        //------------------------------------------------------
        // DB Stuff
        //------------------------------------------------------
        long[] longX = new long[288];                                                               //  12 readings in an hour 24 hours in a day
        int[]  intY  = new int[288];

        //------------------------------------------------------
        // Moving average stuff
        //------------------------------------------------------
        int[]  averageY  = new int[288];                                                            // Moving average - Complete array
        int[]  tempY     = new int[14];                                                             // our working array
        int avgDivisor=0;                                                                           // how many values to divide totalBg by
        int totalBg=0;                                                                              // THe sum of the 12 values (also when we subtract)

        //-----------------------------------
        // used for the bottom of the screen
        //-----------------------------------
        int  lowestBg=999;
        int  highestBg=0;
        long bgSum=0;
        long lowestBgTime=0;
        long highestBgTime=0;


        //Long   offsetId;                                                                            // Used to display the BG at the top of the chart
        String bgRecords = "-|-|-";                                                                 //  " " "
        int recordCount=0;

        MyDatabase myDb    = new MyDatabase(getActivity());                                        // instantiate the class
        try {
            myDb.open();
            recordCount = myDb.getBgDataAsArrays(longX, intY, (System.currentTimeMillis() / 1000) - (60*60*hoursBack));   // get the values from the database
            for(int i=0; i < recordCount; i++) {
                if(intY[i] < lowestBg) {                                                            // initially set to 0, then anything smaller will reset the value
                    lowestBg = intY[i];
                    lowestBgTime = longX[i];
                }
                if(intY[i] > highestBg) {                                                           // initially set to 0, then anything larger will set the value higher
                    highestBg=intY[i];
                    highestBgTime = longX[i];
                }
                bgSum = bgSum + intY[i];                                                            // Total the BG's.. will be used to calculate average
                //--------------------------------------------------
                // As we loop through, calculate the moving average
                //--------------------------------------------------

                if(i>11) {
                    totalBg -= tempY[i % 12];                                                       // subtract from total the old slot value
                    --avgDivisor;                                                                   // take away from the divisor
                }
                tempY[i % 12] = intY[i];                                                            // assign to the new slot

                avgDivisor++;                                                                       // Increment the number we will divide with (usually 6)
                totalBg+=tempY[i%12];                                                               // Add the new value to the total

                averageY[i]=totalBg/avgDivisor;                                                     // set the moving average array
                //Log.d("Fragment2", "averageY--> i=" + i + " Average=" + averageY[i]);

                //--------------------------------------------------
                // END moving average calculations
                //--------------------------------------------------
            }
            bgRecords = myDb.getBgData(15);                                                         // now the you have the offset, this will be part of your predicate

        }
        catch (Exception e) {
            e.printStackTrace();
        }

        //-----------------------------------------------------------------
        // Get preferences so we know how to draw the yellow and red bar
        //----------------------------------------------------------------

        int warnHigh   = Integer.parseInt(prefs.getString("listHigh", "180"));                        // What is the high warning - as string then int
        int warnLow    = Integer.parseInt(prefs.getString("listLow", "70"));                          // What is low warning - as string then int
        int smartHigh  = Integer.parseInt(prefs.getString("sustainedListHigh","170"));
        boolean a1cFlag = prefs.getBoolean("pref_a1c", false);

        //----------------------------------------------------------------
        // Graph the results
        //------------------------------------------------------
        MyScatterGraph sgraph = new MyScatterGraph();
        GraphicalView gView;

        //-----------------------------------------------
        // Call the getview class in file MyScatterGraph
        //-----------------------------------------------
        gView = sgraph.getView(getActivity(),recordCount, hoursBack, longX, intY, averageY, warnHigh,
                warnLow,
                prefs.getBoolean("pref_smartLimit", true),
                prefs.getBoolean("pref_averageGraph", false),
                smartHigh);

        //----------------------------------------------------------------
        View v = inflater.inflate(R.layout.fragment_2, container, false);                           // Inflate this fragment
        LinearLayout layout = (LinearLayout) v.findViewById(R.id.chart);                            // This basically creates a container to hold the graph

        TextView chartMsg   = (TextView)  v.findViewById(R.id.frag2chartmsg);                        // The BG value that we display on top of the chart
        ImageView chartImg  = (ImageView) v.findViewById(R.id.frag2bgIcon);
        TextView lowBgMsg   = (TextView)  v.findViewById(R.id.frag2LowBg);
        TextView lowBgTime  = (TextView)  v.findViewById(R.id.frag2TimeLowBg);
        TextView highBgMsg  = (TextView)  v.findViewById(R.id.frag2HighBg);
        TextView highBgTime = (TextView)  v.findViewById(R.id.frag2TimeHighBg);
        TextView avgBgMsg   = (TextView)  v.findViewById(R.id.frag2AvgBg);
        TextView a1cTitle   = (TextView)  v.findViewById(R.id.frag2A1C_title);
        TextView a1cValue   = (TextView)  v.findViewById(R.id.frag2A1C);

        if( (gView != null) && (bgSum > 0) ) {
            lowBgMsg.setText(String.valueOf(lowestBg));
            lowBgTime.setText(myTools.epoch2FmtTime(lowestBgTime, "hh:mm a"));
            highBgMsg.setText(String.valueOf(highestBg));
            highBgTime.setText(myTools.epoch2FmtTime(highestBgTime, "hh:mm a"));
            avgBgMsg.setText(String.valueOf(bgSum / recordCount));
            if(a1cFlag) {
                a1cTitle.setText(getString(R.string.frag2Label4));
                a1cValue.setText( String.valueOf( (bgSum/recordCount+46.7)/28.7 ) );
            }
            else {
                a1cTitle.setText("");
                a1cValue.setText("");
            }

        }

        if( (gView != null) && (recordCount > 0) )  {
            if(bgRecords != null) {
                String[] recordPiece = bgRecords.split("\\|");                                      // Split the record which is delimited with the pipe
                String calcColor=myTools.bgToColor(Integer.parseInt(recordPiece[1]),
                        warnLow,
                        warnHigh);
                chartMsg.setTextColor(Color.parseColor(calcColor));
                if(Integer.parseInt(recordPiece[1])< 40) {
                    chartMsg.setText(getString(R.string.low));
                }
                else {
                    chartMsg.setText(recordPiece[1]);
                }

                switch (Integer.parseInt(recordPiece[2])) {
                    case 0:
                        chartImg.setImageResource(R.mipmap.s0);
                        break;
                    case 10:
                        chartImg.setImageResource(R.mipmap.s10);
                        break;
                    case 1:
                        chartImg.setImageResource(R.mipmap.s90);
                        break;
                    case 2:
                        chartImg.setImageResource(R.mipmap.s110);
                        break;
                    case 3:
                        chartImg.setImageResource(R.mipmap.s135);
                        break;
                    case 4:
                        chartImg.setImageResource(R.mipmap.s180);
                        break;
                    case 5:
                        chartImg.setImageResource(R.mipmap.s225);
                        break;
                    case 6:
                        chartImg.setImageResource(R.mipmap.s250);
                        break;
                    case 7:
                        chartImg.setImageResource(R.mipmap.s270);
                        break;
                    default:
                        chartImg.setImageResource(R.mipmap.s0);
                        break;
                }
            }
            else {
                chartMsg.setText("---");
            }

            layout.addView(gView);
        }
        else {
            chartMsg.setText(getString(R.string.frag2_novalueMsg));
        }

        Button b3 = (Button) v.findViewById(R.id.button3Hour);
        b3.setOnClickListener(this);

        Button b6 = (Button) v.findViewById(R.id.button6Hour);
        b6.setOnClickListener(this);

        Button b12 = (Button) v.findViewById(R.id.button12Hour);
        b12.setOnClickListener(this);

        Button b24 = (Button) v.findViewById(R.id.button24Hour);
        b24.setOnClickListener(this);

        myDb.close();
        return v;
    }



    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button3Hour:
                hoursBack=3;
                refresh();
                break;
            case R.id.button6Hour:
                hoursBack=6;
                refresh();
                break;
            case R.id.button12Hour:
                hoursBack=12;
                refresh();
                break;
            case R.id.button24Hour:
                hoursBack=24;
                refresh();
                break;
        }
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
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(r, new IntentFilter("TAG_REFRESH"));
    }

    private class MyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            refresh();
        }
    }

}

