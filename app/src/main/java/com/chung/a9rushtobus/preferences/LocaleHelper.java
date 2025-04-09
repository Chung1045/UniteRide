package com.chung.a9rushtobus.preferences;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.widget.SearchView;

import com.chung.a9rushtobus.R;
import com.chung.a9rushtobus.UserPreferences;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.lang.reflect.Method;
import java.util.Locale;

public class LocaleHelper {
    /**
     * Sets the app locale and recreates the activity to apply changes
     */
    public void setAppLocale(Context context, String localeCode) {
        applyLocale(context, localeCode);

        // Recreate the activity to apply changes
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            activity.recreate();
        }
    }
    
    /**
     * Sets the app locale without recreating the activity
     * @return Configuration object with the new locale applied
     */
    public Configuration setAppLocaleWithoutRecreate(Context context, String localeCode) {
        return applyLocale(context, localeCode);
    }
    
    /**
     * Updates all UI elements in the app to reflect the new language
     * This should be called after changing the locale without recreating the activity
     */
    public void updateAllUIElements(Activity activity) {
        if (activity == null) return;
        
        // Get the current locale configuration
        Resources resources = activity.getResources();
        Configuration config = resources.getConfiguration();
        
        // Update the activity's title
        if (activity.getTitle() != null) {
            activity.setTitle(activity.getTitle());
        }
        
        // Force update bottom navigation view if it exists
        BottomNavigationView bottomNav = activity.findViewById(R.id.bottomNav_main);
        if (bottomNav != null) {
            // Update menu items
            bottomNav.getMenu().findItem(R.id.menu_item_main_saved).setTitle(resources.getString(R.string.bottomNav_saved_tabName));
            bottomNav.getMenu().findItem(R.id.menu_item_main_nearby).setTitle(resources.getString(R.string.bottomNav_nearby_tabName));
            bottomNav.getMenu().findItem(R.id.menu_item_main_search).setTitle(resources.getString(R.string.bottomNav_search_tabName));
            bottomNav.getMenu().findItem(R.id.menu_item_main_news).setTitle(resources.getString(R.string.bottomNav_news_tabName));
            bottomNav.getMenu().findItem(R.id.menu_item_main_settings).setTitle(resources.getString(R.string.bottomNav_settings_tabName));
        }
        
        // Force update toolbar if it exists
        androidx.appcompat.widget.Toolbar toolbar = activity.findViewById(R.id.settingsToolBar);
        if (toolbar != null && toolbar.getTitle() != null) {
            // Get the current title and try to find the corresponding string resource
            CharSequence currentTitle = toolbar.getTitle();
            toolbar.setTitle(currentTitle);
            
            // Also update collapsing toolbar if it exists
            com.google.android.material.appbar.CollapsingToolbarLayout collapsingToolbar = 
                (com.google.android.material.appbar.CollapsingToolbarLayout) toolbar.getParent();
            if (collapsingToolbar != null) {
                collapsingToolbar.setTitle(currentTitle);
            }
        }
        
        // Update all text views in the entire view hierarchy
        if (activity.getWindow() != null && activity.getWindow().getDecorView() != null) {
            updateTextViewsRecursively(activity.getWindow().getDecorView());
        }
        
        // Update specific UI elements that might not be caught by the recursive update
        updateSpecificUIElements(activity);
        
        // Notify all fragments about configuration change
        notifyFragmentsOnConfigurationChanged(activity, config);
        
        // Force refresh all settings fragments
        if (activity instanceof androidx.fragment.app.FragmentActivity) {
            refreshAllSettingsFragments((androidx.fragment.app.FragmentActivity) activity);
        }
    }
    
    /**
     * Updates all TextView instances in the view hierarchy recursively
     */
    private void updateTextViewsRecursively(View view) {
        if (view == null) return;
        
        // If this is a TextView, update its text if it's from a string resource
        if (view instanceof android.widget.TextView) {
            android.widget.TextView textView = (android.widget.TextView) view;
            
            // Force the TextView to refresh its text
            CharSequence text = textView.getText();
            if (text != null) {
                textView.setText(text);
            }
            
            // Also update hint text if present
            CharSequence hint = textView.getHint();
            if (hint != null) {
                textView.setHint(hint);
            }
        }
        
        // If this is a ViewGroup, recursively update all its children
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup viewGroup = (android.view.ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                updateTextViewsRecursively(viewGroup.getChildAt(i));
            }
        }
    }
    
    /**
     * Updates specific UI elements that might need special handling
     */
    private void updateSpecificUIElements(Activity activity) {
        // Update search bar in fragment_search
        View searchEditText = activity.findViewById(R.id.search_edit_text);
        if (searchEditText instanceof android.widget.EditText) {
            android.widget.EditText editText = (android.widget.EditText) searchEditText;
            editText.setHint(activity.getString(R.string.frag_search_searchBar_hint));
        }
        
        // Update bottom sheet title in fragment_nearby
        View sheetTitle = activity.findViewById(R.id.sheetTitle);
        if (sheetTitle instanceof android.widget.TextView) {
            ((android.widget.TextView) sheetTitle).setText(activity.getString(R.string.bottomNav_nearby_tabName));
        }
        
        // Update location info in fragment_nearby
        View locationInfo = activity.findViewById(R.id.locationInfo);
        if (locationInfo instanceof android.widget.TextView) {
            ((android.widget.TextView) locationInfo).setText(activity.getString(R.string.settings_location_required_req_name));
        }
        
        // Update no saved stops text in fragment_saved
        View noSavedText = activity.findViewById(R.id.text_no_saved_stops);
        if (noSavedText instanceof android.widget.TextView) {
            ((android.widget.TextView) noSavedText).setText(activity.getString(R.string.frag_saved_noSaved_title));
        }
        
        // Update saved fragment toolbar
        androidx.appcompat.widget.Toolbar savedToolbar = activity.findViewById(R.id.toolBar_saved);
        if (savedToolbar != null) {
            savedToolbar.setTitle(activity.getString(R.string.bottomNav_saved_tabName));
        }
    }
    
    /**
     * Force refreshes all settings preference fragments in the activity
     */
    private void refreshAllSettingsFragments(androidx.fragment.app.FragmentActivity activity) {
        // Find all preference fragments and refresh them
        findAndRefreshPreferenceFragments(activity.getSupportFragmentManager());
    }
    
    /**
     * Recursively finds and refreshes all preference fragments in a fragment manager
     */
    private void findAndRefreshPreferenceFragments(androidx.fragment.app.FragmentManager fragmentManager) {
        if (fragmentManager == null) return;
        
        // Check all fragments in this manager
        for (androidx.fragment.app.Fragment fragment : fragmentManager.getFragments()) {
            if (fragment == null || !fragment.isAdded()) continue;
            
            // If this is a preference fragment, refresh it
            if (fragment instanceof androidx.preference.PreferenceFragmentCompat) {
                androidx.preference.PreferenceFragmentCompat prefFragment = 
                    (androidx.preference.PreferenceFragmentCompat) fragment;
                
                // Get the XML resource ID used by this fragment
                int xmlResId = 0;
                
                // Determine which XML resource to use based on fragment type
                if (fragment instanceof SettingsMainPreferenceView) {
                    xmlResId = R.xml.preference_main;
                } else if (fragment instanceof SettingsLanguagePreferenceView) {
                    xmlResId = R.xml.preference_language;
                } else if (fragment instanceof SettingsThemePreferenceView) {
                    xmlResId = R.xml.preference_application_theme;
                } else if (fragment instanceof SettingsFeaturesPreferenceView) {
                    xmlResId = R.xml.preference_features;
                } else if (fragment instanceof SettingsAccessibilityPreferenceView) {
                    xmlResId = R.xml.preference_accessibility;
                } else if (fragment instanceof SettingsAboutView) {
                    xmlResId = R.xml.preference_about_libs;
                } else if (fragment instanceof SettingsDatabaseView) {
                    xmlResId = R.xml.preference_database;
                }
                
                // If we found a matching XML resource, reload the preferences
                if (xmlResId != 0 && prefFragment.getPreferenceScreen() != null) {
                    final int finalXmlResId = xmlResId;
                    
                    // Run on UI thread to avoid potential crashes
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        try {
                            // Remove all existing preferences
                            prefFragment.getPreferenceScreen().removeAll();
                            
                            // Reload preferences from XML
                            prefFragment.setPreferencesFromResource(finalXmlResId, null);
                            
                            // Call layoutInit and listenerInit if they exist
                            try {
                                java.lang.reflect.Method layoutInitMethod = 
                                    fragment.getClass().getDeclaredMethod("layoutInit");
                                layoutInitMethod.setAccessible(true);
                                layoutInitMethod.invoke(fragment);
                            } catch (Exception e) {
                                // Method doesn't exist or can't be called, ignore
                            }
                            
                            try {
                                java.lang.reflect.Method listenerInitMethod = 
                                    fragment.getClass().getDeclaredMethod("listenerInit");
                                listenerInitMethod.setAccessible(true);
                                listenerInitMethod.invoke(fragment);
                            } catch (Exception e) {
                                // Method doesn't exist or can't be called, ignore
                            }
                        } catch (Exception e) {
                            // Log any errors but don't crash
                            android.util.Log.e("LocaleHelper", "Error refreshing preferences", e);
                        }
                    });
                }
            }
            
            // Recursively check child fragments
            findAndRefreshPreferenceFragments(fragment.getChildFragmentManager());
        }
    }
    
    /**
     * Notifies all fragments about configuration changes
     */
    private void notifyFragmentsOnConfigurationChanged(Activity activity, Configuration config) {
        if (activity instanceof androidx.fragment.app.FragmentActivity) {
            androidx.fragment.app.FragmentActivity fragmentActivity = (androidx.fragment.app.FragmentActivity) activity;
            
            // Get all fragments from the activity's fragment manager
            for (androidx.fragment.app.Fragment fragment : fragmentActivity.getSupportFragmentManager().getFragments()) {
                if (fragment != null && fragment.isAdded()) {
                    // Manually call onConfigurationChanged on each fragment
                    fragment.onConfigurationChanged(config);
                    
                    // Special handling for preference fragments
                    if (fragment instanceof androidx.preference.PreferenceFragmentCompat) {
                        refreshPreferenceFragment((androidx.preference.PreferenceFragmentCompat) fragment);
                    }
                    
                    // Also notify child fragments
                    for (androidx.fragment.app.Fragment childFragment : fragment.getChildFragmentManager().getFragments()) {
                        if (childFragment != null && childFragment.isAdded()) {
                            childFragment.onConfigurationChanged(config);
                            
                            // Special handling for preference fragments
                            if (childFragment instanceof androidx.preference.PreferenceFragmentCompat) {
                                refreshPreferenceFragment((androidx.preference.PreferenceFragmentCompat) childFragment);
                            }
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Refreshes all preferences in a PreferenceFragmentCompat
     */
    private void refreshPreferenceFragment(androidx.preference.PreferenceFragmentCompat fragment) {
        if (fragment == null || !fragment.isAdded()) return;
        
        // Get the preference screen
        androidx.preference.PreferenceScreen preferenceScreen = fragment.getPreferenceScreen();
        if (preferenceScreen == null) return;
        
        // Force refresh all preferences
        int count = preferenceScreen.getPreferenceCount();
        for (int i = 0; i < count; i++) {
            androidx.preference.Preference preference = preferenceScreen.getPreference(i);
            if (preference != null) {
                // Force preference to refresh its view
                preference.setTitle(preference.getTitle());
                if (preference.getSummary() != null) {
                    preference.setSummary(preference.getSummary());
                }
                
                // Handle special preference types
                if (preference instanceof androidx.preference.PreferenceGroup) {
                    refreshPreferenceGroup((androidx.preference.PreferenceGroup) preference);
                }
            }
        }
    }
    
    /**
     * Recursively refreshes all preferences in a PreferenceGroup
     */
    private void refreshPreferenceGroup(androidx.preference.PreferenceGroup group) {
        if (group == null) return;
        
        int count = group.getPreferenceCount();
        for (int i = 0; i < count; i++) {
            androidx.preference.Preference preference = group.getPreference(i);
            if (preference != null) {
                // Force preference to refresh its view
                preference.setTitle(preference.getTitle());
                if (preference.getSummary() != null) {
                    preference.setSummary(preference.getSummary());
                }
                
                // Recursively handle nested preference groups
                if (preference instanceof androidx.preference.PreferenceGroup) {
                    refreshPreferenceGroup((androidx.preference.PreferenceGroup) preference);
                }
            }
        }
    }
    
    /**
     * Common method to apply locale changes
     * @return Configuration object with the new locale applied
     */
    private Configuration applyLocale(Context context, String localeCode) {
        Resources resources = context.getResources();
        Configuration config = resources.getConfiguration();
        Locale locale = null;

        // Handle different locale codes properly
        switch (localeCode) {
            case "auto":
                // Use system default locale
                locale = Resources.getSystem().getConfiguration().getLocales().get(0);
                break;
            case "zh-rHK":
                locale = Locale.TRADITIONAL_CHINESE;
                break;
            case "zh-rCN":
                locale = Locale.SIMPLIFIED_CHINESE;
                break;
            case "en":
                locale = new Locale(localeCode);
                break;
        }

        Locale.setDefault(locale);
        config.setLocale(locale);

        // Update the configuration
        context.createConfigurationContext(config);
        resources.updateConfiguration(config, resources.getDisplayMetrics());

        // Save the selected language preference
        UserPreferences.editor.putString(UserPreferences.SETTINGS_APP_LANG, localeCode).commit();
        
        return config;
    }
    
    public void toggleFollowSysLocale(Context context) {
        // Toggle the follow system locale setting
        boolean followSystem = UserPreferences.sharedPref.getBoolean(UserPreferences.SETTINGS_LANG_FOLLOW_SYSTEM, true);
        UserPreferences.editor.putBoolean(UserPreferences.SETTINGS_LANG_FOLLOW_SYSTEM, !followSystem).commit();
        
        // Get current locale setting
        String localeCode = UserPreferences.sharedPref.getString(UserPreferences.SETTINGS_APP_LANG, "en");
        
        // Toggle between system and user locale
        if (localeCode.equals("auto")) {
            localeCode = "en"; // Default to English when turning off system locale
        } else {
            localeCode = "auto"; // Use system locale
        }
        
        // Set new locale
        setAppLocale(context, localeCode);
    }
    
}
