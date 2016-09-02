package com.dextender.dextender;

import java.io.File;
import java.util.Locale;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;

import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Environment;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.v13.app.FragmentPagerAdapter;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

//============================================================================
// Class: MyActivity
// Author: MLV
//
// Purpose: The main enchilada.
//          After the splash screen is displayed, this guy is called.
//          It sets up the whole tabbing infrastructure, and then calls in
//          the alarmmanager, which sets up the service. In some tutorials
//          they show the alarm manager being called in the 'onresume' which
//          is kind of wrong in two ways.
//          A) That 'stuff' really belongs in a function (which I did)
//          B) It should be called in the onCreate method, so it's not a
//             constant churn to the alarm manager.
//
// Notes  : There's probably a better way of making sure that once the alarm
//          manager is set, that we can ignore it.. but I can see why we need
//          to call it (if we're doing software updates). So.. leave for now as is
//============================================================================
public class MyActivity extends Activity implements ActionBar.TabListener {

    final static int NUMBER_OF_TABS=4;
     /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v13.app.FragmentStatePagerAdapter}.
     */
    SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);


        //------------------------------------------------------------
        // Check Permissions starting in API 23 (Marshmallow
        //------------------------------------------------------------
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            checkExtenderPermissions();
        }

        //------------------------------------------------------------
        // Variants of this exist in splash and in preference
        // Modifications are made to pass the correct var type
        //------------------------------------------------------------
        File[] Dirs = ContextCompat.getExternalFilesDirs(this, null);
        String fileName   = Dirs[0] + File.separator;

        if(!(Thread.getDefaultUncaughtExceptionHandler() instanceof MyExceptionHandler)) {

            Thread.setDefaultUncaughtExceptionHandler(new MyExceptionHandler(fileName));
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        ViewPager relative = (ViewPager) findViewById(R.id.pager);
        switch(Integer.parseInt(prefs.getString("pref_backgroundImages", "0"))) {
            case 0: relative.setBackgroundResource(0);
                break;
            case 1: relative.setBackgroundResource(R.mipmap.bg_space);
                break;
            case 2: relative.setBackgroundResource(R.mipmap.bg_space_stars);
                break;
            case 3: relative.setBackgroundResource(R.mipmap.bg_space_nebula);
                break;
            case 4: relative.setBackgroundResource(R.mipmap.bg_space_milkyway);
                break;
            case 5: relative.setBackgroundResource(R.mipmap.bg_summer_night);
                break;
            case 6: relative.setBackgroundResource(R.mipmap.bg_trakai_lake);
                break;
            case 7: relative.setBackgroundResource(R.mipmap.bg_summer_night);
                break;
            case 8: relative.setBackgroundResource(R.mipmap.bg_nature_gloomy_trees);
                break;
            case 9: relative.setBackgroundResource(R.mipmap.bg_fireworks);
                break;
            case 10: relative.setBackgroundResource(R.mipmap.bg_halloween1);
                break;
            case 11: relative.setBackgroundResource(R.mipmap.bg_halloween2);
                break;
            case 12: relative.setBackgroundResource(R.mipmap.bg_halloween3);
                break;
            case 13: relative.setBackgroundResource(R.mipmap.bg_halloween_scary);
                break;
            case 14: relative.setBackgroundResource(R.mipmap.bg_christmas);
                break;
            case 15: relative.setBackgroundResource(R.mipmap.bg_christmas2);
                break;
            case 16: relative.setBackgroundResource(R.mipmap.bg_christmas3);
                break;
            case 17: relative.setBackgroundResource(R.mipmap.bg_xmas_lights);
                break;
            case 18: relative.setBackgroundResource(R.mipmap.bg_blue_ornament);
                break;
            case 19: relative.setBackgroundResource(R.mipmap.bg_snowman);
                break;
            case 20: relative.setBackgroundResource(R.mipmap.bg_hanukkah);
                break;
            case 21: relative.setBackgroundResource(R.mipmap.bg_mountain_sky);
                break;
            case 22: relative.setBackgroundResource(R.mipmap.bg_winter_trees);
                break;
            default: relative.setBackgroundResource(0);
                break;

        }

        //===========================================================
        // Maybe future use
        //===========================================================
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);

            //Log.d("MAIN", action);
            //Log.d("MAIN", type);
            if(sharedText != null) {
                Log.i("MAIN", sharedText);
            }

        }
        //----------------------------------------------------------------------------------
        // Since API 11, you can't make http calls on the main thread. since the calls are
        // lightweight (and the background program was written by me in C++), we can override
        // this restriction with the following
        //----------------------------------------------------------------------------------
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);


        // Set up the action bar.
        final ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        // When swiping between different sections, select the corresponding
        // tab. We can also use ActionBar.Tab#select() to do this if we have
        // a reference to the Tab.
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
            }
        });

        // For each of the sections in the app, add a tab to the action bar.
        for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
            // Create a tab with text corresponding to the page title defined by
            // the adapter. Also specify this Activity object, which implements
            // the TabListener interface, as the callback (listener) for when
            // this tab is selected.
            actionBar.addTab(
                    actionBar.newTab()
                            .setText(mSectionsPagerAdapter.getPageTitle(i))
                            .setTabListener(this));
        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.my, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        // MLV - Action bar clicks are different than tab clicks ...
        //       ie. this will get invoked when clicking "settings"
        int id = item.getItemId();
        switch(id)
        {
         case R.id.action_settings:
             Intent p = new Intent("com.dextender.dextender.PREFERENCE");
             startActivity(p);
             return true;
         default:
              return super.onOptionsItemSelected(item);
        }
    }


    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        // When the given tab is selected, switch to the corresponding page in
        // the ViewPager.
        // MLV - Ok.. this is interesting. If you swipe or click the first tab
        //       the tab.getPosition returns 'i0'.. all the way to 3 (which is how many tabs we have)
        // Log.d("MyActivity", "onTabSelected — get item number " + tab.getPosition());
        mViewPager.setCurrentItem(tab.getPosition());

        //--------------------- NEW !! --------------------
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        switch (tab.getPosition()) {
            case 0:
                Intent i0 = new Intent("TAG_REFRESH");
                lbm.sendBroadcast(i0);
                break;
            case 1:
                Intent i1 = new Intent("TAG_REFRESH");
                lbm.sendBroadcast(i1);
                break;
            case 2:
                Intent i2 = new Intent("TAG_REFRESH");
                lbm.sendBroadcast(i2);
                break;
            case 3:
                Intent i3 = new Intent("TAG_REFRESH");
                lbm.sendBroadcast(i3);
                break;
        }

    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        // Constructor (similar to c++)
        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }


        @Override
        public Fragment getItem(int position) {
            Fragment fragment=null;
            switch(position)
            {
              case 0: fragment= new fragment_1();
                      break;
              case 1: fragment= new fragment_2();
                      break;
              case 2: fragment= new fragment_3();
                      break;
              case 3: fragment= new fragment_4();
                    break;
            }
            return (fragment);
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            //return PlaceholderFragment.newInstance(position + 1);
        }

        //============================================================================
        // Method : GetCount
        // Author : MLV
        // Purpose: Return the number of tabs that we will display.
        // NOTE   : When I originally created this page, my android skills were nill
        //          so I followed the tutorials. One of the Interesting things, as I
        //          write this, is "why?" I believe this guys is only being called
        //          from within the same class. Nevertheless, I'm not going to touch it
        //          More comments if I do..
        //============================================================================
        @Override
        public int getCount() {
            // Show 4 total pages.
            return NUMBER_OF_TABS;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                    return getString(R.string.title_section1);
                case 1:
                    return getString(R.string.title_section2).toUpperCase(l);
                case 2:
                    return getString(R.string.title_section3).toUpperCase(l);
                case 3:
                    return getString(R.string.title_section4).toUpperCase(l);
            }
            return null;
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

            View rootView = inflater.inflate(R.layout.fragment_my, container, false);
            return rootView;
        }
    }

    //----------------------------------------------------------------
    // This is for the wakeup service
    //----------------------------------------------------------------
    public void onResume() {

        super.onResume();
    }


    //----------------------------------------------------------------
    // Starting with API 23, you need to request permissions. It's
    // no longer cool just to have it in the manifest.. ughhh..
    //----------------------------------------------------------------
    @TargetApi(23)
    public void checkExtenderPermissions() {

        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (shouldShowRequestPermissionRationale(
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
                // Explain to the user why we need to read the contacts
            }

            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);

        }

    }


/*
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startActivityForResult(i, RESULT_LOAD_IMAGE);
                    Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

                } else {

                    Toast.makeText(this, "Cannot access gallery. Try again.", Toast.LENGTH_SHORT).show();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }

    }
*/

}
