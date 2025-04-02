package com.chung.a9rushtobus;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

public class UserPreferences {
    public static final String SHARED_PREFS = "sharedPrefs";
    public static final String SETTINGS_FEATURE_SHOW_RTHK_NEWS = "featureShowRTHKNews";
    public static final String SETTINGS_THEME_FOLLOW_SYSTEM = "themeFollowSystem";
    public static final String SETTINGS_LANG_FOLLOW_SYSTEM = "langFollowSystem";
    public static final String SETTINGS_APP_LANG = "appLang"; //
    public static final String SETTINGS_THEME_DARK = "themeDark";
    public static final String SETTINGS_THEME_LIGHT = "themeLight";
    public static final String ONBOARDING_COMPLETE = "onboardingComplete";
    public static SharedPreferences sharedPref;
    public static SharedPreferences.Editor editor;

    public UserPreferences(Activity a){
        sharedPref = a.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE);
        editor = sharedPref.edit();
    }

}
