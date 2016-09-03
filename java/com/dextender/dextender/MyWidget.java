package com.dextender.dextender;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.widget.RemoteViews;
import android.widget.Toast;

public class MyWidget extends AppWidgetProvider {

    //private static final String ACTION_CLICK = "ACTION_CLICK";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        //----------------------------------------------
        // Call the classes
        //----------------------------------------------
        MyTools    myTools  = new MyTools();
        MyDatabase myDb    = new MyDatabase(context);                                              // Call the database class
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);


        String dbBgVal="";                                                                          // display of bg
        int dbBgTrend=0;                                                                            // display trend
        int dbBgNumValue=0;
        String bgRecords="";                                                                        // the line from the database
        String lastRunDate="--:--:--";

        //---------------------------------------------------------
        // Get the real data from the database
        //---------------------------------------------------------
        boolean dbRc=false;
        try {
            myDb.open();                                                                            // open the database
            //Long sequenceId = myDb.getSlotPrevVal("dexBg");                                       // get the offset from table 'offset'
            bgRecords = myDb.getBgData(30);                                                         // now the you have the offset, this will be part of your predicate
            dbRc=true;
        }
        catch (Exception e) {
            e.printStackTrace();
            myDb.close();
        }

        Integer prefLowBg = Integer.parseInt(prefs.getString("listLow", "70"));
        Integer prefHighBg = Integer.parseInt(prefs.getString("listHigh", "180"));

        if(dbRc) {
            //---------------------------------------------------------------------------------------
            // Start processing the record. First, we'll split it up since it's delimited by pipes
            //---------------------------------------------------------------------------------------
            if (bgRecords != null) {

                String recordPiece[] = bgRecords.split("\\|");                                      // Split the record into it's respective parts
                dbBgVal = recordPiece[1];
                dbBgNumValue=Integer.parseInt(recordPiece[1]);

                //-------------------------------------------------------
                // Format the display date from epoch to readable time
                //-------------------------------------------------------
                lastRunDate = myTools.epoch2FmtTime(Long.parseLong(recordPiece[3]), "MMM d yyyy h:mm a");
                dbBgTrend = Integer.parseInt(recordPiece[2]);
            }



            //-----------------------------------------------------------------------------------
            // This I'm not so sure fragment_about. We need to talk to ALL widgets that might be running
            // Get all ids
            //-----------------------------------------------------------------------------------
            ComponentName thisWidget = new ComponentName(context, MyWidget.class);
            int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
            for (int widgetId : allWidgetIds) {

                RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget);

                String tempString=prefs.getString("pref_widgetBackground", "clear_widget");
                int id = context.getResources().getIdentifier(tempString, "drawable", context.getPackageName());
                remoteViews.setInt(R.id.layout, "setBackgroundResource", id);

                // Set the text

                if (bgRecords != null) {
                    remoteViews.setTextViewText(R.id.widget_usbTime, lastRunDate);
                    remoteViews.setTextColor(R.id.bgVal, Color.parseColor(myTools.bgToColor(dbBgNumValue, prefLowBg, prefHighBg)));
                    remoteViews.setTextViewText(R.id.widget_bg, dbBgVal);


                    // set the trend (do it as an image)
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
                } else {
                    long epochAsLong = myDb.getLastRunTime();
                    if(epochAsLong != 0) {
                        remoteViews.setTextViewText(R.id.widget_usbTime, myTools.now());
                    }
                    else {
                        remoteViews.setTextViewText(R.id.widget_usbTime, "--:--:--");
                    }
                    remoteViews.setTextViewText(R.id.widget_bg, "");
                    remoteViews.setImageViewResource(R.id.widget_trend, R.mipmap.s0);
                }


                Intent intent = new Intent(context, MyActivity.class);

                PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

/* leave*/      remoteViews.setOnClickPendingIntent(R.id.layout, pendingIntent);
/* leave*/      appWidgetManager.updateAppWidget(widgetId, remoteViews);

            }
            myDb.close();
        }
    }



    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
        Toast.makeText(context, "Widget removed", Toast.LENGTH_SHORT).show();
    }


}