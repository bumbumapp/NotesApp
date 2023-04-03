package com.bumbumapps.mynotes.Methods;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.bumbumapps.mynotes.Activity.DeleteActivity.DeleteActivity;
import com.bumbumapps.mynotes.Activity.Setting.SettingActivity;

public class NavigationUtil {

    public static void SettingActivity(@NonNull Activity activity) {
        ActivityCompat.startActivity(activity, new Intent(activity, SettingActivity.class), null);
        activity.finish();
    }

    public static void DeleteActivity(@NonNull Activity activity) {
        ActivityCompat.startActivity(activity, new Intent(activity, DeleteActivity.class), null);
        activity.finish();
    }
}
