package com.bumbumapps.mynotes.Methods;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Toast;


import es.dmoral.toasty.Toasty;
import com.bumbumapps.mynotes.Constant.Constant;
import com.bumbumapps.mynotes.R;
import com.bumbumapps.mynotes.SharedPref.Setting;
import com.bumbumapps.mynotes.entities.Note;
import com.bumbumapps.mynotes.listeners.InterAdListener;


public class Methods {

    private Context context;
    private InterAdListener interAdListener;

    public Methods(Context context) {
        this.context = context;
    }

    public Methods(Context context, InterAdListener interAdListener) {
        this.context = context;
        this.interAdListener = interAdListener;
        loadad();
    }

    public void showSnackBar(String message, String type) {
        if (type.equals("success")){
            Toasty.success(context, message, Toast.LENGTH_SHORT, true).show();
        }else {
            Toasty.error(context, message, Toast.LENGTH_SHORT, true).show();
        }
    }


    public boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }


    private void loadad() {
        // Interstitials are handled by AdsHelper (UMP consent + frequency cap).
    }


    public void showInter(final int pos, final Note note, final String type) {
            interAdListener.onClick(pos, note, type);

    }

}