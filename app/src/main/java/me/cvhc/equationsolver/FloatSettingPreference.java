package me.cvhc.equationsolver;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;


public class FloatSettingPreference extends DialogPreference {
    private Button positiveButton;
    private float mCurrent, mAbsMax;
    private FloatSettingView settingView;

    public static final float DEFAULT_VALUE = 1.0F;

    public FloatSettingPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray floatSettingType = context.getTheme().obtainStyledAttributes(attrs,
                R.styleable.FloatSettingPreference, 0, 0);

        mAbsMax = floatSettingType.getFloat(R.styleable.FloatSettingPreference_abs_max, Float.POSITIVE_INFINITY);
    }

    @Override
    protected View onCreateDialogView() {
        settingView = new FloatSettingView(getContext());

        settingView.setAbsMax(mAbsMax);
        settingView.setInputValue(mCurrent);

        settingView.setOnInputValueChangedListener(new FloatSettingView.OnInputValueChangedListener() {
            @Override
            public void onInputValueChanged(float val) {
                positiveButton.setEnabled(!(Float.isNaN(val) || Float.isInfinite(val)));
            }
        });

        return settingView;
    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);
        AlertDialog dialog = (AlertDialog)getDialog();
        positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            float newValue = settingView.getInputValue();
            if (callChangeListener(newValue)) {
                mCurrent = newValue;
                persistFloat(mCurrent);
            }
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getFloat(index, DEFAULT_VALUE);
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        if (restorePersistedValue) {
            mCurrent = getPersistedFloat(DEFAULT_VALUE);
        } else {
            mCurrent = (float)defaultValue;
            persistFloat(mCurrent);
        }
    }
}
