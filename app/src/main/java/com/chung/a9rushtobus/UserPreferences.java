package com.chung.a9rushtobus;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

public class UserPreferences {
    public static final String SHARED_PREFS = "sharedPrefs";
    public static final String SETTINGS_SHOW_RTHK_NEWS = "showRTHKNews";
    public static SharedPreferences sharedPref;
    public static SharedPreferences.Editor editor;

    public UserPreferences(Activity a){
        sharedPref = a.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE);
        editor = sharedPref.edit();
    }

}
