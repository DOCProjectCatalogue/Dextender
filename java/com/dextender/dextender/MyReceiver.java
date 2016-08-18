package com.dextender.dextender;

//----------------------------------------------------------------------------------------------
// Class      : MyReceiver
// Author     : http://it-ride.blogspot.com/2010/10/android-implementing-notification.html
// Modified by: Mike LiVolsi
// Date       : October 2014
//
// Purpose    : Original purpose was to fire up the service when the USB was attached, as reflected
//              in the manifest. Now, handles the 5 (or so) minutes of when a service is started by
//              the alarm manager
// 
// Called by  : system broadcast 
//
// NOTE       : Clean up the USB stuff !!
//----------------------------------------------------------------------------------------------


import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbManager;
import android.os.SystemClock;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;
import android.widget.Toast;

public class MyReceiver extends WakefulBroadcastReceiver {

    @Override
    public void onReceive (Context context, Intent intent) {

    if(intent.getAction() != null) {
        if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
            Toast.makeText(context, "usb was disconneced", Toast.LENGTH_LONG).show();
            context.stopService(intent);
        }
    }
    else {
            //Toast.makeText(context, "Starting service @ " + SystemClock.elapsedRealtime(), Toast.LENGTH_LONG).show();
            Intent service = new Intent(context, MyService.class);
            startWakefulService(context, service);
        }
    }
}
