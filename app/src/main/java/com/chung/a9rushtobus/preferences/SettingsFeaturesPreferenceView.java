package com.chung.a9rushtobus.preferences;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.chung.a9rushtobus.R;
import com.chung.a9rushtobus.UserPreferences;
import com.google.android.material.appbar.MaterialToolbar;

public class SettingsFeaturesPreferenceView extends PreferenceFragmentCompat {

    SwitchPreferenceCompat switchPref;

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
            toolbar.setTitle(getString(R.string.settings_category_featuresOption_name));
        }

        switchPref = findPreference("pref_feature_rthkNews");
        assert switchPref != null;
        switchPref.setChecked(UserPreferences.sharedPref.getBoolean("featureShowRTHKNews", false));
    }

    private void listenerInit() {

        SwitchPreferenceCompat switchPref = findPreference("pref_feature_rthkNews");
        assert switchPref != null;
        switchPref.setChecked(false);
        switchPref.setOnPreferenceChangeListener((preference, newValue) -> {
            // newValue is the new state of the switch
            boolean isSwitchOn = (Boolean) newValue;
            // Do something with the state, for example:
            UserPreferences.editor.putBoolean("featureShowRTHKNews", isSwitchOn).apply();
            Toast.makeText(getContext(), "Switch is " + (isSwitchOn ? "on" : "off"), Toast.LENGTH_SHORT).show();
            // Returning true means to update the state of the preference with the new value.
            return true;
        });

    }
}
