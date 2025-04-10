package com.chung.a9rushtobus.preferences;

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
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.appbar.MaterialToolbar;

public class SettingsThemePreferenceView extends PreferenceFragmentCompat {

    private UserPreferences userPreferences;

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.preference_application_theme, rootKey);
        userPreferences = new UserPreferences(requireActivity());
        layoutInit();
        listenerInit();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        updateToolbarTitle(getString(R.string.settings_category_themeOption_name));
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
            
            // Save current language settings before changing theme
            String currentLocale = UserPreferences.sharedPref.getString(UserPreferences.SETTINGS_APP_LANG, "en");
            boolean langFollowSystem = UserPreferences.sharedPref.getBoolean(UserPreferences.SETTINGS_LANG_FOLLOW_SYSTEM, false);
            
            if (isChecked) {
                Toast.makeText(getContext(), "Follow system theme selected", Toast.LENGTH_SHORT).show();
                UserPreferences.editor.putBoolean(UserPreferences.SETTINGS_THEME_FOLLOW_SYSTEM, true).commit();
                lightThemePref.setEnabled(false);
                darkThemePref.setEnabled(false);
                
                // Apply theme change
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            } else {
                Toast.makeText(getContext(), "Follow system theme deselected", Toast.LENGTH_SHORT).show();
                UserPreferences.editor.putBoolean(UserPreferences.SETTINGS_THEME_FOLLOW_SYSTEM, false).commit();
                lightThemePref.setEnabled(true);
                darkThemePref.setEnabled(true);

                // Apply appropriate theme based on selection
                if (lightThemePref.isChecked()) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                }
            }
            
            // Ensure language settings are preserved after theme change
            UserPreferences.editor.putString(UserPreferences.SETTINGS_APP_LANG, currentLocale).commit();
            UserPreferences.editor.putBoolean(UserPreferences.SETTINGS_LANG_FOLLOW_SYSTEM, langFollowSystem).commit();
            
            return true;
        });

        lightThemePref.setOnPreferenceClickListener(preference -> {
            // Save current language settings before changing theme
            String currentLocale = UserPreferences.sharedPref.getString(UserPreferences.SETTINGS_APP_LANG, "en");
            boolean langFollowSystem = UserPreferences.sharedPref.getBoolean(UserPreferences.SETTINGS_LANG_FOLLOW_SYSTEM, false);
            
            lightThemePref.setChecked(true);
            darkThemePref.setChecked(false);
            UserPreferences.editor.putBoolean(UserPreferences.SETTINGS_THEME_LIGHT, true).commit();
            UserPreferences.editor.putBoolean(UserPreferences.SETTINGS_THEME_DARK, false).commit();
            
            // Ensure language settings are preserved after theme change
            UserPreferences.editor.putString(UserPreferences.SETTINGS_APP_LANG, currentLocale).commit();
            UserPreferences.editor.putBoolean(UserPreferences.SETTINGS_LANG_FOLLOW_SYSTEM, langFollowSystem).commit();
            
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            return true;
        });

        darkThemePref.setOnPreferenceClickListener(preference -> {
            // Save current language settings before changing theme
            String currentLocale = UserPreferences.sharedPref.getString(UserPreferences.SETTINGS_APP_LANG, "en");
            boolean langFollowSystem = UserPreferences.sharedPref.getBoolean(UserPreferences.SETTINGS_LANG_FOLLOW_SYSTEM, false);
            
            darkThemePref.setChecked(true);
            lightThemePref.setChecked(false);
            UserPreferences.editor.putBoolean(UserPreferences.SETTINGS_THEME_DARK, true).commit();
            UserPreferences.editor.putBoolean(UserPreferences.SETTINGS_THEME_LIGHT, false).commit();
            
            // Ensure language settings are preserved after theme change
            UserPreferences.editor.putString(UserPreferences.SETTINGS_APP_LANG, currentLocale).commit();
            UserPreferences.editor.putBoolean(UserPreferences.SETTINGS_LANG_FOLLOW_SYSTEM, langFollowSystem).commit();
            
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            return true;
        });
    }

    private void layoutInit() {
        CustomRadioButtonPreference lightThemePref = findPreference("pref_theme_light_theme");
        CustomRadioButtonPreference darkThemePref = findPreference("pref_theme_dark_theme");
        CustomMainSwitchPreference followSystemPref = findPreference("pref_theme_follow_system");
        assert lightThemePref != null;
        assert darkThemePref != null;
        assert followSystemPref != null;
        lightThemePref.setChecked(UserPreferences.sharedPref.getBoolean(UserPreferences.SETTINGS_THEME_LIGHT, false));
        darkThemePref.setChecked(UserPreferences.sharedPref.getBoolean(UserPreferences.SETTINGS_THEME_DARK, false));
        followSystemPref.setChecked(UserPreferences.sharedPref.getBoolean(UserPreferences.SETTINGS_THEME_FOLLOW_SYSTEM, false));
        if (followSystemPref.isChecked()) {
            lightThemePref.setEnabled(false);
            darkThemePref.setEnabled(false);
        } else {
            lightThemePref.setEnabled(true);
            darkThemePref.setEnabled(true);
        }
    }
}