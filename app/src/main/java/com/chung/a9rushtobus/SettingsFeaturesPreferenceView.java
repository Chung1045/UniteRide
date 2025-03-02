package com.chung.a9rushtobus;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.google.android.material.appbar.MaterialToolbar;

public class SettingsFeaturesPreferenceView extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preference_features, rootKey);
        listenerInit();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Get the toolbar from the hosting activity
        MaterialToolbar toolbar = requireActivity().findViewById(R.id.settingsToolBar);
        if (toolbar != null) {
            toolbar.setTitle("Features");
        }
    }

    private void listenerInit(){

        SwitchPreferenceCompat switchPref = findPreference("pref_feature_1");
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
