package com.dextender.dextender;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.SystemClock;

import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;


import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

//------------------------------------------------------------------------------------
// Class : MyTools
// Author: Mike LiVolsi
//
// Purpose: A multi-purpose class to do mundane things, like calc differences in time
//          time offsets, etc..
//-----------------------------------------------------------------------------------
public class MyTools {

    // YOU pass the format
    public String epoch2FmtTime(long argEpoch, String argFormat) {
        Date date = new Date(argEpoch * 1000L);
        DateFormat format;
        if (argFormat == null)
            format = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        else
            format = new SimpleDateFormat(argFormat, Locale.getDefault());
        format.setTimeZone(TimeZone.getDefault());
        return format.format(date);
    }

    /*public int getOffsetFromUtc() {
        TimeZone tz = TimeZone.getDefault();
        Date now = new Date();
        return tz.getOffset(now.getTime()) / 1000;
    }*/


    public String now() {
        DateFormat format = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        Date now = new Date();
        return format.format(now.getTime());
    }

    public int modulo(int m, int n) {
        int mod = m % n;
        return (mod < 0) ? mod + n : mod;
    }


    public int roundUp(int inNumber, int inDivisor) {
        if(inNumber%inDivisor==0) return(inNumber);
        else                      return(inNumber + (inDivisor - (inNumber%inDivisor)));
    }
    /*public Date epoch2Date(long argEpoch) {

        Date date = new Date(argEpoch * 1000L);
        return date;
        // DateFormat format = new SimpleDateFormat("HH:mm:ss");
        // format.setTimeZone(TimeZone.getDefault());

    }
*/

    public String fuzzyTimeDiff(long epochSeconds1, long epochSeconds2) {
        long diffInSeconds = epochSeconds1 - epochSeconds2;

        String output;

        float floatTime;

        if (diffInSeconds < 60) {
            output = diffInSeconds + " seconds ago";
        }
        else {
            if (diffInSeconds < 3600) {
                floatTime = diffInSeconds / 60;
                if(diffInSeconds%60 >=30) output = Math.round(floatTime) + ".5 minutes ago";
                else                      output = Math.round(floatTime) + " minutes ago";
            } else {
                floatTime = diffInSeconds / 3600;
                output = Math.round(floatTime) + " hours ago";
            }
        }

        return output;
    }

    //-------------------------------------------------------------------------------
    // Set the color of the font based on the BG level
    //-------------------------------------------------------------------------------
    public String bgToColor(Integer inBg, Integer inlowBg, Integer inHighBg) {
        if( inBg >= inlowBg && inBg <=inHighBg) {
            return "#ffffff";
        }
        else {
            if(inBg < inlowBg) return "#ff0000";
            else {
                if( (inBg - inHighBg) < 255 )  {
                    if((inBg - inHighBg) <= 80) {
                        if((80 - (inBg - inHighBg)) < 10)
                            return "#ff" + Integer.toHexString(255 - (inBg - inHighBg)) + "0" + Integer.toHexString(80 - (inBg - inHighBg));
                        else
                            return "#ff" + Integer.toHexString(255 - (inBg - inHighBg)) + Integer.toHexString(80 - (inBg - inHighBg));
                    }
                    else {
                        if ((255 - (inBg - inHighBg)) < 10)
                            return "#ff0" + Integer.toHexString(255 - (inBg - inHighBg)) + "80";
                        else
                            return "#ff" + Integer.toHexString(255 - (inBg - inHighBg)) + "80";
                    }
                }
                else {
                    return "#ff0000";
                }

            }
        }
    }

