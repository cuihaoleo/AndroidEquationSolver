package me.cvhc.equationsolver;

import android.content.Context;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;


public class ExpressionSettingView extends FrameLayout {
    Button buttonPositive, buttonNegative;
    EditText editExpression;
    TextView labelExpressionPrefix;

    private class SimpleExpressionInputFilter implements InputFilter {
        public CharSequence filter(CharSequence source, int start, int end,
                                   Spanned dest, int dstart, int dend) {
            for (int i = start; i < end; i++) {
                char c = source.charAt(i);
                if (!Character.isLetter(c) && !Character.isDigit(c) && ".+-*/^() ".indexOf(c) == -1) {
                    Toast.makeText(ExpressionSettingView.this.getContext(),
                            R.string.error_illegal_char, Toast.LENGTH_SHORT).show();
                    return "";
                }
            }
            return null;
        }
    };

    public ExpressionSettingView(Context context) {
        super(context);
        initView();
    }

    public ExpressionSettingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public ExpressionSettingView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    private void initView() {
        View view = inflate(getContext(), R.layout.expression_setting_view, null);
        addView(view);

        labelExpressionPrefix = (TextView)findViewById(R.id.labelExpressionPrefix);
        editExpression = (EditText)findViewById(R.id.editExpression);
        buttonPositive = (Button)findViewById(R.id.buttonPositive);
        buttonNegative = (Button)findViewById(R.id.buttonNegative);

        editExpression.setFilters(new InputFilter[]{new SimpleExpressionInputFilter()});
        editExpression.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String str = s.toString().trim();
                ExpressionEvaluator eval = new ExpressionEvaluator(str);
                if (eval.isError()) {
                    buttonPositive.setEnabled(false);
                    editExpression.setError(getContext().getString(R.string.error_illegal_expression));
                } else {
                    buttonPositive.setEnabled(true);
                }
            }
        });
    }

    void setPositiveButtonListener(OnClickListener listener) {
        buttonPositive.setOnClickListener(listener);
    }

    void setNegativeButtonListener(OnClickListener listener) {
        buttonNegative.setOnClickListener(listener);
    }

    void setPrefixText(CharSequence s) {
        labelExpressionPrefix.setText(s);
    }

    void setText(CharSequence s) {
        editExpression.setText(s);
        editExpression.setError(null);
    }

    ExpressionEvaluator getExpression() {
        String str = editExpression.getText().toString().trim();
        ExpressionEvaluator eval = new ExpressionEvaluator(str);
        return eval.isError() ? null : eval;
    }
}
