package com.chung.a9rushtobus.preferences;



import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceFragmentCompat;

import com.chung.a9rushtobus.R;
import com.chung.a9rushtobus.Utils;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.appbar.MaterialToolbar;

public class SettingsAboutView extends Fragment {
    private MaterialToolbar toolbar;
    private CollapsingToolbarLayout collapsingToolbar;
    private TextView prefAboutPermission, prefAboutLibraries, prefAboutGitHub;
    private Utils utils;
    private String previousTitle; // Store the previous title for back navigation

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Restore previous title from saved instance state if available
        if (savedInstanceState != null) {
            previousTitle = savedInstanceState.getString("previous_title");
            Log.d("SettingsAboutView", "onCreate: Restored previous title from savedInstanceState: " + previousTitle);
        } else {
            // Default title for this fragment if not restored
            previousTitle = getString(R.string.settings_category_aboutOption_name); // "About"
            Log.d("SettingsAboutView", "onCreate: Set default title: " + previousTitle);
        }
    }
    
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save the previous title for configuration changes
        if (previousTitle != null) {
            outState.putString("previous_title", previousTitle);
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        
        // Check if we need to restore the title
        if (toolbar != null) {
            // If we're returning to this fragment and have a previous title, restore it
            if (previousTitle != null && !previousTitle.isEmpty()) {
                Log.d("SettingsAboutView", "onResume: Restoring title to: " + previousTitle);
                // Use direct update to avoid overwriting the previousTitle value
                updateToolbarTitleDirectly(previousTitle);
            } else if (toolbar.getTitle() != null) {
                // Otherwise just use the current title
                String currentTitle = toolbar.getTitle().toString();
                Log.d("SettingsAboutView", "onResume: Using current title: " + currentTitle);
                updateToolbarTitle(currentTitle);
            }
        }
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        
        // Remove the back stack listener to prevent memory leaks and crashes
        if (backStackChangedListener != null && isAdded()) {
            try {
                getParentFragmentManager().removeOnBackStackChangedListener(backStackChangedListener);
                Log.d("SettingsAboutView", "onDestroyView: Removed back stack listener");
            } catch (IllegalStateException e) {
                Log.e("SettingsAboutView", "onDestroyView: Error removing listener", e);
            }
        }
    }
    
    /**
     * Restores the previous toolbar title when navigating back
     */
    public void restorePreviousTitle() {
        if (!isAdded()) {
            Log.d("SettingsAboutView", "restorePreviousTitle: Fragment not attached, skipping");
            return;
        }
        
        if (previousTitle != null && !previousTitle.isEmpty()) {
            Log.d("SettingsAboutView", "Restoring previous title: " + previousTitle);
            
            // Use a direct update without saving the current title as previous
            updateToolbarTitleDirectly(previousTitle);
        } else {
            // If no previous title is available, use the default "About" title
            String defaultTitle = getString(R.string.settings_category_aboutOption_name);
            Log.d("SettingsAboutView", "No previous title to restore, using default: " + defaultTitle);
            updateToolbarTitleDirectly(defaultTitle);
        }
    }
    
    /**
     * Updates the toolbar title directly without saving the current title as previous
     * @param title The title to set
     */
    private void updateToolbarTitleDirectly(String title) {
        if (!isAdded() || getActivity() == null) {
            return; // Don't proceed if fragment is not attached
        }
        
        requireActivity().runOnUiThread(() -> {
            try {
                if (toolbar != null) {
                    toolbar.setTitle(title);
                    
                    if (collapsingToolbar != null) {
                        collapsingToolbar.setTitle(title);
                    }
                    
                    // Log successful title update
                    Log.d("SettingsAboutView", "Title directly updated to: " + title);
                } else {
                    Log.e("SettingsAboutView", "Toolbar not found");
                }
            } catch (Exception e) {
                Log.e("SettingsAboutView", "Error updating toolbar title: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * Sets up back navigation handling to restore the previous title
     */

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings_about, container, false);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        try {
            // Find the toolbar in the parent fragment's view
            if (getParentFragment() != null && getParentFragment().getView() != null) {
                toolbar = getParentFragment().getView().findViewById(R.id.settingsToolBar);
                
                // Get the CollapsingToolbarLayout (which is the parent of the toolbar)
                if (toolbar != null) {
                    collapsingToolbar = (CollapsingToolbarLayout) toolbar.getParent();
                    
                    Log.d("SettingsAboutView", "onViewCreated: toolbar = " + toolbar);
                    Log.d("SettingsAboutView", "onViewCreated: collapsingToolbar = " + collapsingToolbar);
                    
                    if (toolbar.getTitle() != null) {
                        // Save the initial title as the previous title if not already set
                        if (previousTitle == null) {
                            previousTitle = toolbar.getTitle().toString();
                        }
                        Log.d("SettingsAboutView", "Toolbar title: " + toolbar.getTitle());
                    }
                    
                    if (collapsingToolbar != null && collapsingToolbar.getTitle() != null) {
                        Log.d("SettingsAboutView", "CollapsingToolbar title: " + collapsingToolbar.getTitle());
                    }
                } else {
                    Log.e("SettingsAboutView", "Toolbar not found in parent fragment");
                }
            } else {
                Log.e("SettingsAboutView", "Parent fragment or its view is null");
            }
        } catch (Exception e) {
            Log.e("SettingsAboutView", "Error initializing toolbar: " + e.getMessage(), e);
        }
        
        utils = new Utils(requireActivity(), view, getContext());
        prefAboutPermission = view.findViewById(R.id.pref_about_permission);
        prefAboutLibraries = view.findViewById(R.id.pref_about_libraries);
        prefAboutGitHub = view.findViewById(R.id.pref_about_github);
        
        // Set up back navigation listener
        setupBackNavigation();
        
        listenerInit();
    }

    private void listenerInit() {

        prefAboutPermission.setOnClickListener(view -> {
            // Save the current title before changing it
            if (toolbar != null && toolbar.getTitle() != null) {
                previousTitle = toolbar.getTitle().toString();
                Log.d("SettingsAboutView", "Saved previous title before navigation: " + previousTitle);
            }
            
            String newTitle = getString(R.string.settings_about_permissionOption_name);
            
            // Update title with delay and then perform fragment transaction
            updateToolbarTitleWithDelay(newTitle, 50, () -> {
                if (isAdded()) {
                    getParentFragmentManager()
                            .beginTransaction()
                            .setCustomAnimations(
                                    R.anim.slide_in_right,
                                    R.anim.slide_out_left,
                                    R.anim.slide_in_left,
                                    R.anim.slide_out_right
                            ).replace(R.id.fragmentContainerView, new PrefPermissionView())
                            .addToBackStack(null)
                            .commit();
                }
            });
        });

        prefAboutLibraries.setOnClickListener(view -> {
            // Save the current title before changing it
            if (toolbar != null && toolbar.getTitle() != null) {
                previousTitle = toolbar.getTitle().toString();
                Log.d("SettingsAboutView", "Saved previous title before navigation: " + previousTitle);
            }
            
            String newTitle = getString(R.string.settings_about_libOption_name);
            
            // Update title with delay and then perform fragment transaction
            updateToolbarTitleWithDelay(newTitle, 50, () -> {
                if (isAdded()) {
                    getParentFragmentManager()
                            .beginTransaction()
                            .setCustomAnimations(
                                    R.anim.slide_in_right,
                                    R.anim.slide_out_left,
                                    R.anim.slide_in_left,
                                    R.anim.slide_out_right
                            ).replace(R.id.fragmentContainerView, new PrefLibrariesView())
                            .addToBackStack(null)
                            .commit();
                }
            });
        });

        prefAboutGitHub.setOnClickListener(view -> utils.startUrlIntent("https://github.com/Chung1045/UniteRide"));
    }

    private FragmentManager.OnBackStackChangedListener backStackChangedListener;
    
    private void setupBackNavigation() {
        // Create a listener that we can later remove to prevent memory leaks
        backStackChangedListener = () -> {
            // Check if fragment is still attached before accessing FragmentManager
            if (!isAdded()) {
                Log.d("SettingsAboutView", "BackStackChanged: Fragment not attached, skipping");
                return;
            }
            
            try {
                Fragment currentFragment = getParentFragmentManager().findFragmentById(R.id.fragmentContainerView);
                if (currentFragment instanceof SettingsAboutView) {
                    // We're back to the SettingsAboutView, restore the title
                    if (previousTitle != null && !previousTitle.isEmpty()) {
                        Log.d("SettingsAboutView", "BackStackChanged: Restoring title to: " + previousTitle);
                        restorePreviousTitle();
                    }
                }
            } catch (IllegalStateException e) {
                // Fragment not associated with a fragment manager
                Log.e("SettingsAboutView", "BackStackChanged: Error accessing fragment manager", e);
            }
        };
        
        // Add the listener
        getParentFragmentManager().addOnBackStackChangedListener(backStackChangedListener);

        // Handle back button presses
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        if (getChildFragmentManager().getBackStackEntryCount() > 0) {
                            getChildFragmentManager().popBackStack();
                            restorePreviousTitle();
                        } else {
                            setEnabled(false);
                            requireActivity().onBackPressed();
                        }
                    }
                });
    }


    private void updateToolbarTitle(String title) {
        if (!isAdded() || getActivity() == null) {
            return; // Don't proceed if fragment is not attached
        }
        
        requireActivity().runOnUiThread(() -> {
            try {
                if (toolbar != null) {
                    // Save the current title as the previous title before changing
                    if (toolbar.getTitle() != null) {
                        previousTitle = toolbar.getTitle().toString();
                    }
                    
                    toolbar.setTitle(title);
                    
                    if (collapsingToolbar != null) {
                        collapsingToolbar.setTitle(title);
                    }
                    
                    // Log successful title update
                    Log.d("SettingsAboutView", "Title updated to: " + title + " (Previous: " + previousTitle + ")");
                } else {
                    Log.e("SettingsAboutView", "Toolbar not found");
                }
            } catch (Exception e) {
                Log.e("SettingsAboutView", "Error updating toolbar title: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * Updates the toolbar title with a delay to ensure UI updates before fragment transactions
     * @param title The new title to set
     * @param delayMillis Delay in milliseconds
     * @param callback Optional callback to run after the delay
     */
    private void updateToolbarTitleWithDelay(String title, long delayMillis, Runnable callback) {
        updateToolbarTitle(title);
        
        // Add a delay to ensure UI updates before executing the callback
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (isAdded() && callback != null) {
                callback.run();
            }
        }, delayMillis);
    }

    public static class PrefPermissionView extends Fragment {
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_settings_permission_description, container, false);
        }

        @Override
        public void onDestroyView() {
            super.onDestroyView();
            
            try {
                // Restore the previous title when this fragment's view is destroyed
                if (isAdded()) {
                    Fragment parentFragment = getParentFragmentManager().findFragmentById(R.id.fragmentContainerView);
                    if (parentFragment instanceof SettingsAboutView) {
                        SettingsAboutView settingsAboutView = (SettingsAboutView) parentFragment;
                        settingsAboutView.restorePreviousTitle();
                        Log.d("PrefPermissionView", "onDestroyView: Restored title");
                    }
                }
            } catch (Exception e) {
                Log.e("PrefPermissionView", "Error restoring title", e);
            }
        }
    }

    public static class PrefLibrariesView extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
            setPreferencesFromResource(R.xml.preference_about_libs, rootKey);
        }
        
        @Override
        public void onDestroyView() {
            super.onDestroyView();
            
            try {
                // Restore the previous title when this fragment's view is destroyed
                if (isAdded()) {
                    Fragment parentFragment = getParentFragmentManager().findFragmentById(R.id.fragmentContainerView);
                    if (parentFragment instanceof SettingsAboutView) {
                        SettingsAboutView settingsAboutView = (SettingsAboutView) parentFragment;
                        settingsAboutView.restorePreviousTitle();
                        Log.d("PrefLibrariesView", "onDestroyView: Restored title");
                    }
                }
            } catch (Exception e) {
                Log.e("PrefLibrariesView", "Error restoring title", e);
            }
        }
    }
}
