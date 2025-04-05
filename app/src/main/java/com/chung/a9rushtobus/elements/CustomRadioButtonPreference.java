package com.chung.a9rushtobus.elements;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.AttributeSet;
import android.widget.RadioButton;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.widget.CompoundButtonCompat;
import androidx.preference.CheckBoxPreference;
import androidx.preference.PreferenceViewHolder;

import com.chung.a9rushtobus.R;

public class CustomRadioButtonPreference extends CheckBoxPreference {

    private RadioButton radioButton;

    public CustomRadioButtonPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWidgetLayoutResource(R.layout.components_radio_option);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        radioButton = (RadioButton) holder.findViewById(R.id.radio_widget);

        if (radioButton != null) {
            boolean isChecked = getPersistedBoolean(false);
            radioButton.setChecked(isChecked);
        }
    }

    @Override
    public void setChecked(boolean checked) {
        super.setChecked(checked);
        if (radioButton != null) {
            radioButton.setChecked(checked);
        }
    }
}
