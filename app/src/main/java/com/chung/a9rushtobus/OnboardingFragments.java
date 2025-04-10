package com.chung.a9rushtobus;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.chung.a9rushtobus.preferences.LocaleHelper;

public class OnboardingFragments {
    // PERMISSION CODE DEFINE
    private static final int NOTIFICATION_PERMISSION_CODE = 100;
    private static final int LOCATION_PERMISSION_CODE = 101;

    @SuppressLint("ClickableViewAccessibility")
    protected static void setupNavigationButton(Button button, Fragment fragment) {
        button.setOnClickListener(v -> {
            if (fragment.getActivity() instanceof OnboardingActivity) {
                ((OnboardingActivity) fragment.getActivity()).goToNextPage();
            }
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    protected static void onHoverListener(Button button, Fragment fragment, int hoveredBackgroundTintColor, int hoveredTextColor){
        if (button instanceof com.google.android.material.button.MaterialButton) {
            com.google.android.material.button.MaterialButton materialButton =
                    (com.google.android.material.button.MaterialButton) button;

            ColorStateList originalBackgroundTintColor = materialButton.getBackgroundTintList();
            ColorStateList originalTextColor = materialButton.getTextColors();

            button.setOnHoverListener((v, event) -> {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_HOVER_ENTER:
                        materialButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(fragment.requireContext(), hoveredBackgroundTintColor)));
                        materialButton.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(fragment.requireContext(), hoveredTextColor)));
                        materialButton.setIconTint(ColorStateList.valueOf(ContextCompat.getColor(fragment.requireContext(), hoveredTextColor)));
                        break;
                    case MotionEvent.ACTION_HOVER_EXIT:
                        materialButton.setBackgroundTintList(originalBackgroundTintColor);
                        materialButton.setTextColor(originalTextColor);
                        materialButton.setIconTint(originalTextColor);
                        break;
                }
                return false;
            });

