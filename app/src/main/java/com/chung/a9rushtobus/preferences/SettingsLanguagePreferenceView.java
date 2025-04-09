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
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        
        // Refresh all preferences with new language
        if (isAdded() && getPreferenceScreen() != null) {
            // Reload preferences from XML
            getPreferenceScreen().removeAll();
            setPreferencesFromResource(R.xml.preference_language, null);
            
            // Re-initialize preferences
            layoutInit();
            listenerInit();
            
            // Update UI elements
            updateUIForLanguageChange(newConfig);
        }
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
            if (toolbar != null) {
                toolbar.setTitle(title);
                
                com.google.android.material.appbar.CollapsingToolbarLayout collapsingToolbar = 
                    (com.google.android.material.appbar.CollapsingToolbarLayout) toolbar.getParent();
                    
                if (collapsingToolbar != null) {
                    collapsingToolbar.setTitle(title);
                }
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
            
            // Toggle follow system setting in preferences
            UserPreferences.editor.putBoolean(UserPreferences.SETTINGS_LANG_FOLLOW_SYSTEM, isChecked).commit();
            
            if (isChecked) {
                // Get system locale
                Locale sysLocale = Resources.getSystem().getConfiguration().getLocales().get(0);
                String localeCode = "auto";
                
                // Apply locale without recreating activity
                Configuration config = localeHelper.setAppLocaleWithoutRecreate(getContext(), localeCode);
                
                // Update all UI elements in the app
                if (getActivity() != null) {
                    localeHelper.updateAllUIElements(getActivity());
                }
                
                // Disable language selection options
                chinTradPref.setEnabled(false);
                chinSimpPref.setEnabled(false);
                engPref.setEnabled(false);
                
                // Show toast with updated resources
                Toast.makeText(getContext(), 
                    getResources().getString(R.string.settings_language_changed), 
                    Toast.LENGTH_SHORT).show();
            } else {
                // Default to English when turning off system locale
                String localeCode = "en";
                UserPreferences.editor.putString(UserPreferences.SETTINGS_APP_LANG, localeCode).commit();
                
                // Apply locale without recreating activity
                Configuration config = localeHelper.setAppLocaleWithoutRecreate(getContext(), localeCode);
                
                // Update all UI elements in the app
                if (getActivity() != null) {
                    localeHelper.updateAllUIElements(getActivity());
                }
                
                // Enable language selection options
                chinTradPref.setEnabled(true);
                chinSimpPref.setEnabled(true);
                engPref.setEnabled(true);
                
                // Update radio button selection
                engPref.setChecked(true);
                chinTradPref.setChecked(false);
                chinSimpPref.setChecked(false);
                
                // Show toast with updated resources
                Toast.makeText(getContext(), 
                    getResources().getString(R.string.settings_language_changed), 
                    Toast.LENGTH_SHORT).show();
            }
            return true;
        });

        chinTradPref.setOnPreferenceChangeListener((preference, newValue) -> {
            if ((boolean) newValue) {
                // Update radio button selection
                chinTradPref.setChecked(true);
                chinSimpPref.setChecked(false);
                engPref.setChecked(false);
                
                // Apply locale without recreating activity
                setAppLocale(getContext(), "zh-rHK");
            }
            return true;
        });

        chinSimpPref.setOnPreferenceChangeListener((preference, newValue) -> {
            if ((boolean) newValue) {
                // Update radio button selection
                chinSimpPref.setChecked(true);
                chinTradPref.setChecked(false);
                engPref.setChecked(false);
                
                // Apply locale without recreating activity
                setAppLocale(getContext(), "zh-rCN");                
            }
            return true;
        });

        engPref.setOnPreferenceChangeListener((preference, newValue) -> {
            if ((boolean) newValue) {
                // Update radio button selection
                engPref.setChecked(true);
                chinTradPref.setChecked(false);
                chinSimpPref.setChecked(false);
                
                // Apply locale without recreating activity
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
        // Save the current toolbar title
        UserPreferences.editor.putString(CURRENT_TOOLBAR_TITLE, 
            getString(R.string.settings_category_langOption_name)).apply();
            
        // Apply locale changes without recreating the activity
        Configuration config = localeHelper.setAppLocaleWithoutRecreate(context, localeCode);
        
        // Update UI to reflect language changes
        updateUIForLanguageChange(config);
        
        // Update all UI elements in the app
        if (getActivity() != null) {
            localeHelper.updateAllUIElements(getActivity());
            
            // Force refresh this fragment
            if (isAdded() && getPreferenceScreen() != null) {
                // Reload preferences from XML
                getPreferenceScreen().removeAll();
                setPreferencesFromResource(R.xml.preference_language, null);
                
                // Re-initialize preferences
                layoutInit();
                listenerInit();
            }
        }
    }
    
    /**
     * Updates UI elements to reflect language changes without activity recreation
     */
    private void updateUIForLanguageChange(Configuration config) {
        if (getActivity() == null) return;
        
        // Get updated resources with new locale
        Resources resources = getActivity().getResources();
        
        // Update preference titles and summaries
        if (followSysPref != null) {
            followSysPref.setTitle(resources.getString(R.string.settings_mainSwitchPref_followSys_name));
        }
        
        if (chinTradPref != null) {
            chinTradPref.setTitle("中文（繁體）");
            chinTradPref.setSummary(resources.getString(R.string.settings_lang_chineseTradOption_name));
        }
        
        if (chinSimpPref != null) {
            chinSimpPref.setTitle("中文（簡体）");
            chinSimpPref.setSummary(resources.getString(R.string.settings_lang_chineseSimpOption_name));
        }
        
        if (engPref != null) {
            engPref.setTitle(resources.getString(R.string.settings_lang_englishOption_name));
        }
        
        // Update toolbar title
        updateToolbarTitle(resources.getString(R.string.settings_category_langOption_name));
        
        // Show a toast message to confirm language change
        Toast.makeText(getContext(), 
            resources.getString(R.string.settings_language_changed), 
            Toast.LENGTH_SHORT).show();
    }
}