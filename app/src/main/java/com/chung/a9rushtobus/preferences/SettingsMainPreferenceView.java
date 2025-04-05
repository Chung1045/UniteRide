package com.chung.a9rushtobus.preferences;

import static org.chromium.base.ThreadUtils.runOnUiThread;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.chung.a9rushtobus.service.DataFetcher;
import com.chung.a9rushtobus.OnboardingActivity;
import com.chung.a9rushtobus.R;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class SettingsMainPreferenceView extends PreferenceFragmentCompat {

    private static final String KEY_CURRENT_TITLE = "current_title";
    private BottomNavigationView bottomNavigationView;
    private DataFetcher dataFetcher;
    private String currentTitle;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preference_main, rootKey);
        dataFetcher = new DataFetcher(requireActivity());
        listenerInit();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bottomNavigationView = requireActivity().findViewById(R.id.bottomNav_main);

        // Restore saved title or set default
        if (savedInstanceState != null) {
            currentTitle = savedInstanceState.getString(KEY_CURRENT_TITLE);
        }
        if (currentTitle == null) {
            currentTitle = getString(R.string.bottomNav_settings_tabName);
        }
        updateToolbarTitle(currentTitle);
        bottomNavigationView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_CURRENT_TITLE, currentTitle);
    }

    private void updateToolbarTitle(String title) {
        MaterialToolbar toolbar = requireActivity().findViewById(R.id.settingsToolBar);
        CollapsingToolbarLayout collapsingToolbar = (CollapsingToolbarLayout) toolbar.getParent();

        currentTitle = title;  // Save the current title
        toolbar.setTitle(title);
        if (collapsingToolbar != null) {
            collapsingToolbar.setTitle(title);
        }
    }

    private void listenerInit() {

        Preference applicationLanguage = findPreference("pref_main_language");
        assert applicationLanguage != null;

        applicationLanguage.setOnPreferenceClickListener(view -> {
            updateToolbarTitle(getString(R.string.settings_category_langOption_name));
            getParentFragmentManager().beginTransaction()
                    .setCustomAnimations(
                            R.anim.slide_in_right,
                            R.anim.slide_out_left,
                            R.anim.slide_in_left,
                            R.anim.slide_out_right
                    ).replace(R.id.fragmentContainerView, SettingsLanguagePreferenceView.newInstance())
                    .addToBackStack(null)
                    .commit();
            bottomNavigationView.setVisibility(View.GONE);
            return false;
        });

        Preference applicationTheme = findPreference("pref_main_theme");
        assert applicationTheme != null;

        applicationTheme.setOnPreferenceClickListener(view -> {
            updateToolbarTitle(getString(R.string.settings_category_themeOption_name));
            getParentFragmentManager().beginTransaction()
                    .setCustomAnimations(
                    R.anim.slide_in_right,
                    R.anim.slide_out_left,
                    R.anim.slide_in_left,
                    R.anim.slide_out_right
                    ).replace(R.id.fragmentContainerView, new SettingsThemePreferenceView())
                    .addToBackStack(null)
                    .commit();
            return false;
        });

        Preference feauturePreference = findPreference("pref_main_features");
        assert feauturePreference != null;

        feauturePreference.setOnPreferenceClickListener(view -> {
            updateToolbarTitle(getString(R.string.settings_category_featuresOption_name));
            getParentFragmentManager().beginTransaction().setCustomAnimations(
                            R.anim.slide_in_right,
                            R.anim.slide_out_left,
                            R.anim.slide_in_left,
                            R.anim.slide_out_right
                    ).replace(R.id.fragmentContainerView, new SettingsFeaturesPreferenceView())
                    .addToBackStack(null)
                    .commit();
            return false;
        });

        Preference aboutPreference = findPreference("pref_main_about");
        assert aboutPreference != null;

        aboutPreference.setOnPreferenceClickListener(view -> {
            updateToolbarTitle(getString(R.string.settings_category_aboutOption_name));
            getParentFragmentManager().beginTransaction().setCustomAnimations(
                            R.anim.slide_in_right,
                            R.anim.slide_out_left,
                            R.anim.slide_in_left,
                            R.anim.slide_out_right
                    ).replace(R.id.fragmentContainerView, new SettingsAboutView())
                    .addToBackStack(null)
                    .commit();
            return false;
        });

        Preference accessibilityPreference = findPreference("pref_main_accessibility");
        assert accessibilityPreference != null;

        accessibilityPreference.setOnPreferenceClickListener(view -> {
            updateToolbarTitle(getString(R.string.settings_category_accessibilityOption_name));
            getParentFragmentManager().beginTransaction().setCustomAnimations(
                            R.anim.slide_in_right,
                            R.anim.slide_out_left,
                            R.anim.slide_in_left,
                            R.anim.slide_out_right
                    ).replace(R.id.fragmentContainerView, new SettingsAccessibilityPreferenceView())
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

        Preference dev_FetchData = findPreference("pref_dev_fetchData");
        assert dev_FetchData!= null;

        dev_FetchData.setOnPreferenceClickListener(view -> {
            new Thread(() -> {
                try {
                    runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Fetching data...", Toast.LENGTH_SHORT).show();
                        dataFetcher.refreshAllData();
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            }).start();
            return false;
        });

    }
}