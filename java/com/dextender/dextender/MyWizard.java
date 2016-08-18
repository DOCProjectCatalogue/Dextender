package com.dextender.dextender;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import com.dextender.dextender.R;

//==============================================================
// NOTE:  This is just a template. Would like to recreate the 
//        registration of the follow app. 
//
// https://www.youtube.com/watch?v=-zGS_zrL0rY
// Author       : The new boston - Tutorials 11 through 17
// Modifications: So very slight by Mike LiVolsi
// Created by livolsi on 1/6/2015.
//
// Purpose      : 
//
//
//==============================================================
public class MyWizard extends Activity  implements View.OnClickListener{

    Button   nextButton;
    EditText UrlEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getActionBar();
        if(actionBar != null) actionBar.hide();
        setContentView(R.layout.wizard);


//        ImageButton bgImgTrend      = (ImageButton) view.findViewById(R.id.bgImgTrend) ;
          nextButton   = (Button) findViewById(R.id.wizard_submitButton) ;
          UrlEdit      = (EditText) findViewById(R.id.wizard_Url) ;

        nextButton.setOnClickListener(this);

    }

    @Override
    protected void onPause() {
        super.onPause();
        finish();
    }

    @Override
    public void onClick(View v) {
        setupWizard2();
    }


    void setupWizard2() {
        Integer pos=UrlEdit.getText().toString().lastIndexOf('/');
        String foo=UrlEdit.toString().substring(pos);
    }
}
