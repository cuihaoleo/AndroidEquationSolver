package me.cvhc.equationsolver;

import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

public class IMEDetectActivity extends AppCompatActivity {
    View mActivityRootView;
    OnSoftKeyboardVisibilityChangedListener mListener;
    Boolean mLastState;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    private static float dpToPx(Context context, float valueInDp) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, valueInDp, metrics);
    }

    public void setOnSoftKeyboardVisibilityChangedListener(OnSoftKeyboardVisibilityChangedListener listener) {
        mListener = listener;
        if (mActivityRootView == null) {
            mActivityRootView = findViewById(android.R.id.content);
            mActivityRootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    boolean state = isSoftKeyboardVisible();
                    if (mLastState == null || state != mLastState) {
                        mListener.onSoftKeyboardVisibilityChanged(mLastState = state);
                    }
                }
            });
        }
    }

    public boolean isSoftKeyboardVisible() {
        if (mActivityRootView != null) {
            Rect rect = new Rect();
            mActivityRootView.getWindowVisibleDisplayFrame(rect);
            int heightDiff = mActivityRootView.getRootView().getHeight() - (rect.bottom - rect.top);
            float thresh = dpToPx(IMEDetectActivity.this, 200);
            // if more than 200 dp, it's probably a keyboard...
            return heightDiff > thresh;
        } else {
            return false;
        }
    }

    public interface OnSoftKeyboardVisibilityChangedListener {
        void onSoftKeyboardVisibilityChanged(boolean currentState);
    }
}
