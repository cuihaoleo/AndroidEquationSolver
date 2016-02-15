package me.cvhc.equationsolver;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.NumberPicker;


public class NumberPickerPreference extends DialogPreference {

    private int mMax = 0;
    private int mMin = 0;
    private static int DEFAULT_VALUE = 0;
    private NumberPicker picker;
    private int value;

    public NumberPickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray numberPickerType = context.getTheme().obtainStyledAttributes(attrs,
                R.styleable.NumberPickerPreference, 0, 0);
        mMax = numberPickerType.getInt(R.styleable.NumberPickerPreference_max, 99);
        mMin = numberPickerType.getInt(R.styleable.NumberPickerPreference_min, 0);
        numberPickerType.recycle();
    }

    @Override
    protected View onCreateDialogView() {
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.gravity = Gravity.CENTER;

        picker = new NumberPicker(getContext());
        picker.setLayoutParams(layoutParams);

        FrameLayout dialogView = new FrameLayout(getContext());
        dialogView.addView(picker);

        return dialogView;
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        picker.setMinValue(mMin);
        picker.setMaxValue(mMax);
        picker.setWrapSelectorWheel(false);
        picker.setValue(value);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            picker.clearFocus();
            int newValue = picker.getValue();
            if (callChangeListener(newValue)) {
                value = newValue;
                persistInt(value);
            }
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInt(index, DEFAULT_VALUE);
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        if (restorePersistedValue) {
            value = getPersistedInt(mMin);
        } else {
            value = (Integer)defaultValue;
            persistInt(value);
        }
    }

    public int getValue() {
        return value;
    }
}