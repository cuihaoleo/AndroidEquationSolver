package me.cvhc.equationsolver;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;


public class DecimalInputView extends TextView {
    Double mValue;
    String mEditDialogTitle = "Setting";
    Double mDefaultValue;

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
                            label.setValue(settingView.getInputValue());
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null);

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
        setText(mValue.toString());
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
}
