package com.chung.a9rushtobus;

import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.CheckBoxPreference;

public class CustomRadioButtonPreference extends CheckBoxPreference {

    public CustomRadioButtonPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setWidgetLayoutResource(R.layout.preference_widget_radiobutton);
    }

    public CustomRadioButtonPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWidgetLayoutResource(R.layout.preference_widget_radiobutton);
    }

    public CustomRadioButtonPreference(Context context) {
        this(context, null);
    }

    @Override
    public void onClick() {
        if (this.isChecked()) {
            return;
        }
        super.onClick();
    }
}