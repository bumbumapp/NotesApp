package com.bumbumapps.mynotes.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.bumbumapps.mynotes.Activity.LockScreen.LockScreenActivity;
import com.bumbumapps.mynotes.R;
import com.bumbumapps.mynotes.SharedPref.Setting;
import com.bumbumapps.mynotes.SharedPref.SharedPref;

public class SplashActivity extends AppCompatActivity {

    private static int SPLASH_TIME_OUT = 2000;
    private SharedPref sharedPref;
    private CardView Logo;
    private Animation animation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        sharedPref = new SharedPref(this);
        if (sharedPref.getNightMode()) {
            Setting.Dark_Mode = true;
            setTheme(R.style.AppTheme2);
        } else {
            Setting.Dark_Mode = false;
            setTheme(R.style.AppTheme);
        }
        if (sharedPref.getIn_Code()) {
            Setting.in_code = true;
        } else {
            Setting.in_code = false;
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        animation = AnimationUtils.loadAnimation(this, R.anim.smalltobig);
        Logo = findViewById(R.id.logo);
        Logo.startAnimation(animation);
        loadSettings();
    }

    public void loadSettings() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (Setting.in_code){
                    Intent main = new Intent(SplashActivity.this, LockScreenActivity.class);
                    startActivity(main);
                    finish();
                }else {
                    Intent main = new Intent(SplashActivity.this, MainActivity.class);
                    startActivity(main);
                    finish();
                }
            }
        },SPLASH_TIME_OUT);
    }
}