package com.chung.a9rushtobus;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

public class SettingsThemePreferenceView extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.preference_application_theme, rootKey);
        listenerInit();
    }

    private void listenerInit(){
        SwitchPreferenceCompat switchPref = findPreference("pref_theme_follow_system");
        assert switchPref != null;
        switchPref.setChecked(false);
        switchPref.setOnPreferenceChangeListener((preference, newValue) -> {
            // newValue is the new state of the switch
            boolean isSwitchOn = (Boolean) newValue;
            // Do something with the state, for example:
            Toast.makeText(getContext(), "Switch is " + (isSwitchOn ? "on" : "off"), Toast.LENGTH_SHORT).show();
            // Returning true means to update the state of the preference with the new value.
            return true;
        });

    }


}

