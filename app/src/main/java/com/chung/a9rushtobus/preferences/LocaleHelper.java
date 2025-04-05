package com.chung.a9rushtobus.preferences;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;

import com.chung.a9rushtobus.UserPreferences;

import java.util.Locale;

public class LocaleHelper {
    public void setAppLocale(Context context, String localeCode) {
        Resources resources = context.getResources();
        Configuration config = resources.getConfiguration();
        Locale locale = null;

        // Handle different locale codes properly
        switch (localeCode) {
            case "auto":
                // Use system default locale
                locale = Resources.getSystem().getConfiguration().getLocales().get(0);
                break;
            case "zh-rHK":
                locale = Locale.TRADITIONAL_CHINESE;
                break;
            case "zh-rCN":
                locale = Locale.SIMPLIFIED_CHINESE;
                break;
            case "en":
                locale = new Locale(localeCode);
                break;
        }

        Locale.setDefault(locale);
        config.setLocale(locale);

        // Update the configuration
        context.createConfigurationContext(config);
        resources.updateConfiguration(config, resources.getDisplayMetrics());

        // Save the selected language preference
        UserPreferences.editor.putString(UserPreferences.SETTINGS_APP_LANG, localeCode).commit();

        // Recreate the activity to apply changes
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            activity.recreate();
        }
    }
    
    public void toggleFollowSysLocale(Context context) {
        // Toggle the follow system locale setting
        boolean followSystem = UserPreferences.sharedPref.getBoolean(UserPreferences.SETTINGS_LANG_FOLLOW_SYSTEM, true);
        UserPreferences.editor.putBoolean(UserPreferences.SETTINGS_LANG_FOLLOW_SYSTEM, !followSystem).commit();
        
        // Get current locale setting
        String localeCode = UserPreferences.sharedPref.getString(UserPreferences.SETTINGS_APP_LANG, "en");
        
        // Toggle between system and user locale
        if (localeCode.equals("auto")) {
            localeCode = "en"; // Default to English when turning off system locale
        } else {
            localeCode = "auto"; // Use system locale
        }
        
        // Set new locale
        setAppLocale(context, localeCode);
    }
    
}
