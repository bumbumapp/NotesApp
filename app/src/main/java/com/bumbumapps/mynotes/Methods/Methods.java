package com.bumbumapps.mynotes.Methods;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.ads.consent.ConsentInformation;
import com.google.ads.consent.ConsentStatus;
import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;

import es.dmoral.toasty.Toasty;
import com.bumbumapps.mynotes.Constant.Constant;
import com.bumbumapps.mynotes.R;
import com.bumbumapps.mynotes.SharedPref.Setting;
import com.bumbumapps.mynotes.entities.Note;
import com.bumbumapps.mynotes.listeners.InterAdListener;


public class Methods {

    private Context context;
    private InterAdListener interAdListener;
    private InterstitialAd interstitialAd;

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
        if (Constant.isAdmobInterAd) {
            interstitialAd = new InterstitialAd(context);
            AdRequest adRequest;
            if (ConsentInformation.getInstance(context).getConsentStatus() == ConsentStatus.PERSONALIZED) {
                adRequest = new AdRequest.Builder()
                        .build();
            } else {
                Bundle extras = new Bundle();
                extras.putString("npa", "1");
                adRequest = new AdRequest.Builder()
                        .addNetworkExtrasBundle(AdMobAdapter.class, extras)
                        .build();
            }
//            interstitialAd.setAdUnitId(context.getString(R.string.interstitia_ad_unit_id));
//            interstitialAd.loadAd(adRequest);
        }
    }


    public void showInter(final int pos, final Note note, final String type) {
        if (Constant.isAdmobInterAd) {
            Setting.adCount = Setting.adCount + 1;
            if (Setting.adCount % Constant.adShow == 0) {
                interstitialAd.setAdListener(new com.google.android.gms.ads.AdListener() {
                    @Override
                    public void onAdClosed() {
                        interAdListener.onClick(pos, note, type);
                        super.onAdClosed();
                    }
                });
                if (interstitialAd.isLoaded()) {
                    interstitialAd.show();
                } else {
                    interAdListener.onClick(pos, note, type);
                }
                loadad();
            } else {
                interAdListener.onClick(pos, note, type);
            }
        } else if (Constant.isFBInterAd) {
            Setting.adCount = Setting.adCount + 1;
        } else {
            interAdListener.onClick(pos, note, type);
        }
    }

    public void showBannerAd(LinearLayout linearLayout) {
        if (isNetworkAvailable() && linearLayout != null) {
            if (ConsentInformation.getInstance(context).getConsentStatus() == ConsentStatus.NON_PERSONALIZED) {
                showNonPersonalizedAds(linearLayout);
            } else {
                showPersonalizedAds(linearLayout);
            }
        }
    }


    private void showPersonalizedAds(LinearLayout linearLayout) {
        if (Constant.isAdmobBannerAd) {
            AdView adView = new AdView(context);
            AdRequest adRequest = new AdRequest.Builder().addTestDevice("").build();
//            adView.setAdUnitId(context.getString(R.string.banner_ad_unit_id));
            adView.setAdSize(AdSize.SMART_BANNER);
            linearLayout.addView(adView);
            adView.loadAd(adRequest);
        }
    }

    private void showNonPersonalizedAds(LinearLayout linearLayout) {
        if (Constant.isAdmobBannerAd) {
            Bundle extras = new Bundle();
            extras.putString("npa", "1");
            AdView adView = new AdView(context);
            AdRequest adRequest = new AdRequest.Builder()
                    .addNetworkExtrasBundle(AdMobAdapter.class, extras)
                    .build();
//            adView.setAdUnitId(context.getString(R.string.banner_ad_unit_id));
//            adView.setAdSize(AdSize.SMART_BANNER);
            linearLayout.addView(adView);
            adView.loadAd(adRequest);
        }
    }

}