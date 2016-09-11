package me.cvhc.equationsolver;


import android.app.Activity;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.KeyboardView;
import android.text.Editable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;


public class ExpressionKeypadActionListener extends InputMethodService implements KeyboardView.OnKeyboardActionListener {
    Activity targetActivity;
    private final String LOG_TAG = MainActivity.class.getSimpleName();
    private OnChangeModeListener mListener;

    public ExpressionKeypadActionListener(Activity activity) {
        targetActivity = activity;
    }

    @Override public void onPress(int primaryCode) { }
    @Override public void onRelease(int primaryCode) { }

    @Override
    public void onKey(int primaryCode, int[] keyCodes) {
        long eventTime = System.currentTimeMillis();

        if (primaryCode == -1) {
            KeyEvent event = new KeyEvent(eventTime, eventTime,
                    KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER, 0, 0, 0, 0,
                    KeyEvent.FLAG_SOFT_KEYBOARD | KeyEvent.FLAG_KEEP_TOUCH_MODE
                            | KeyEvent.FLAG_EDITOR_ACTION);
            targetActivity.dispatchKeyEvent(event);
        } else if (primaryCode == 0) {
            if (mListener != null) {
                mListener.onChangeMode();
            }
        } else {
            KeyEvent event = new KeyEvent(eventTime, eventTime,
                    KeyEvent.ACTION_DOWN, primaryCode, 0, 0, 0, 0,
                    KeyEvent.FLAG_SOFT_KEYBOARD | KeyEvent.FLAG_KEEP_TOUCH_MODE);
            targetActivity.dispatchKeyEvent(event);
        }
    }

    @Override
    public void onText(CharSequence text) {
        View focusCurrent = targetActivity.getWindow().getCurrentFocus();
        if (focusCurrent == null || !(focusCurrent instanceof EditText) ) {
            Log.d(LOG_TAG, "Cannot get a valid focus editable view.");
            return;
        }

        EditText edittext = (EditText) focusCurrent;
        Editable editable = edittext.getText();

        int start = edittext.getSelectionStart();
        editable.insert(start, text);
    }

    public interface OnChangeModeListener {
        void onChangeMode();
    }

    public void addOnChangeModeListener(OnChangeModeListener listener) {
        mListener = listener;
    }

    @Override public void swipeLeft() { }
    @Override public void swipeRight() { }
    @Override public void swipeDown() { }
    @Override public void swipeUp() { }
}