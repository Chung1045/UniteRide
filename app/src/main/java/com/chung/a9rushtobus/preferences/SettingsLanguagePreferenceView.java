package com.chung.a9rushtobus.preferences;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
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
import com.chung.a9rushtobus.elements.CustomSwitchPreference;

import java.util.Locale;
import java.util.Objects;

public class SettingsLanguagePreferenceView extends PreferenceFragmentCompat {
    private static final String CURRENT_TOOLBAR_TITLE = "current_toolbar_title";
    
    // Factory method to create a new instance of this fragment
    public static SettingsLanguagePreferenceView newInstance() {
        return new SettingsLanguagePreferenceView();
    }

    private CustomMainSwitchPreference followSysPref;
    private CustomRadioButtonPreference chinTradPref, chinSimpPref, engPref;
    private LocaleHelper localeHelper;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preference_language, rootKey);
        localeHelper = new LocaleHelper();
        
        // Initialize UserPreferences only if we're attached to an activity
        if (getActivity() != null) {
            new UserPreferences(getActivity());
        }
        
        layoutInit();
        listenerInit();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Restore toolbar title
        String savedTitle = UserPreferences.sharedPref.getString(CURRENT_TOOLBAR_TITLE, null);
        if (savedTitle != null) {
            updateToolbarTitle(savedTitle);
        }
    }

    private void updateToolbarTitle(String title) {
        if (getActivity() != null) {
            androidx.appcompat.widget.Toolbar toolbar = getActivity().findViewById(R.id.settingsToolBar);
            com.google.android.material.appbar.CollapsingToolbarLayout collapsingToolbar = 
                (com.google.android.material.appbar.CollapsingToolbarLayout) toolbar.getParent();

            toolbar.setTitle(title);
            if (collapsingToolbar != null) {
                collapsingToolbar.setTitle(title);
            }
        }
    }

    private void listenerInit() {
        // Make sure we're attached to a context before proceeding
        if (getContext() == null) return;

        assert chinTradPref != null;
        assert chinSimpPref != null;
        assert engPref != null;
        assert followSysPref != null;

        followSysPref.setOnPreferenceChangeListener((preference, newValue) -> {
            boolean isChecked = (Boolean) newValue;
            if (isChecked) {
                Toast.makeText(getContext(), "Follow system lang selected", Toast.LENGTH_SHORT).show();
                localeHelper.toggleFollowSysLocale(getContext());
                chinTradPref.setEnabled(false);
                chinSimpPref.setEnabled(false);
                engPref.setEnabled(false);
            } else {
                Toast.makeText(getContext(), "Follow system lang deselected", Toast.LENGTH_SHORT).show();
                localeHelper.toggleFollowSysLocale(getContext());
                chinTradPref.setEnabled(true);
                chinSimpPref.setEnabled(true);
                engPref.setEnabled(true);
            }
            return true;
        });

        chinTradPref.setOnPreferenceChangeListener((preference, newValue) -> {
            if ((boolean) newValue) {
                chinTradPref.setChecked(true);
                chinSimpPref.setChecked(false);
                engPref.setChecked(false);
                setAppLocale(getContext(), "zh-rHK");
            }
            return true;
        });

        chinSimpPref.setOnPreferenceChangeListener((preference, newValue) -> {
            if ((boolean) newValue) {
                chinSimpPref.setChecked(true);
                chinTradPref.setChecked(false);
                engPref.setChecked(false);
                setAppLocale(getContext(), "zh-rCN");                
            }
            return true;
        });

        engPref.setOnPreferenceChangeListener((preference, newValue) -> {
            if ((boolean) newValue) {
                engPref.setChecked(true);
                chinTradPref.setChecked(false);
                chinSimpPref.setChecked(false);
                setAppLocale(getContext(), "en");
            }
            return true;
        });

    }

    private void layoutInit() {
        // Make sure we're attached to a context before proceeding
        if (getContext() == null) return;
        
        followSysPref = findPreference("pref_language_follow_system");
        chinTradPref = findPreference("pref_language_trad_chinese");
        chinSimpPref = findPreference("pref_language_simp_chinese");
        engPref = findPreference("pref_language_english");

        assert followSysPref != null;
        assert chinTradPref != null;
        assert chinSimpPref != null;
        assert engPref != null;

        followSysPref.setChecked(UserPreferences.sharedPref.getBoolean(UserPreferences.SETTINGS_LANG_FOLLOW_SYSTEM, false));
        if (followSysPref.isChecked()) {
            chinTradPref.setEnabled(false);
            chinSimpPref.setEnabled(false);
            engPref.setEnabled(false);
        } else {
            chinTradPref.setEnabled(true);
            chinSimpPref.setEnabled(true);
            engPref.setEnabled(true);
        }

        switch (UserPreferences.sharedPref.getString(UserPreferences.SETTINGS_APP_LANG, "en")) {
            case "zh-rHK":
                chinTradPref.setChecked(true);
                chinSimpPref.setChecked(false);
                engPref.setChecked(false);
                break;
            case "zh-rCN":
                chinSimpPref.setChecked(true);
                chinTradPref.setChecked(false);
                engPref.setChecked(false);
                break;
            case "en":
                engPref.setChecked(true);
                chinTradPref.setChecked(false);
                chinSimpPref.setChecked(false);
        }

    }

    public void setAppLocale(Context context, String localeCode) {
        Resources resources = context.getResources();
        Configuration config = resources.getConfiguration();
        Locale locale;
        
        // Handle different locale codes properly
        if (localeCode.equals("zh-rHK")) {
            locale = Locale.TRADITIONAL_CHINESE;
        } else if (localeCode.equals("zh-rCN")) {
            locale = Locale.SIMPLIFIED_CHINESE;
        } else {
            locale = new Locale(localeCode);
        }
        
        Locale.setDefault(locale);
        config.setLocale(locale);
        
        // Update the configuration
        context.createConfigurationContext(config);
        resources.updateConfiguration(config, resources.getDisplayMetrics());

        // Save the selected language preference
        UserPreferences.editor.putString(UserPreferences.SETTINGS_APP_LANG, localeCode).commit();

        // Save current toolbar title before recreating
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            // Save the current toolbar title
            UserPreferences.editor.putString(CURRENT_TOOLBAR_TITLE, 
                getString(R.string.settings_category_langOption_name)).apply();
            activity.recreate();
        }
    }
}