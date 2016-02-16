package me.cvhc.equationsolver;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;


public class FloatSettingView extends FrameLayout {
    private EditText editCoefficient;
    private EditText editExponent;
    private TextView textWarning;
    private float lastValue = Float.NaN;
    private float mAbsMax = Float.POSITIVE_INFINITY;
    private OnInputValueChangedListener mListener = null;

    public FloatSettingView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView();
    }

    public FloatSettingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public FloatSettingView(Context context) {
        super(context);
        initView();
    }

    private void initView() {
        View view = inflate(getContext(), R.layout.floatsetting_view, null);
        addView(view);
        editCoefficient = (EditText)findViewById(R.id.editCoefficient);
        editExponent = (EditText)findViewById(R.id.editExponent);
        textWarning = (TextView)findViewById(R.id.textWarning);

        editCoefficient.addTextChangedListener(new CustomTextWatcher());
        editExponent.addTextChangedListener(new CustomTextWatcher());
    }

    public void setAbsMax(float v) {
        mAbsMax = v;
    }

    public float getInputValue() {
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

    public void setInputValue(float val) {
        int exponent = val == 0.0 ? 0 : (int)Math.log10(Math.abs(val));
        float coefficient = val == 0.0 ? 0.0F : val / (float)Math.pow(10, exponent);
        editExponent.setText(String.valueOf(exponent));
        editCoefficient.setText(String.valueOf(coefficient));
    }

    public interface OnInputValueChangedListener {
        void onInputValueChanged(float val);
    }

    public void setOnInputValueChangedListener(OnInputValueChangedListener eventListener) {
        mListener = eventListener;
    }

    private class CustomTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable s) {
            float val = getInputValue();

            if (val == lastValue) {
                return;
            } else if (mListener != null) {
                mListener.onInputValueChanged(lastValue = val);
            }

            if (Float.isNaN(val)) {
                textWarning.setVisibility(View.VISIBLE);
                textWarning.setText("Invalid number format");

            } else if (Math.abs(val) > mAbsMax) {
                textWarning.setVisibility(View.VISIBLE);
                textWarning.setText("Input number is too big");
            } else {
                textWarning.setVisibility(View.INVISIBLE);
            }
        }
    }
}
