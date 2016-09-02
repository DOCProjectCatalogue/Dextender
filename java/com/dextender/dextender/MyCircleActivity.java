package com.dextender.dextender;


import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class MyCircleActivity extends Activity {

    int bgRangeType=1;
    int bgSetting=70;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.seek_circle);


        TextView tCircleTitle = (TextView) findViewById(R.id.circleTitle);
        //-----------------------------------------------------------------------------------
        // MLV Added
        // Depending on the range type (high or low)
        // we are going to disable chunks of the circle
        // meaning: if this is a "low" (0) range, only allow the circle to display between 60-100
        //          if a "high" (1) range, then circle can only display between 110-400
        //------------------------------------------------------------------------------------
        Bundle extras = getIntent().getExtras();
        String circleTitle="";

        if (extras != null) {
            try {
                bgRangeType = Integer.parseInt(extras.getString("bgRangeType"));
                bgSetting = Integer.parseInt(extras.getString("bgSetting"));
                //Toast.makeText(this, "hello " + bgSetting, Toast.LENGTH_LONG).show();
                //bgSetting   = Integer.parseInt(extras.getString("bgSetting"));
                circleTitle = extras.getString("circleTitle");
            }
            catch (Exception e) {
                bgRangeType=0;
                circleTitle = "Unknown event - Exception";
            }
        }


        tCircleTitle.setText(circleTitle);
        // end MLV

       // Toast.makeText(this, "hello " + bgSetting, Toast.LENGTH_LONG).show();
        SeekCircle seekCircle = (SeekCircle)findViewById(R.id.seekCircle);
        seekCircle.setProgress(bgSetting);


        seekCircle.setOnSeekCircleChangeListener(new SeekCircle.OnSeekCircleChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekCircle seekCircle)
            {}

            @Override
            public void onStartTrackingTouch(SeekCircle seekCircle)
            {}

            @Override
            public void onProgressChanged(SeekCircle seekCircle, int progress, boolean fromUser)
            {
                updateText();
            }
        });


        updateText();
    }

    private void updateText()
    {

        SeekCircle seekCircle = (SeekCircle)findViewById(R.id.seekCircle);
        TextView textProgress = (TextView)findViewById(R.id.textProgress);

        if (textProgress != null && seekCircle != null)
        {
            int progress = seekCircle.getProgress();
            //---------------------------------------------
            // MLV Added
            // NOTE: Original only set the 'textProgress'
            //---------------------------------------------
            switch (bgRangeType) {
                case 1:
                case 5:
                    if(progress < 60) {
                        progress = 0;
                        seekCircle.setProgress(progress);
                        textProgress.setText(R.string.Off);
                    }
                    else {
                        if (progress > 100) {
                            progress = 100;
                            seekCircle.setProgress(progress);
                        }
                        else {
                            if(progress % 5 != 0) {
                                progress = 5 * (Math.round(progress / 5));
                                seekCircle.setProgress(progress);
                            }

                        }
                        textProgress.setText(String.valueOf(progress));
                    }

                    break;
                case 2:
                case 3:
                case 4:
                    if(progress < 110) {
                        progress=110;
                        seekCircle.setProgress(progress);
                    }
                    else {
                        if(progress % 5 != 0) {
                            progress = 5 * (Math.round(progress / 5));
                            seekCircle.setProgress(progress);
                        }
                    }
                    textProgress.setText(String.valueOf(progress));
                    break;
                default:
                    textProgress.setText(String.valueOf(progress));
            }
        }
    }

    public void submitIt(View view) {

        SeekCircle seekCircle = (SeekCircle)findViewById(R.id.seekCircle);

        //Toast.makeText(this, "Limit set to " + String.valueOf(seekCircle.getProgress()), Toast.LENGTH_SHORT).show();

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor1 = settings.edit();

        switch(bgRangeType){
            case 1:
                editor1.putString("listLow", String.valueOf(seekCircle.getProgress()));
                editor1.apply();
                break;
            case 2:
                editor1.putString("listHigh", String.valueOf(seekCircle.getProgress()));                    // if it was, we'd set it to our target bg
                editor1.apply();
                break;
            case 3:
                editor1.putString("sustainedListHigh", String.valueOf(seekCircle.getProgress()));                    // if it was, we'd set it to our target bg
                editor1.apply();
                break;
            case 4:
                editor1.putString("followListHigh", String.valueOf(seekCircle.getProgress()));                    // if it was, we'd set it to our target bg
                editor1.apply();
                break;
            case 5:
                editor1.putString("followListLow", String.valueOf(seekCircle.getProgress()));                    // if it was, we'd set it to our target bg
                editor1.apply();
                break;
        }
        Toast toast;
        toast = Toast.makeText(this, "Value set to " + String.valueOf(seekCircle.getProgress()), Toast.LENGTH_SHORT);
        toast.show();

        onDestroy();

    }


    @Override
    protected void onDestroy() {

        //   myDb.clearAlarm(CRITICAL_LOW_ALARM);
        //   myDb.close();
        finish();
        super.onDestroy();
    }

}
