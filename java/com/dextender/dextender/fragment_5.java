package com.dextender.dextender;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;


//===============================================================================================
// Created by livolsi on 1/24/2014.
//===============================================================================================
public class fragment_5 extends Fragment {

    ArrayAdapter listAdapter;
    ListView listView;
    BluetoothAdapter btAdapter;
    Set<BluetoothDevice> deviceArray;
    ArrayList<String> pairedDevices;
    IntentFilter filter;
    BroadcastReceiver receiver;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_5, container, false);
        init();
        if(btAdapter == null) {
            Toast toast = Toast.makeText(getActivity(), "No bluetooth detected", Toast.LENGTH_SHORT);
            toast.show();
        }
        else {
            if(!btAdapter.isEnabled()) {
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(intent,1);

            }
        }

        return v;
    }

    private void init() {
        //ListView lv = (ListView) ListView.findViewById(R.id.listview);                              // Name of the view in fragment_4.xml

        listAdapter=new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1,0);
        listView.setAdapter(listAdapter);
        btAdapter=BluetoothAdapter.getDefaultAdapter();
        pairedDevices = new ArrayList<String>();
        filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);


        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action =  intent.getAction();
                if(BluetoothDevice.ACTION_FOUND  == action) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    listAdapter.add(device.getName()+"\n"+ device.getAddress());
                }
            }

        };
        //registerReceiver(receiver, filter);

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == Activity.RESULT_CANCELED) {
            Toast toast = Toast.makeText(getActivity(), "Bluetooth must be enabled", Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    private void getPairedDevices(){
        deviceArray=btAdapter.getBondedDevices();
        if(deviceArray.size() > 0 ) {
            for(BluetoothDevice device:deviceArray){
                //listAdapter.add(device.getName()+"\n" + device.getAddress());
                pairedDevices.add(device.getName());
            }
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
