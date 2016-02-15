package me.cvhc.equationsolver;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;


public class FloatSettingPreference extends DialogPreference {
    private EditText editCoefficient;
    private EditText editExponent;
    private Button positiveButton;
    private TextView textWarning;
    private float mCurrrent, mAbsMax;

    public static final float DEFAULT_VALUE = 1.0F;

    public FloatSettingPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray floatSettingType = context.getTheme().obtainStyledAttributes(attrs,
                R.styleable.FloatSettingPreference, 0, 0);
        setDialogLayoutResource(R.layout.floatsetting_dialog);

        mAbsMax = floatSettingType.getFloat(R.styleable.FloatSettingPreference_abs_max, Float.POSITIVE_INFINITY);
    }

    private float getInputValue() {
        String coefficientString = editCoefficient.getText().toString();
        String exponentString = editExponent.getText().toString();

        float coefficient = Float.parseFloat(editCoefficient.getHint().toString());
        int exponent = Integer.parseInt(editExponent.getHint().toString());

        try {
            if (coefficientString.length() > 0) {
                coefficient = Float.parseFloat(coefficientString);
            }

            if (exponentString.length() > 0) {
                exponent = Integer.parseInt(exponentString);
            }
        } catch (NumberFormatException e) {
            return Float.NaN;
        }

        return (float)(coefficient * Math.pow(10F, exponent));
    }


    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);
        AlertDialog dialog = (AlertDialog)getDialog();
        positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        editCoefficient = (EditText) view.findViewById(R.id.editCoefficient);
        editExponent = (EditText) view.findViewById(R.id.editExponent);
        textWarning = (TextView) view.findViewById(R.id.textWarning);

        int exponent = mCurrrent == 0.0 ? 0 : (int)Math.log10(Math.abs(mCurrrent));
        float coefficient = mCurrrent == 0.0 ? 0.0F : mCurrrent / (float)Math.pow(10, exponent);
        editExponent.setText(String.valueOf(exponent));
        editCoefficient.setText(String.valueOf(coefficient));

        class CustomTextWatcher implements TextWatcher {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                float val = getInputValue();
                if (Float.isNaN(val)) {
                    positiveButton.setEnabled(false);
                    textWarning.setVisibility(View.VISIBLE);
                    textWarning.setText("Invalid number format");
                } else if (Math.abs(val) > mAbsMax) {
                    positiveButton.setEnabled(false);
                    textWarning.setVisibility(View.VISIBLE);
                    textWarning.setText("Input number is too big");
                } else {
                    textWarning.setVisibility(View.INVISIBLE);
                    positiveButton.setEnabled(true);
                }
            }
        }

        editCoefficient.addTextChangedListener(new CustomTextWatcher());
        editExponent.addTextChangedListener(new CustomTextWatcher());
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            float newValue = getInputValue();
            if (callChangeListener(newValue)) {
                mCurrrent = newValue;
                persistFloat(mCurrrent);
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
            mCurrrent = getPersistedFloat(DEFAULT_VALUE);
        } else {
            mCurrrent = (float)defaultValue;
            persistFloat(mCurrrent);
        }
    }

    public float getValue() {
        return mCurrrent;
    }
}
