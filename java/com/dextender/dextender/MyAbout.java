package com.dextender.dextender;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;

//==============================================================
// https://www.youtube.com/watch?v=-zGS_zrL0rY
// Author       : The new boston - Tutorials 11 through 17
// Modifications: So very slight by Mike LiVolsi
// Created by livolsi on 1/6/2015.
//
// Purpose      : Flash a welcome screen, play a little song..
//                then show the real screens
//
//==============================================================
public class MyAbout extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getActionBar();
        if(actionBar != null) actionBar.hide();
        setContentView(R.layout.fragment_about);
    }

    @Override
    protected void onPause() {
        super.onPause();
        finish();
    }
}
