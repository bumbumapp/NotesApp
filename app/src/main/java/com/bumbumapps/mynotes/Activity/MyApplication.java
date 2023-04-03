package com.bumbumapps.mynotes.Activity;

import android.app.Application;
import android.content.Context;
import android.os.StrictMode;

import androidx.multidex.MultiDex;


import com.google.android.gms.ads.MobileAds;

import com.bumbumapps.mynotes.R;
import com.bumbumapps.mynotes.SharedPref.Setting;
import com.bumbumapps.mynotes.SharedPref.SharedPref;

public class MyApplication extends Application {

    private SharedPref sharedPref;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sharedPref = new SharedPref(this);
        if (sharedPref.getNightMode()) {
            Setting.Dark_Mode = true;
        } else {
            Setting.Dark_Mode = false;
        }

        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        // Enable verbose OneSignal logging to debug issues if needed.



    }

}