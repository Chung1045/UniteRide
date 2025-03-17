package com.chung.a9rushtobus.preferences;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import com.chung.a9rushtobus.R;

public class SettingsLanguagePreferenceView extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preference_language, rootKey);
    }
}
