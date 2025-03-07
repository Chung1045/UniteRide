package com.chung.a9rushtobus.elements;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import androidx.preference.PreferenceViewHolder;
import androidx.preference.SwitchPreferenceCompat;

import com.chung.a9rushtobus.R;
import com.google.android.material.materialswitch.MaterialSwitch;

public class CustomMainSwitchPreference extends SwitchPreferenceCompat {
    private MaterialSwitch materialSwitch;
    private LinearLayout container;
    private boolean isUserInitiated = true; // Flag to prevent recursive calls

    public CustomMainSwitchPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setLayoutResource(R.layout.components_main_switch);
    }

    public CustomMainSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.components_main_switch);
    }

    public CustomMainSwitchPreference(Context context) {
        super(context);
        setLayoutResource(R.layout.components_main_switch);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        // Find the root container and set the initial background
        container = (LinearLayout) holder.findViewById(R.id.icon_container);
        if (container != null) {
            container.setBackgroundResource(isChecked() ? R.drawable.background_main_switch_on : R.drawable.background_main_switch_off);
        }

        if (container != null) {
            container.setOnClickListener(view -> {
                if (isUserInitiated) {
                    handleToggle();
                }
            });
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
                        updateBackground(isChecked);
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
                updateBackground(newState);
            }
            isUserInitiated = true;
        }
    }

    private void updateBackground(boolean isOn) {
        if (container != null) {
            container.setBackgroundResource(isOn ?
                    R.drawable.background_main_switch_on :
                    R.drawable.background_main_switch_off);
        }
    }
}