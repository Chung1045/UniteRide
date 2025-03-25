package com.chung.a9rushtobus;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.progressindicator.LinearProgressIndicator;

public class OnboardingActivity extends AppCompatActivity {
    private static final String TAG = "OnboardingActivity";
    private LinearProgressIndicator progressIndicator;
    private MaterialToolbar toolbar;
    final private int totalPages = 5;
    private int currentPage = 1;
    private boolean isNavigating = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_onboarding);

        Log.d(TAG, "onCreate: Activity created");

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // CHANGE COLOR OF STATUS BAR & NAVIGATION BAR
        Window myWindow = this.getWindow();
        myWindow.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        myWindow.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        myWindow.setStatusBarColor(getResources().getColor(R.color.onboarding_appBar));
        myWindow.setNavigationBarColor(getResources().getColor(R.color.onboarding_colorSurface));

        setupToolbar();

        if (savedInstanceState == null) {
            loadFirstFragment();
        } else {
            // Restore state
            currentPage = savedInstanceState.getInt("currentPage", 1);
            Log.d(TAG, "onCreate: Restored state, currentPage = " + currentPage);
            updateToolbarState();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("currentPage", currentPage);
        Log.d(TAG, "onSaveInstanceState: Saving currentPage = " + currentPage);
    }

    private void setupToolbar() {
        toolbar = findViewById(R.id.onBoardingAppBar);
        progressIndicator = toolbar.findViewById(R.id.progress);

        if (progressIndicator == null) {
            Log.e(TAG, "setupToolbar: Progress indicator not found! Check the ID");
        } else {
            progressIndicator.setMax(totalPages);
            progressIndicator.setProgress(currentPage);
            Log.d(TAG, "setupToolbar: Progress indicator initialized with " + currentPage + "/" + totalPages);
        }

        toolbar.setNavigationOnClickListener(v -> {
            Log.d(TAG, "Navigation icon clicked");
            if (!isNavigating) {
                goBackPage();
            } else {
                Log.d(TAG, "Ignoring navigation click during animation");
            }
        });

        updateToolbarState();
    }

    private void updateToolbarState() {
        if (currentPage == 1) {
            toolbar.setNavigationIcon(null);
            Log.d(TAG, "updateToolbarState: Hiding back button (page 1)");
        } else {
            toolbar.setNavigationIcon(R.drawable.back);
            Log.d(TAG, "updateToolbarState: Showing back button (page " + currentPage + ")");
        }
    }

    private void loadFirstFragment() {
        Log.d(TAG, "loadFirstFragment: Loading first fragment");
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainerView_Onboard, new OnboardingFragments.FragmentOnBoarding1())
                .setReorderingAllowed(true)
                .commit();
    }

    private void updateProgressSmoothly(int fromValue, int toValue) {
        if (progressIndicator == null) {
            Log.e(TAG, "updateProgressSmoothly: Progress indicator is null");
            return;
        }

        Log.d(TAG, "updateProgressSmoothly: Animating from " + fromValue + " to " + toValue);

        ValueAnimator animator = ValueAnimator.ofInt(fromValue, toValue);
        animator.setDuration(300);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());

        animator.addUpdateListener(animation -> {
            int animatedValue = (int) animation.getAnimatedValue();
            progressIndicator.setProgress(animatedValue);
        });

        animator.start();
    }

    public void goToNextPage() {
        if (isNavigating) {
            Log.d(TAG, "goToNextPage: Navigation already in progress, ignoring request");
            return;
        }

        if (currentPage < totalPages) {
            isNavigating = true;
            Log.d(TAG, "goToNextPage: Navigating from page " + currentPage + " to page " + (currentPage + 1));

            Fragment nextFragment = null;

            switch (currentPage + 1) {
                case 2:
                    nextFragment = new OnboardingFragments.FragmentOnBoarding2();
                    break;
                case 3:
                    nextFragment = new OnboardingFragments.FragmentOnBoarding3();
                    break;
                case 4:
                    nextFragment = new OnboardingFragments.FragmentOnBoarding4();
                    break;
                case 5:
                    nextFragment = new OnboardingFragments.FragmentOnBoarding5();
                    // Hide toolbar for the last fragment
                    if (toolbar != null) {
                        toolbar.postDelayed(() -> {
                            toolbar.setVisibility(View.GONE);
                            Window myWindow = this.getWindow();
                            myWindow.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                            myWindow.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                            myWindow.setStatusBarColor(getResources().getColor(R.color.onboarding_colorSurface));
                            myWindow.setNavigationBarColor(getResources().getColor(R.color.onboarding_colorSurface));
                        }, 300);
                    }

                    break;
            }

            if (nextFragment != null) {
                int previousPage = currentPage;
                currentPage++;

                // If not the last page, update progress and toolbar
                if (currentPage < totalPages) {
                    updateProgressSmoothly(previousPage, currentPage);
                    updateToolbarState();
                } else {
                    // For last page, just update the progress
                    updateProgressSmoothly(previousPage, currentPage);
                }

                // Simple animation with no layout changes
                if (toolbar != null && toolbar.getVisibility() == View.VISIBLE) {
                    Animation pulseAnimation = AnimationUtils.loadAnimation(this, R.anim.toolbar_pulse);
                    toolbar.startAnimation(pulseAnimation);
                }

                // Start fragment transaction
                getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(
                                R.anim.slide_in_right,
                                R.anim.slide_out_left,
                                R.anim.slide_in_left,
                                R.anim.slide_out_right
                        )
                        .replace(R.id.fragmentContainerView_Onboard, nextFragment)
                        .setReorderingAllowed(true)
                        .commit();

                Log.d(TAG, "goToNextPage: Fragment transaction committed, current page set to " + currentPage);

                // Reset navigation flag after a shorter delay
                toolbar.postDelayed(() -> {
                    isNavigating = false;
                    Log.d(TAG, "Navigation lock released");
                }, 200);
            } else {
                isNavigating = false;
                Log.e(TAG, "goToNextPage: Failed to create next fragment");
            }
        } else {
            Log.d(TAG, "goToNextPage: Already at last page (" + currentPage + ")");
        }
    }

    public void goBackPage() {
        if (isNavigating) {
            Log.d(TAG, "goBackPage: Navigation already in progress, ignoring request");
            return;
        }

        if (currentPage > 1) {
            isNavigating = true;
            Log.d(TAG, "goBackPage: Navigating from page " + currentPage + " to page " + (currentPage - 1));

            Fragment previousFragment = null;

            switch (currentPage - 1) {
                case 1:
                    previousFragment = new OnboardingFragments.FragmentOnBoarding1();
                    break;
                case 2:
                    previousFragment = new OnboardingFragments.FragmentOnBoarding2();
                    break;
                case 3:
                    previousFragment = new OnboardingFragments.FragmentOnBoarding3();
                    break;
                case 4:
                    previousFragment = new OnboardingFragments.FragmentOnBoarding4();
                    break;
            }

            if (previousFragment != null) {
                int oldPage = currentPage;
                currentPage--;

                // If we're navigating back from the last page and toolbar was hidden
                if (oldPage == totalPages && toolbar != null && toolbar.getVisibility() != View.VISIBLE) {
                    toolbar.setVisibility(View.VISIBLE);
                }

                // Update progress indicator
                updateProgressSmoothly(oldPage, currentPage);
                updateToolbarState();

                // Simple animation with no layout changes
                Animation pulseAnimation = AnimationUtils.loadAnimation(this, R.anim.toolbar_pulse);
                if (toolbar != null) {
                    toolbar.startAnimation(pulseAnimation);
                }

                // Start fragment transaction with reverse animations
                getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(
                                R.anim.slide_in_left,  // Reversed from goToNextPage
                                R.anim.slide_out_right, // Reversed from goToNextPage
                                R.anim.slide_in_right,
                                R.anim.slide_out_left
                        )
                        .replace(R.id.fragmentContainerView_Onboard, previousFragment)
                        .setReorderingAllowed(true)
                        .commit();

                Log.d(TAG, "goBackPage: Fragment transaction committed, current page set to " + currentPage);

                // Reset flags after delay
                toolbar.postDelayed(() -> {
                    isNavigating = false;
                    Log.d(TAG, "Navigation lock released");
                }, 200);
            } else {
                isNavigating = false;
                Log.e(TAG, "goBackPage: Failed to create previous fragment");
            }
        } else {
            Log.d(TAG, "goBackPage: Already at first page");
            finish(); // Exit the activity if on first page
        }
    }
}