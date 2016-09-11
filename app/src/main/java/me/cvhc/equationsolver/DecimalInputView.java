package me.cvhc.equationsolver;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;


public class DecimalInputView extends TextView {
    Double mValue;
    String mEditDialogTitle = "Setting";
    Double mDefaultValue;
    OnValueChangedListener mListener;

    final OnClickListener mOnClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            final DecimalInputView label = (DecimalInputView)v;
            final DecimalSettingView settingView = new DecimalSettingView(getContext());

            AlertDialog.Builder alert = new AlertDialog.Builder(getContext())
                    .setTitle(mEditDialogTitle)
                    .setView(settingView)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Number num = settingView.getInputValue();
                            if (mValue == null || num.doubleValue() != mValue) {
                                label.setValue(num);
                                if (mListener != null) {
                                    mListener.onValueChanged(num);
                                }
                            }

                            InputMethodManager inputManager = (InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                            inputManager.hideSoftInputFromWindow(settingView.getWindowToken(), 0);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            InputMethodManager inputManager = (InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                            inputManager.hideSoftInputFromWindow(settingView.getWindowToken(), 0);
                        }
                    });

            final AlertDialog dialog = alert.create();
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

            if (mValue != null) {
                settingView.setDefaultValue(mValue);
            }

            if (mDefaultValue != null) {
                settingView.setDefaultValue(mDefaultValue);
            }

            dialog.show();
        }
    };

    public DecimalInputView(Context context) {
        super(context);
        initView();
    }

    public DecimalInputView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public DecimalInputView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    public void initView() {
        setOnClickListener(mOnClickListener);
    }

    public void setValue(Number value) {
        mValue = value.doubleValue();
        setText(String.format(getContext().getString(R.string.format_bound), mValue));
    }

    public void setDefaultValue(Number number) {
        mDefaultValue = number.doubleValue();
        setHint(mDefaultValue.toString());
    }

    public double getValue() {
        return mValue == null ? mDefaultValue : mValue;
    }

    public void setDialogTitle(String str) {
        mEditDialogTitle = str;
    }

    public void setOnValueChangedListener(OnValueChangedListener listener) {
        mListener = listener;
    }

    public interface OnValueChangedListener {
        void onValueChanged(Number val);
    }
}
