package com.chung.a9rushtobus;

import android.content.res.ColorStateList;
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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

public class OnboardingFragments {
    // PERMISSION CODE DEFINE
    private static final int NOTIFICATION_PERMISSION_CODE = 100;
    private static final int LOCATION_PERMISSION_CODE = 101;

    @SuppressLint("ClickableViewAccessibility")
    protected static void setupNavigationButton(Button button, Fragment fragment, int hoveredBackgroundTintColor, int hoveredExitBackgroundTintColor, int hoveredTextColor, int hoveredExitTextColor) {
        button.setOnClickListener(v -> {
            if (fragment.getActivity() instanceof OnboardingActivity) {
                ((OnboardingActivity) fragment.getActivity()).goToNextPage();
            }
        });

        if (button instanceof com.google.android.material.button.MaterialButton) {
            com.google.android.material.button.MaterialButton materialButton =
                    (com.google.android.material.button.MaterialButton) button;

            button.setOnHoverListener((v, event) -> {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_HOVER_ENTER:
                        materialButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(fragment.requireContext(), hoveredBackgroundTintColor)));
                        materialButton.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(fragment.requireContext(), hoveredTextColor)));
//                        materialButton.setBackgroundTintList(ColorStateList.valueOf(
//                                ContextCompat.getColor(fragment.requireContext(), R.color.brand_colorOnSurfaceSecondary)));
//                        materialButton.setTextColor(ColorStateList.valueOf(
//                                ContextCompat.getColor(fragment.requireContext(), R.color.primary_light)));
                        break;
                    case MotionEvent.ACTION_HOVER_EXIT:
                        materialButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(fragment.requireContext(), hoveredExitBackgroundTintColor)));
                        materialButton.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(fragment.requireContext(), hoveredExitTextColor)));
//                        materialButton.setBackgroundTintList(ColorStateList.valueOf(
//                                ContextCompat.getColor(fragment.requireContext(), R.color.primary_light)));
//                        materialButton.setTextColor(ColorStateList.valueOf(
//                                ContextCompat.getColor(fragment.requireContext(), R.color.primary_light_font)));
                        break;
                }
                return false;
            });

            button.setOnTouchListener((v, event) -> {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        materialButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(fragment.requireContext(), hoveredBackgroundTintColor)));
                        materialButton.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(fragment.requireContext(), hoveredTextColor)));
//                        materialButton.setBackgroundTintList(ColorStateList.valueOf(
//                                ContextCompat.getColor(fragment.requireContext(), R.color.brand_colorOnSurfaceSecondary)));
//                        materialButton.setTextColor(ColorStateList.valueOf(
//                                ContextCompat.getColor(fragment.requireContext(), R.color.primary_light)));
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        materialButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(fragment.requireContext(), hoveredExitBackgroundTintColor)));
                        materialButton.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(fragment.requireContext(), hoveredExitTextColor)));
//                        materialButton.setBackgroundTintList(ColorStateList.valueOf(
//                                ContextCompat.getColor(fragment.requireContext(), R.color.primary_light)));
//                        materialButton.setTextColor(ColorStateList.valueOf(
//                                ContextCompat.getColor(fragment.requireContext(), R.color.primary_light_font)));
                        break;
                }
                return false;
            });
        }
    }

    // PERMISSION REQUEST
    protected static void requestPermission(Fragment fragment, String permission, int requestCode) {
        if (ActivityCompat.checkSelfPermission(fragment.requireContext(), permission) == PackageManager.PERMISSION_GRANTED) {
//            Toast.makeText(fragment.requireContext(), "Permission Granted. You can use the API which requires the permission.", Toast.LENGTH_LONG).show();
            if (fragment.getActivity() instanceof OnboardingActivity) {
                ((OnboardingActivity) fragment.getActivity()).goToNextPage();
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ||
                    !permission.equals(Manifest.permission.POST_NOTIFICATIONS)) {
                fragment.requestPermissions(new String[]{permission}, requestCode);
            }
//            else
//            {
//                Toast.makeText(fragment.requireContext(), "Permission not required for this Android version", Toast.LENGTH_LONG).show();
//            }
            if (fragment.getActivity() instanceof OnboardingActivity) {
                ((OnboardingActivity) fragment.getActivity()).goToNextPage();
            }
        }
    }

    // BASE FRAGMENT CLASS FOR EXTENDS
    public static abstract class BaseOnboardingFragment extends Fragment {
        @Override
        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);

//            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                Toast.makeText(requireContext(), "Permission Granted. You can use the API which requires the permission.", Toast.LENGTH_LONG).show();
//            } else {
//                Toast.makeText(requireContext(), "Permission Denied. You cannot use the API which requires the permission.", Toast.LENGTH_LONG).show();
//            }
        }
    }

    public static class FragmentOnBoarding1 extends BaseOnboardingFragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_onboarding_1_5, container, false);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            Button nextButton = view.findViewById(R.id.onboarding_1_5_next_button);
            setupNavigationButton(nextButton, this, R.color.brand_colorOnSurfaceSecondary, R.color.primary_light, R.color.primary_light, R.color.primary_light_font);
        }
    }

    public static class FragmentOnBoarding2 extends BaseOnboardingFragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_onboarding_2_5, container, false);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            Button nextButton = view.findViewById(R.id.onboarding_2_5_next_button);
            setupNavigationButton(nextButton, this, R.color.brand_colorOnSurfaceSecondary, R.color.primary_light, R.color.primary_light, R.color.primary_light_font);
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
            setupNavigationButton(nextButton, this, R.color.brand_colorOnSurfaceSecondary, R.color.primary_light, R.color.primary_light, R.color.primary_light_font);
            setupNavigationButton(accessButton, this, R.color.brand_colorOnSurfaceSecondary, R.color.brand_colorTertiary, R.color.primarySecond_light, R.color.primary_light);
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
            setupNavigationButton(nextButton, this, R.color.brand_colorOnSurfaceSecondary, R.color.primary_light, R.color.primary_light, R.color.primary_light_font);
            setupNavigationButton(accessButton, this, R.color.brand_colorOnSurfaceSecondary, R.color.brand_colorTertiary, R.color.primarySecond_light, R.color.primary_light);
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
            setupNavigationButton(finishButton, this, R.color.brand_colorOnSurfaceSecondary, R.color.primary_light, R.color.primary_light, R.color.primary_light_font);

            finishButton.setOnClickListener(v -> {
                Intent intent = new Intent(requireActivity(), MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(intent);
                requireActivity().overridePendingTransition(0, 0); // Disable transition animation
                requireActivity().finish();
            });
        }
    }
}