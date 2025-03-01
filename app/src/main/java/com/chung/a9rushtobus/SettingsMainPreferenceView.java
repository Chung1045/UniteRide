package com.chung.a9rushtobus;

import android.os.Bundle;
import android.widget.Toast;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

public class SettingsMainPreferenceView extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preference_main, rootKey);
        listenerInit();
    }

    private void listenerInit(){

        Preference applicationLanguage = findPreference("pref_main_language");
        assert applicationLanguage != null;

        applicationLanguage.setOnPreferenceClickListener(view -> {
                    Toast.makeText(getContext(), "clicked", Toast.LENGTH_SHORT).show();
                    return false;
                }
        );

        Preference applicationTheme = findPreference("pref_main_theme");
        assert applicationTheme != null;

        applicationTheme.setOnPreferenceClickListener(view -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainerView, new SettingsThemePreferenceView())
                    .addToBackStack(null)
                    .commit();
            return false;
        });

    }

}