    //----------------------------------------------------------------------------------------------
    // Method: processCloudBgRecords
    // Author: MLV
    // Purpose: To take the string coming in from the server with client records and parse it
    //          and populate into the associated arrays
    // Arguments: inString
    //
    //----------------------------------------------------------------------------------------------
    public int processCloudBgRecords(String inString, String[] outTime, String[] outBg, String[] outTrend) {

        int j = 0;
        try {
            JSONArray jArray = new JSONArray(inString);
            for (int i = jArray.length() - 1; i >= 0; i--) {
                JSONObject jObject = jArray.getJSONObject(i);
                outTime[j] = jObject.getString("WT").substring(6,16);
                outBg[j] = jObject.getString("Value");
                outTrend[j] = jObject.getString("Trend");
                j++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return j;
    }

    //---------------------------------------------------------------------------
    // Check if we can actually ping
    //---------------------------------------------------------------------------
    public boolean isNetworkAvailable(Context inContext) {
        ConnectivityManager cm = (ConnectivityManager)  inContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return (netInfo != null && netInfo.isConnectedOrConnecting());
    }


    public boolean internetConnectionAvailable(int timeOut) {
        InetAddress inetAddress = null;
        try {
            Future<InetAddress> future = Executors.newSingleThreadExecutor().submit(new Callable<InetAddress>() {
                @Override
                public InetAddress call() {
                    try {
                        return InetAddress.getByName("google.com");
                    } catch (UnknownHostException e) {
                        return null;
                    }
                }
            });
            inetAddress = future.get(timeOut, TimeUnit.MILLISECONDS);
            future.cancel(true);
        } catch (InterruptedException | ExecutionException | TimeoutException e ) {
            e.printStackTrace();
        }
        return inetAddress!=null && !inetAddress.equals("");
    }



    public void setNextRun(double inMinutes, int inNextMinutes, Context inContext, boolean overRide, long inEpochLastTime) {

        //===============================================================
        // Clear the alarm just in case we get hosed up
        //===============================================================
        // setup the alarm manager
        AlarmManager alarmManager = (AlarmManager) inContext.getSystemService(inContext.ALARM_SERVICE);

        // setup the intent to do stuff with the service
        //Intent serviceIntent = new Intent(this, MyService.class);

        Intent i = new Intent(inContext, MyReceiver.class);

        // Was 'getService' - Changed to 'getBroadcast'
        PendingIntent pendingIntent;
        pendingIntent = PendingIntent.getBroadcast(inContext, 0, i, 0);

        // Kill any stragglers (if any ) if override is specified
        if (overRide) {
            alarmManager.cancel(pendingIntent);
        }

        boolean alarmUp = (PendingIntent.getBroadcast(inContext, 0, i, PendingIntent.FLAG_NO_CREATE) != null); // is there an alarm ?
        if ((!alarmUp) || (overRide)) {

            //------------------------------------------------------------------------
            // set the alarm
            // If minutes > 0, then set the alarm
            // NOTE: MyReceiver will handle any calls at this point
            // THE TWO LINES BELOW ARE A LITTLE DIFFERENT THAN THE SERVICE ALARM CALL
            //------------------------------------------------------------------------
            double calcSeconds;
            if(inEpochLastTime !=0) {

                if ( ((System.currentTimeMillis() / 1000) - inEpochLastTime) >= (inMinutes*60) ) {             // if last time greater than 5 minutes
                    calcSeconds = inMinutes * 60;                                                   // next run time (ie .3 *60  = 30 seconds)
                }
                else {
                    calcSeconds = 300 - ((System.currentTimeMillis() / 1000) - inEpochLastTime);    // 5 minutes - (how long ago we ran)
                }
            }
            else  {
                calcSeconds =  inMinutes * 60;
            }

            long whenToFire = SystemClock.elapsedRealtime() + (long) calcSeconds * 1000;
            long whenToFireAfterThat = inNextMinutes * 60 * 1000;

            if (calcSeconds > 0) {                                                                  // this should always be
                if (calcSeconds >= 60) {
                    Toast toast = Toast.makeText(inContext, "Next run time in " + (int) calcSeconds / 60 + " minute(s)", Toast.LENGTH_SHORT);
                    toast.show();
                } else {
                    Toast toast = Toast.makeText(inContext, "Next run time in " + (int) calcSeconds + " seconds", Toast.LENGTH_SHORT);
                    toast.show();
                }
                alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        whenToFire,
                        whenToFireAfterThat,
                        pendingIntent);
            }
        } else {
            Toast toast = Toast.makeText(inContext, "Alarm clock already set", Toast.LENGTH_SHORT);
            toast.show();
        }

    }

    //==============================================================================================
    // Method: ProjectedBgValue
    //
    //==============================================================================================
    public static void projectedBgValue( int inBgValue[], int inElementCount, int outProjectedValues[]) {
        short trend=0;
        int[] tmpArray = new int[6];               // used to analyze bg array coming in. if all the same trend, then we end up with 6
        short tmpELements=0;                       // number of elements in the tmpArray
        int[] workingBgArray = new int[6];

        final short FLAT=0;                        // trend level
        final short CHANGE=1;                  // angled up or angled down
        final short RAPID_CHANGE=2;            // double angled up or doubled angle down
        final short LINEAR=3;                      // between angled up and angled down
        final short UNKNOWN=99;                    // equivalent to question marks

        final short MINIMUM_SAMPLE_POINTS=4;

        //------------------------
        // Quick initialization
        //------------------------
        for(int i=0; i<3; i++) {
            outProjectedValues[i]=-1;
        }

        //------------------------------------------------------------------------------------
        // Step 1. Create an array that only contains elements that match the current trend
        //         In english: if one reading is down and the next 3 are up, then ignore
        //         the first reading. This gets put into an array
        //------------------------------------------------------------------------------------
        for(int i=0; i < inElementCount-1; i++) {   // loop through all the passed elements except the last (handled seperately)
            if( (inBgValue[i] >= inBgValue[i + 1]) && ((trend==0)||(trend==1)) ){
                    tmpArray[tmpELements] = inBgValue[i];
                    trend = 1;
                    tmpELements++;

            } else {
                if( (inBgValue[i] <= inBgValue[i + 1]) && ((trend==0)||(trend==-1)) ) {
                    tmpArray[tmpELements] = inBgValue[i];
                    trend = -1;
                    tmpELements++;
                }
                else {
                    i = inElementCount;
                }
            }
        }
        //-----------------------------------------------------------------------------
        // Step 1A. For the last element, we can't compare to the next, so we compare
        // to the previous (opposite logic)
        //-----------------------------------------------------------------------------
        //Log.d("Tools", "Predictive - tmpElements " + tmpELements + " inElementCount " + inElementCount);
        if( tmpELements < MINIMUM_SAMPLE_POINTS-1 ){
            return;
        }
        else {
            if ((inBgValue[tmpELements] <= inBgValue[tmpELements - 1]) && (trend == 1)) {
                tmpArray[tmpELements] = inBgValue[tmpELements];
                tmpELements++;
            } else {
                if ((inBgValue[tmpELements] >= inBgValue[tmpELements - 1]) && (trend == -1)) {
                    tmpArray[tmpELements] = inBgValue[tmpELements];
                    tmpELements++;
                }
            }
        }

        //---------------------------------------------
        // Sort
        //---------------------------------------------
        if (tmpELements < MINIMUM_SAMPLE_POINTS) {
            return;
        }
        int j=0;
        for(int i=tmpELements-1; i>=0; --i){
            workingBgArray[j]=tmpArray[i];
            j++;
        }

        //--------------------------------------------------------
        // lets find out what kind of curve we are dealing with
        //-------------------------------------------------------
                trend=0;
        int     average=0;
        int     lastBgValue=0;
        boolean deltaFlag=false;     // ignore first delta difference
        int     delta=0;             // difference between this bg and previous bg
        int     previousDelta=0;     // duh
        int     absoluteDeltaDelta;  // The absolute value of the differences between deltas (ie. if last delta was 5 and this delta was 5, then deltadelta is 0)
        short   lastCurveType=FLAT;

        for(int i=0; i<tmpELements; i++){
            average += workingBgArray[i];
            if(i!=0) {
                delta=(workingBgArray[i]-lastBgValue);
                //Log.d("MyTools", "bgvalue-->" + workingBgArray[i] + " delta = " + delta + ", last delta "+ previousDelta + " last curve type " + lastCurveType);
                if(deltaFlag) {
                    //-----------------------------------
                    // FLAT MOVEMENT
                    //-----------------------------------
                    if(Math.abs(delta) <= 2) {                                                      // Our current BG values didn't change that much
                        switch(lastCurveType) {
                            case FLAT:
                                lastCurveType = FLAT;                                               // if last was flat, the it's flat now
                                if (workingBgArray[i] == lastBgValue) trend = 0;
                                else {
                                    if (workingBgArray[i] < lastBgValue) trend = -1;
                                    else                                 trend = 1;
                                }
                                break;
                            case LINEAR:
                                lastCurveType = CHANGE;                                         // slowing down
                                trend=-1;
                                break;
                            case CHANGE:
                                lastCurveType = FLAT;                                             // no longer geometric
                                trend=-1;
                                break;
                            case RAPID_CHANGE:
                                lastCurveType = LINEAR;
                                trend=-1;
                                break;
                            default:
                                lastCurveType = UNKNOWN;
                                break;
                        }
                    }
                    else {                                                                          // we're moving, but not steady

                        absoluteDeltaDelta = Math.abs(Math.abs(delta) - Math.abs(previousDelta));
                        int deltaDelta     = Math.abs(Math.abs(delta) - Math.abs(previousDelta));
                        if(deltaDelta < 0)  trend=-1;
                        else                trend=1;
                        if( absoluteDeltaDelta <= 2) {                                              // steady increase or decrease
                            //------------------------------------------------------------------
                            // A Linear change
                            //------------------------------------------------------------------
                            switch (lastCurveType) {
                                case LINEAR:
                                    lastCurveType=LINEAR;
                                    break;
                                case RAPID_CHANGE:
                                    lastCurveType=LINEAR;
                                    break;
                                case CHANGE:
                                    lastCurveType=LINEAR;
                                    trend=0;
                                    break;
                                case FLAT:
                                    lastCurveType=LINEAR;
                                    trend=0;
                                    break;
                            }
                        }
                        else {
                            if(absoluteDeltaDelta < 5) {
                                //------------------------------------------------------------------
                                // ACCELERATING | DECELERATING
                                //------------------------------------------------------------------
                                switch (lastCurveType) {
                                    case LINEAR:
                                        lastCurveType=RAPID_CHANGE;
                                        break;
                                    case CHANGE:
                                        trend=0;
                                        lastCurveType=CHANGE;
                                        break;
                                    case RAPID_CHANGE:
                                        lastCurveType = LINEAR;
                                        break;
                                    case FLAT:
                                        lastCurveType=CHANGE;
                                        break;
                                }
                            }
                            else {
                                //------------------------------------------------------------------
                                // RAPID CHANGE
                                //------------------------------------------------------------------
                                switch (lastCurveType) {
                                    case LINEAR:
                                        lastCurveType=RAPID_CHANGE;
                                        break;
                                    case CHANGE:
                                        lastCurveType=RAPID_CHANGE;
                                        break;
                                    case RAPID_CHANGE:
                                        lastCurveType = RAPID_CHANGE;
                                        trend=0;
                                        break;
                                    case FLAT:
                                        lastCurveType=CHANGE;
                                        break;
                                }

                            }
                        }

                    }

                }
                //Log.d("MyTools", "Last curve type is now : " + lastCurveType  + " and trend is " + trend);
                previousDelta=delta;
                deltaFlag=true;
            }
                                                                                          // first value in array
            lastBgValue=workingBgArray[i];

        }
        //average=average/tmpELements;

        switch (lastCurveType) {
            case FLAT:                                                  // Done - verified
                delta=trend;
            case LINEAR:                                                 // Done - verified
                outProjectedValues[0]=lastBgValue+delta;
                outProjectedValues[1]=outProjectedValues[0]+delta;
                outProjectedValues[2]=outProjectedValues[1]+delta;
                break;
            case CHANGE:
                if(trend==1) {
                    outProjectedValues[0] = lastBgValue - (previousDelta + 1);
                    outProjectedValues[1] = outProjectedValues[0] - (previousDelta + 3);
                    outProjectedValues[2] = outProjectedValues[1] - (previousDelta + 5);
                }
                else {
                    outProjectedValues[0] = lastBgValue + (previousDelta - 1);
                    outProjectedValues[1] = outProjectedValues[0] + (previousDelta - 3);
                    outProjectedValues[2] = outProjectedValues[1] + (previousDelta - 5);
                }
                break;
            case RAPID_CHANGE:
                //-------------------------------------------------------------------------
                // Don't go crazy.. otherwise you can get exponential growth which
                // actually will exceed the bodies ability to really have BG's rise that
                // quickly. This needs experimentation
                //-------------------------------------------------------------------------
                if(trend==1) {
                    outProjectedValues[0] = lastBgValue - (previousDelta + 1);
                    outProjectedValues[1] = outProjectedValues[0] - (previousDelta + 3);
                    outProjectedValues[2] = outProjectedValues[1] - (previousDelta + 5);
                }
                else {
                    outProjectedValues[0] = lastBgValue + (previousDelta - 1);
                    outProjectedValues[1] = outProjectedValues[0] + (previousDelta - 3);
                    outProjectedValues[2] = outProjectedValues[1] + (previousDelta - 5);
                }
                break;
            default:
                break;
        }

        //Log.d("MyTools", "Projected 1 : " + outProjectedValues[0]);
        //Log.d("MyTools", "Projected 2 : " + outProjectedValues[1]);
        //Log.d("MyTools", "Projected 3 : " + outProjectedValues[2]);

    }


}