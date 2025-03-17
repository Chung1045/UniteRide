package com.chung.a9rushtobus.preferences;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.chung.a9rushtobus.OnboardingActivity;
import com.chung.a9rushtobus.R;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class SettingsMainPreferenceView extends PreferenceFragmentCompat {

    private BottomNavigationView bottomNavigationView;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preference_main, rootKey);
        listenerInit();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bottomNavigationView = requireActivity().findViewById(R.id.bottomNav_main);

        // Set initial toolbar title
        updateToolbarTitle("Settings");
        bottomNavigationView.setVisibility(View.VISIBLE);
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

        Preference applicationLanguage = findPreference("pref_main_language");
        assert applicationLanguage != null;

        applicationLanguage.setOnPreferenceClickListener(view -> {
            updateToolbarTitle("Language");
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainerView, new SettingsLanguagePreferenceView())
                    .addToBackStack(null)
                    .commit();
            bottomNavigationView.setVisibility(View.GONE);
            return false;
        });

        Preference applicationTheme = findPreference("pref_main_theme");
        assert applicationTheme != null;

        applicationTheme.setOnPreferenceClickListener(view -> {
            updateToolbarTitle("Theme");
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainerView, new SettingsThemePreferenceView())
                    .addToBackStack(null)
                    .commit();
            return false;
        });

        Preference feauturePreference = findPreference("pref_main_features");
        assert feauturePreference != null;

        feauturePreference.setOnPreferenceClickListener(view -> {
            updateToolbarTitle("Features");
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainerView, new SettingsFeaturesPreferenceView())
                    .addToBackStack(null)
                    .commit();
            return false;
        });

        Preference aboutPreference = findPreference("pref_main_about");
        assert aboutPreference != null;

        aboutPreference.setOnPreferenceClickListener(view -> {
            updateToolbarTitle("About");
            return false;
        });

        Preference accessibilityPreference = findPreference("pref_main_accessibility");
        assert accessibilityPreference != null;

        accessibilityPreference.setOnPreferenceClickListener(view -> {
            updateToolbarTitle("Accessibility");
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainerView, new SettingsAccessibilityPreferenceView())
                    .addToBackStack(null)
                    .commit();
            return false;
        });

        Preference dev_OnboardPreference = findPreference("pref_dev_onboard");
        assert dev_OnboardPreference != null;

        dev_OnboardPreference.setOnPreferenceClickListener(view -> {
            Intent newActivity = new Intent(view.getContext(), OnboardingActivity.class);
            startActivity(newActivity, null);
            return false;
        });

    }
}