            button.setOnTouchListener((v, event) -> {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        materialButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(fragment.requireContext(), hoveredBackgroundTintColor)));
                        materialButton.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(fragment.requireContext(), hoveredTextColor)));
                        materialButton.setIconTint(ColorStateList.valueOf(ContextCompat.getColor(fragment.requireContext(), hoveredTextColor)));
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        materialButton.setBackgroundTintList(originalBackgroundTintColor);
                        materialButton.setTextColor(originalTextColor);
                        materialButton.setIconTint(originalTextColor);
                        break;
                }
                return false;
            });
        }
    }

    // PERMISSION REQUEST
    protected static void requestPermission(Fragment fragment, String permission, int requestCode) {
        if (ActivityCompat.checkSelfPermission(fragment.requireContext(), permission) == PackageManager.PERMISSION_GRANTED) {
            if (fragment.getActivity() instanceof OnboardingActivity) {
                ((OnboardingActivity) fragment.getActivity()).goToNextPage();
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ||
                    !permission.equals(Manifest.permission.POST_NOTIFICATIONS)) {
                fragment.requestPermissions(new String[]{permission}, requestCode);
            }
        }
    }

    // BASE FRAGMENT CLASS FOR EXTENDS
    public static abstract class BaseOnboardingFragment extends Fragment {
        @Override
        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);

            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (this.getActivity() instanceof OnboardingActivity) {
                    ((OnboardingActivity) this.getActivity()).goToNextPage();
                }
            }
        }
    }

    // FRAGMENT ACTIVITY OF EACH ONBOARDING
    public static class FragmentOnBoarding1 extends BaseOnboardingFragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_onboarding_1_5, container, false);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            Button nextButton = view.findViewById(R.id.onboarding_1_5_next_button);
            setupNavigationButton(nextButton, this);
            onHoverListener(nextButton, this, R.color.default_buttonColorBackgroundInverted, R.color.default_buttonColorTextInverted);
        }
    }

    public static class FragmentOnBoarding2 extends BaseOnboardingFragment {
        private String currentLanguage = "en"; // Default language is English
        private Button languageButton;
        private LocaleHelper localeHelper;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_onboarding_2_5, container, false);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            // Initialize UserPreferences
            if (getActivity() != null) {
                new UserPreferences(getActivity());
            }

            // Initialize LocaleHelper
            localeHelper = new LocaleHelper();

            // Get current language from preferences if available
            if (UserPreferences.sharedPref != null) {
                currentLanguage = UserPreferences.sharedPref.getString(UserPreferences.SETTINGS_APP_LANG, "en");
            }

            Button nextButton = view.findViewById(R.id.onboarding_2_5_next_button);
            languageButton = view.findViewById(R.id.language_button);
            
            // Set up navigation button
            setupNavigationButton(nextButton, this);
            
            // Set up hover effects
            onHoverListener(nextButton, this, R.color.default_buttonColorBackgroundInverted, R.color.default_buttonColorTextInverted);
            onHoverListener(languageButton, this, R.color.default_buttonColorBackgroundInverted, R.color.default_buttonColorTextInverted);
            
            // Update language button text based on current language
            updateLanguageButtonText();
            
            // Set up language button click listener
            languageButton.setOnClickListener(v -> {
                // Cycle through languages: en -> zh-rHK -> zh-rCN -> en
                switch (currentLanguage) {
                    case "en":
                        currentLanguage = "zh-rHK";
                        break;
                    case "zh-rHK":
                        currentLanguage = "zh-rCN";
                        break;
                    case "zh-rCN":
                    default:
                        currentLanguage = "en";
                        break;
                }
                
                // Apply the new language
                applyLanguage();
            });
        }
        
        /**
         * Updates the language button text based on the current language
         */
        private void updateLanguageButtonText() {
            if (languageButton == null) return;
            
            switch (currentLanguage) {
                case "zh-rHK":
                    languageButton.setText(R.string.onboarding_2_5_language_default); // This will use the zh-rHK value
                    break;
                case "zh-rCN":
                    languageButton.setText("简体中文");
                    break;
                case "en":
                default:
                    languageButton.setText("English");
                    break;
            }
        }
        
        /**
         * Applies the selected language to the app
         */
        private void applyLanguage() {
            if (getActivity() == null) return;
            
            // Save language preference
            UserPreferences.editor.putString(UserPreferences.SETTINGS_APP_LANG, currentLanguage).apply();
            UserPreferences.editor.putBoolean(UserPreferences.SETTINGS_LANG_FOLLOW_SYSTEM, false).apply();
            
            // Apply language without recreating activity
            Configuration config = localeHelper.setAppLocaleWithoutRecreate(getActivity(), currentLanguage);
            
            // Update UI elements
            localeHelper.updateAllUIElements(getActivity());
            
            // Update language button text
            updateLanguageButtonText();
        }
        
        @Override
        public void onConfigurationChanged(Configuration newConfig) {
            super.onConfigurationChanged(newConfig);
            
            // Update UI when configuration changes (like when system language changes)
            if (isAdded() && getView() != null) {
                updateLanguageButtonText();
            }
        }
    }

    public static class FragmentOnBoarding3 extends BaseOnboardingFragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_onboarding_3_5, container, false);
        }

        @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            Button nextButton = view.findViewById(R.id.onboarding_3_5_next_button);
            Button accessButton = view.findViewById(R.id.onboarding_3_5_access_button);
            setupNavigationButton(nextButton, this);
            onHoverListener(nextButton, this, R.color.default_buttonColorBackgroundInverted, R.color.default_buttonColorTextInverted);
            setupNavigationButton(accessButton, this);
            onHoverListener(accessButton, this, R.color.default_buttonColorBackgroundInverted, R.color.default_buttonColorTextInverted);
            accessButton.setOnClickListener(v -> {
                requestPermission(this, Manifest.permission.POST_NOTIFICATIONS, NOTIFICATION_PERMISSION_CODE);
            });


        }
    }

    public static class FragmentOnBoarding4 extends BaseOnboardingFragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_onboarding_4_5, container, false);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            Button nextButton = view.findViewById(R.id.onboarding_4_5_next_button);
            Button accessButton = view.findViewById(R.id.onboarding_4_5_access_button);
            setupNavigationButton(nextButton, this);
            onHoverListener(nextButton, this, R.color.default_buttonColorBackgroundInverted, R.color.default_buttonColorTextInverted);
            setupNavigationButton(accessButton, this);
            onHoverListener(accessButton, this, R.color.default_buttonColorBackgroundInverted, R.color.default_buttonColorTextInverted);
            accessButton.setOnClickListener(v -> {
                requestPermission(this, Manifest.permission.ACCESS_FINE_LOCATION, LOCATION_PERMISSION_CODE);
            });

        }
    }

    public static class FragmentOnBoarding5 extends BaseOnboardingFragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_onboarding_5_5, container, false);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            Button finishButton = view.findViewById(R.id.onboarding_5_5_next_button);
            setupNavigationButton(finishButton, this);
            onHoverListener(finishButton, this, R.color.default_buttonColorBackgroundInverted, R.color.default_buttonColorTextInverted);

            finishButton.setOnClickListener(v -> {
                SharedPreferences sharedPreferences = requireActivity().getSharedPreferences(UserPreferences.SHARED_PREFS, Context.MODE_PRIVATE);

                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean(UserPreferences.ONBOARDING_COMPLETE, false);
                editor.commit();

                Intent intent = new Intent(requireActivity(), MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(intent);
                requireActivity().overridePendingTransition(0, 0); // Disable transition animation
                requireActivity().finish();
                UserPreferences.editor.putBoolean(UserPreferences.ONBOARDING_COMPLETE, true).apply();
            });
        }
    }
}