package com.dextender.dextender;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.ListView;


//===========================================================================
// Created by livolsi on 1/24/2014.
//===========================================================================
public class fragment_4 extends Fragment implements View.OnClickListener {

    Integer MAX_LOG_ITEMS=40;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_4, container, false);       // Inflate this fragment

        ImageButton frag4Refresh    = (ImageButton) v.findViewById(R.id.frag4_refreshButton);
        frag4Refresh.setOnClickListener(this);

        //-------------------------------------------------------------------
        // Only going to display 40 items. That "40" really should be a var
        // Keep this initialization.. this was making me nuts !!! If one of the values
        // in the array was null, then I was getting a nullpointerexception.. on the
        // superGetView.. so
        //-------------------------------------------------------------------
        int i;
        final String[] serviceMsg = new String[MAX_LOG_ITEMS];                                                 // array to hold messages
        for (i=0; i < MAX_LOG_ITEMS; i++) {
            serviceMsg[i] = " ";
        }
        //-------------------------------------------------------------------
        // DATABASE !!! - Fetch records from the database
        //-------------------------------------------------------------------
        MyDatabase myDb = new MyDatabase(getActivity());                                           // open database class
        int dbRecords=0;
        try {
            myDb.open();
            dbRecords=myDb.getLogData(serviceMsg, System.currentTimeMillis() / 1000);                          // fill the array with messages that we have in table "log"
        } catch (Exception e) {
            e.printStackTrace();
        }

        int allocate=1;
        if(dbRecords > 0) {
            // NEW
            allocate=dbRecords;
        }

        final MyRowStructure[] rowStruct = new MyRowStructure[allocate];

        //----------------------------------------------------------------------
        // If there's no record in the database, instead of a null record
        // put something in slot 0 (1st record)
        //----------------------------------------------------------------------
        // Commented out because this sections core dumps if it's  a new system.
        if ( dbRecords == 0 ) {
           rowStruct[0] = new MyRowStructure();
           rowStruct[0].thisRow(R.mipmap.ok,  "---", "Begin Log");
        }
        else {
            for(i=0; i < dbRecords; i++) {
                if(serviceMsg[i] != null) {
                    String rec[]=serviceMsg[i].split("\\|");
                    rowStruct[i] = new MyRowStructure();
                    switch (Integer.parseInt(rec[0])) {
                        case 1:
                            rowStruct[i].thisRow(R.mipmap.red_button, rec[1], rec[2]);
                            break;
                        case 2:
                            rowStruct[i].thisRow(R.mipmap.down, rec[1], rec[2]);
                            break;
                        case 3:
                            rowStruct[i].thisRow(R.mipmap.up, rec[1], rec[2]);
                            break;
                        case 4:
                            rowStruct[i].thisRow(R.mipmap.green_button, rec[1], rec[2]);
                            break;
                        case 5:
                            rowStruct[i].thisRow(R.mipmap.yellow_button, rec[1], rec[2]);
                            break;
                        case 6:
                            rowStruct[i].thisRow(R.mipmap.nilow, rec[1], rec[2]);
                            break;
                    }
                }
            }
        }

        //--------------------------------------------------------------------
        // Find the listview
        // An Adapter is a bridge between the view (listview) and the data
        // we bind the adapter to the view via the setadapter method.
        //--------------------------------------------------------------------
        // NEW - next two lines
        ListAdapter listAdapter = new MyCustomAdapter(getActivity(), rowStruct );
        ListView lv = (ListView) v.findViewById(R.id.listview4);
        lv.setAdapter(listAdapter);

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
