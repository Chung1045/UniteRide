package com.chung.a9rushtobus.elements;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceViewHolder;
import androidx.preference.SwitchPreferenceCompat;

import com.chung.a9rushtobus.R;
import com.google.android.material.materialswitch.MaterialSwitch;

public class CustomSwitchPreference extends SwitchPreferenceCompat {
    private MaterialSwitch materialSwitch;
    private LinearLayout container;
    private boolean isUserInitiated = true; // Flag to prevent recursive calls
    public CustomSwitchPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setLayoutResource(R.layout.components_switch_preference);
    }

    public CustomSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.components_switch_preference);
    }

    public CustomSwitchPreference(Context context) {
        super(context);
        setLayoutResource(R.layout.components_switch_preference);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        // Find the root container and set the initial background
        container = (LinearLayout) holder.findViewById(R.id.linear_layout_switch_pref);

        if (container != null) {
            container.setOnClickListener(view -> {
                if (isUserInitiated) {
                    handleToggle();
                }
            });
        }

        TextView summaryTextView = (TextView) holder.findViewById(android.R.id.summary);

        if (summaryTextView != null) {
            CharSequence summary = getSummary();
            if (summary == null || summary.length() == 0) {
                summaryTextView.setVisibility(View.GONE);  // Hide when empty
            } else {
                summaryTextView.setVisibility(View.VISIBLE); // Show when not empty
            }
        }

        // Find the MaterialSwitch in the custom layout
        materialSwitch = (MaterialSwitch) holder.findViewById(R.id.switch_widget);
        if (materialSwitch != null) {
            // Remove any existing listener to prevent multiple triggers
            materialSwitch.setOnCheckedChangeListener(null);

            // Set the switch state to match the stored preference value
            materialSwitch.setChecked(isChecked());

            // Listen for changes and update the preference when toggled
            materialSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isUserInitiated) {
                    // Update the preference value and notify listeners
                    isUserInitiated = false; // Prevent recursive calls
                    boolean result = callChangeListener(isChecked); // Notify preference change listeners
                    if (result) {
                        setChecked(isChecked);
                    } else {
                        // If the change was rejected, revert the switch
                        materialSwitch.setChecked(!isChecked);
                    }
                    isUserInitiated = true; // Re-enable for next user interaction
                }
            });
        }
    }

    private void handleToggle() {
        if (materialSwitch != null) {
            boolean newState = !materialSwitch.isChecked();
            isUserInitiated = false; // Prevent recursive calls
            boolean result = callChangeListener(newState); // Notify preference change listeners
            if (result) {
                materialSwitch.setChecked(newState);
                setChecked(newState);
            }
            isUserInitiated = true;
        }
    }

}
