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

public class SettingsAccessibilityPreferenceView extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.preference_accessibility, rootKey);
        listenerInit();
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Get the toolbar from the hosting activity
        MaterialToolbar toolbar = requireActivity().findViewById(R.id.settingsToolBar);
        if (toolbar != null) {
            toolbar.setTitle(getString(R.string.settings_category_accessibilityOption_name));
        }
    }

    private void listenerInit(){

        SwitchPreferenceCompat boldTextSwitchPref = findPreference("pref_accessibility_bold_text");
        assert boldTextSwitchPref != null;
        boldTextSwitchPref.setChecked(false);
        boldTextSwitchPref.setOnPreferenceChangeListener((preference, newValue) -> {

            boolean isSwitchOn = (Boolean) newValue;
            // newValue is the new state of the switch
            UserPreferences.editor.putBoolean(UserPreferences.SETTINGS_ACCESS_BOLD_TEXT, isSwitchOn).apply();

            // Notify the user that bold text has been toggled
            if (getActivity() != null) {
                String message = isSwitchOn ?
                        getString(R.string.app_name) + " will now use bold text" :
                        getString(R.string.app_name) + " will now use normal text";
                Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();

                // Recreate the activity to apply the new theme
                getActivity().recreate();
            }
            return true;
        });

    }
}
