package me.cvhc.equationsolver;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;


public class DecimalSettingView extends FrameLayout {
    private EditText editCoefficient;
    private EditText editExponent;
    private TextView textWarning;
    private double lastValue = Double.NaN;
    private double mAbsMax = Double.POSITIVE_INFINITY;
    private OnInputValueChangedListener mListener = null;

    public DecimalSettingView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView();
    }

    public DecimalSettingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public DecimalSettingView(Context context) {
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

    public void setAbsMax(Number v) {
        mAbsMax = v.doubleValue();
    }

    public Number getInputValue() {
        String coefficientString = editCoefficient.getText().toString();
        String exponentString = editExponent.getText().toString();

        double coefficient = Double.parseDouble(editCoefficient.getHint().toString());
        int exponent = Integer.parseInt(editExponent.getHint().toString());

        try {
            if (coefficientString.length() > 0) {
                coefficient = Double.parseDouble(coefficientString);
            }

            if (exponentString.length() > 0) {
                exponent = Integer.parseInt(exponentString);
            }
        } catch (NumberFormatException e) {
            return Double.NaN;
        }

        return coefficient * Math.pow(10F, exponent);
    }

    public void setInputValue(Number number) {
        double val = number.doubleValue();
        int exponent = val == 0.0 ? 0 : (int)Math.log10(Math.abs(val));
        double coefficient = val == 0.0 ? 0.0 : val / Math.pow(10, exponent);
        editExponent.setText(String.valueOf(exponent));
        editCoefficient.setText(String.valueOf(coefficient));
    }

    public void setDefaultValue(Number number) {
        double val = number.doubleValue();
        int exponent = val == 0.0 ? 0 : (int)Math.log10(Math.abs(val));
        double coefficient = val == 0.0 ? 0.0 : val / Math.pow(10, exponent);
        editExponent.setHint(String.valueOf(exponent));
        editCoefficient.setHint(String.valueOf(coefficient));
    }

    public void setWarning(CharSequence msg) {
        textWarning.setVisibility(View.VISIBLE);
        textWarning.setText(msg);
    }

    public void setEditable(boolean editable) {
        editExponent.setFocusable(editable);
        editCoefficient.setFocusable(editable);
    }

    public interface OnInputValueChangedListener {
        void onInputValueChanged(Number val);
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
            double val = getInputValue().doubleValue();

            if (val == lastValue) {
                return;
            }

            if (Double.isNaN(val)) {
                textWarning.setVisibility(View.VISIBLE);
                textWarning.setText("Invalid number format");
            } else if (Math.abs(val) > mAbsMax || Double.isInfinite(val)) {
                textWarning.setVisibility(View.VISIBLE);
                textWarning.setText("Input number is too big");
            } else {
                textWarning.setVisibility(View.INVISIBLE);
            }

            if (mListener != null) {
                mListener.onInputValueChanged(lastValue = val);
            }
        }
    }
}
