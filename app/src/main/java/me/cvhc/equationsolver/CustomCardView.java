package me.cvhc.equationsolver;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.text.Editable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

public class CustomCardView extends CardView {
    private EditText mEditExpression;
    private TextView mLabelSubtext;
    private TextView mLabelWarningMessage;
    private CheckBox mCheckActivate;
    private OnCheckedChangeListener mOnCheckedChangeListener;

    public CustomCardView(Context context) {
        super(context);
        initView();
    }

    public CustomCardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public CustomCardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    private void initView() {
        inflate(getContext(), R.layout.custom_card_view, this);
        mEditExpression = (EditText)findViewById(R.id.editExpression);
        mLabelSubtext = (TextView)findViewById(R.id.labelSubtext);
        mLabelWarningMessage = (TextView)findViewById(R.id.labelWarningMessage);
        mCheckActivate = (CheckBox)findViewById(R.id.checkActivate);

        mCheckActivate.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mEditExpression.setEnabled(true);
                } else {
                    mEditExpression.setEnabled(false);
                }

                if (mOnCheckedChangeListener != null) {
                    mOnCheckedChangeListener.onCheckedChanged(isChecked);
                }
            }
        });
        mCheckActivate.setChecked(true);

        // todo: edit submitted expressions
        mEditExpression.setFocusable(false);
    }

    public void setButtonVisibility(int visibility) {
        mCheckActivate.setVisibility(visibility);
    }

    public void setText(CharSequence s) {
        mEditExpression.setText(s);
    }

    public void setSubtext(CharSequence s) {
        if (s != null && s.length() > 0) {
            mLabelSubtext.setVisibility(VISIBLE);
            mLabelSubtext.setText(s);
        } else {
            mLabelSubtext.setVisibility(GONE);
        }
    }

    public void setWarning(CharSequence s) {
        if (s != null && s.length() > 0) {
            mLabelWarningMessage.setVisibility(VISIBLE);
            mLabelWarningMessage.setText(s);
        } else {
            mLabelWarningMessage.setVisibility(GONE);
        }
    }

    public Editable getText() {
        return mEditExpression.getText();
    }

    public void setChecked(boolean checked) {
        mCheckActivate.setChecked(checked);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return false;
    }

    public void setOnCheckedChangeListener(OnCheckedChangeListener listener) {
        mOnCheckedChangeListener = listener;
    }

    public interface OnCheckedChangeListener {
        void onCheckedChanged(boolean checked);
    }
}
