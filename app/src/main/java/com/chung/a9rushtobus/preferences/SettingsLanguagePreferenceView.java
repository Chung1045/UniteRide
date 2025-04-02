package com.chung.a9rushtobus.preferences;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceFragmentCompat;

import com.chung.a9rushtobus.R;
import com.chung.a9rushtobus.UserPreferences;
import com.chung.a9rushtobus.elements.CustomMainSwitchPreference;
import com.chung.a9rushtobus.elements.CustomRadioButtonPreference;
import com.chung.a9rushtobus.elements.CustomSwitchPreference;

import java.util.Locale;

public class SettingsLanguagePreferenceView extends PreferenceFragmentCompat {

    private CustomMainSwitchPreference followSysPref;

    private CustomRadioButtonPreference chinTradPref, chinSimpPref, engPref;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preference_language, rootKey);
        layoutInit();
        listenerInit();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    private void listenerInit() {

        assert chinTradPref != null;
        assert chinSimpPref != null;
        assert engPref != null;
        assert followSysPref != null;

        followSysPref.setOnPreferenceChangeListener((preference, newValue) -> {
            boolean isChecked = (Boolean) newValue;
            if (isChecked) {
                Toast.makeText(getContext(), "Follow system lang selected", Toast.LENGTH_SHORT).show();
                UserPreferences.editor.putBoolean(UserPreferences.SETTINGS_LANG_FOLLOW_SYSTEM, true).commit();
                chinTradPref.setEnabled(false);
                chinSimpPref.setEnabled(false);
                engPref.setEnabled(false);
            } else {
                Toast.makeText(getContext(), "Follow system lang deselected", Toast.LENGTH_SHORT).show();
                UserPreferences.editor.putBoolean(UserPreferences.SETTINGS_LANG_FOLLOW_SYSTEM, false).commit();
                chinTradPref.setEnabled(true);
                chinSimpPref.setEnabled(true);
                engPref.setEnabled(true);
            }
            return true;
        });

        chinTradPref.setOnPreferenceChangeListener((preference, newValue) -> {
            if ((boolean) newValue) {
                chinSimpPref.setChecked(false);
                engPref.setChecked(false);
                setAppLocale(getContext(), "zh-rHK");
            }
            return true;
        });

        chinSimpPref.setOnPreferenceChangeListener((preference, newValue) -> {
            if ((boolean) newValue) {
                chinTradPref.setChecked(false);
                engPref.setChecked(false);
                setAppLocale(getContext(), "zh-rCN");                
            }
            return true;
        });

        engPref.setOnPreferenceChangeListener((preference, newValue) -> {
            if ((boolean) newValue) {
                chinTradPref.setChecked(false);
                chinSimpPref.setChecked(false);
                setAppLocale(getContext(), "en");
            }
            return true;
        });

    }

    private void layoutInit() {
        followSysPref = findPreference("pref_language_follow_system");
        chinTradPref = findPreference("pref_language_trad_chinese");
        chinSimpPref = findPreference("pref_language_simp_chinese");
        engPref = findPreference("pref_language_english");

        assert followSysPref != null;
        assert chinTradPref != null;
        assert chinSimpPref != null;
        assert engPref != null;

        followSysPref.setChecked(UserPreferences.sharedPref.getBoolean(UserPreferences.SETTINGS_LANG_FOLLOW_SYSTEM, false));
        if (followSysPref.isChecked()) {
            chinTradPref.setEnabled(false);
            chinSimpPref.setEnabled(false);
            engPref.setEnabled(false);
        } else {
            chinTradPref.setEnabled(true);
            chinSimpPref.setEnabled(true);
            engPref.setEnabled(true);
        }
    }

    public void setAppLocale(Context context, String localeCode) {
        Resources resources = context.getResources();
        Configuration config = resources.getConfiguration();
        Locale locale;
        
        // Handle different locale codes properly
        if (localeCode.equals("zh-rHK")) {
            locale = Locale.TRADITIONAL_CHINESE;
        } else if (localeCode.equals("zh-rCN")) {
            locale = Locale.SIMPLIFIED_CHINESE;
        } else {
            locale = new Locale(localeCode);
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
}