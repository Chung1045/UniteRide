package com.chung.a9rushtobus;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceFragmentCompat;

import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.appbar.MaterialToolbar;

public class SettingsThemePreferenceView extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.preference_application_theme, rootKey);
        layoutInit();
        listenerInit();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        updateToolbarTitle("Theme");
    }

    private void updateToolbarTitle(String title) {
        MaterialToolbar toolbar = requireActivity().findViewById(R.id.settingsToolBar);
        CollapsingToolbarLayout collapsingToolbar = (CollapsingToolbarLayout) toolbar.getParent();

        toolbar.setTitle(title);
        if (collapsingToolbar != null) {
            collapsingToolbar.setTitle(title);
        }
    }

    private void listenerInit() {
        // Handle system theme follow switch
        CustomRadioButtonPreference lightThemePref = findPreference("pref_theme_light_theme");
        CustomRadioButtonPreference darkThemePref = findPreference("pref_theme_dark_theme");
        CustomMainSwitchPreference switchPref = findPreference("pref_theme_follow_system");
        assert lightThemePref != null;
        assert darkThemePref != null;
        assert switchPref != null;

        switchPref.setOnPreferenceChangeListener((preference, newValue) -> {
            boolean isChecked = (Boolean) newValue;
            if (isChecked) {
                Toast.makeText(getContext(), "Follow system theme selected", Toast.LENGTH_SHORT).show();
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                lightThemePref.setEnabled(false);
                darkThemePref.setEnabled(false);
            } else {
                Toast.makeText(getContext(), "Follow system theme deselected", Toast.LENGTH_SHORT).show();

                if (lightThemePref.isChecked()) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                }

                lightThemePref.setEnabled(true);
                darkThemePref.setEnabled(true);
            }
            return true;
        });

        lightThemePref.setOnPreferenceClickListener(preference -> {
            lightThemePref.setChecked(true);
            darkThemePref.setChecked(false);
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            return true;
        });

        darkThemePref.setOnPreferenceClickListener(preference -> {
            darkThemePref.setChecked(true);
            lightThemePref.setChecked(false);
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            return true;
        });
    }

    private void layoutInit() {
        CustomRadioButtonPreference lightThemePref = findPreference("pref_theme_light_theme");
        CustomRadioButtonPreference darkThemePref = findPreference("pref_theme_dark_theme");
        CustomMainSwitchPreference switchPref = findPreference("pref_theme_follow_system");
        assert lightThemePref != null;
        assert darkThemePref != null;
        assert switchPref != null;
        if (switchPref.isChecked()) {
            lightThemePref.setEnabled(false);
            darkThemePref.setEnabled(false);
        } else {
            lightThemePref.setEnabled(true);
            darkThemePref.setEnabled(true);
        }
    }
}