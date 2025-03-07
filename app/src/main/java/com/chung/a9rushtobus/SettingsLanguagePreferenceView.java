package com.chung.a9rushtobus;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

public class SettingsLanguagePreferenceView extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preference_language, rootKey);
    }
}
