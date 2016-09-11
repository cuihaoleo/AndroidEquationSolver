package me.cvhc.equationsolver;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.inputmethodservice.Keyboard;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.github.brnunes.swipeablerecyclerview.SwipeableRecyclerViewTouchListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainFragment extends Fragment {
    private final String LOG_TAG = MainFragment.class.getSimpleName();
    private final int HISTORY_SIZE = 10;

    private Activity mActivity;

    private ActionBar mActionBar;
    private RecyclerView mRecyclerView;
    private RecyclerViewAdapter mRecyclerViewAdapter;
    private EditText mEditInputNewExpression;
    private PopupWindow mPopupHistory;

    private ArrayList<String> mAssignmentHistory = new ArrayList<>();
    private int[] mIndexFavoriteInAssignmentHistory = {0};
    private ArrayList<String> mEquationHistory = new ArrayList<>();
    private int[] mIndexFavoriteInEquationHistory = {0};

    private ToggleButton mToggleInputType;
    private Button mButtonAdd;
    private ImageButton mButtonHistory;
    private ExpressionKeypad mExpressionKeypad;
    private TabHost mTabHost;
    private Toast mToast;
    private boolean mDoubleBackToExitPressedOnce = false;
    private View mBisectionSubview;
    private View mBingoSubview;
    private ArrayList<FloatingActionButton> mFabs = new ArrayList<>();

    private SharedPreferences mSharedPreferences;
    private InputMethodManager mInputMethodManager;

    private double mDefaultMinX, mDefaultMaxX, mDefaultBingo;

    public MainFragment() {
    }

    private class CustomTextWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }

        @Override
        public void afterTextChanged(Editable s) {
            String str = s.toString();

            mButtonAdd.setVisibility(str.length() > 0 ? View.VISIBLE : View.GONE);
            if (getExpressionType() == ExpressionType.EQUATION || str.length() == 0) {
                return;
            }

            char id = str.charAt(0);
            String prefix = id + " = ";

            if (!Character.isLowerCase(id) || id == 'x') {
                makeToast(String.format(getString(R.string.error_invalid_id), id));
                mEditInputNewExpression.setText("");
                return;
            }

            if (str.length() == 1) {
                mEditInputNewExpression.setText(prefix);
                mEditInputNewExpression.setSelection(prefix.length());
            } else if (str.length() > 1) {
                if (str.length() < prefix.length()) {
                    mEditInputNewExpression.setText("");
                } else if (!str.startsWith(prefix)) {
                    mEditInputNewExpression.setText(prefix);
                    mEditInputNewExpression.setSelection(prefix.length());
                }
            }
        }
    }

    private class SimpleInputFilter implements InputFilter {
        public CharSequence filter(CharSequence source, int start, int end,
                                   Spanned dest, int dstart, int dend) {
            boolean meetEqual = dest.toString().indexOf('=') != -1;
            char prev = dend > 0 ? dest.toString().charAt(dend - 1) : 0;

            for (int i = start; i < end; i++) {
                char c = source.charAt(i);
                if (c == '=') {
                    if (meetEqual) {
                        makeToast(R.string.error_multiple_equal_sign);
                        return "";
                    }
                } else if (!Character.isLetter(c) && !Character.isDigit(c)
                        && ".=+-*/^() ".indexOf(c) == -1) {
                    makeToast(R.string.error_illegal_char);
                    return "";
                } else if (c == 'E' && !Character.isDigit(prev)) {
                    makeToast(R.string.error_invalid_number_format);
                    return "";
                }
                prev = c;
            }
            return null;
        }
    }

    private class SwipeListener implements SwipeableRecyclerViewTouchListener.SwipeListener {
        @Override
        public boolean canSwipeLeft(int position) {
            return true;
        }

        @Override
        public boolean canSwipeRight(int position) {
            return true;
        }

        private void helper(int[] reverseSortedPositions) {
            for (int position : reverseSortedPositions) {
                mRecyclerViewAdapter.removeItem(position);
            }
        }

        @Override
        public void onDismissedBySwipeLeft(RecyclerView recyclerView, int[] reverseSortedPositions) {
            helper(reverseSortedPositions);
        }

        @Override
        public void onDismissedBySwipeRight(RecyclerView recyclerView, int[] reverseSortedPositions) {
            helper(reverseSortedPositions);
        }
    }

    private boolean submitNewExpression(TextView v) {
        boolean result = false;
        String expression = v.getText().toString();

        if (getExpressionType() == ExpressionType.EQUATION) {
            result = mRecyclerViewAdapter.newItem(expression, true);
            mEquationHistory.add(mIndexFavoriteInEquationHistory[0], expression);
        } else if (getExpressionType() == ExpressionType.ASSIGNMENT) {
            result = mRecyclerViewAdapter.newItem(expression, false);
            mAssignmentHistory.add(mIndexFavoriteInAssignmentHistory[0], expression);
        }

        if (!result) {
            makeToast(R.string.error_illegal_expression);
        } else if (expression.equals(v.getText().toString()))  {
            v.setText("");
        }

        return result;
    }

    public static MainFragment newInstance() {
        MainFragment fragment = new MainFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        // Views
        mActionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerView);
        mEditInputNewExpression = (EditText) rootView.findViewById(R.id.editInputNewExpression);
        mToggleInputType = (ToggleButton) rootView.findViewById(R.id.toggleInputType);
        mButtonAdd = (Button) rootView.findViewById(R.id.buttonAdd);
        mButtonHistory = (ImageButton) rootView.findViewById(R.id.buttonHistory);
        mTabHost = (TabHost) rootView.findViewById(R.id.tabHost);
        mExpressionKeypad = (ExpressionKeypad) rootView.findViewById(R.id.keypadMainActivity);

        mToast = Toast.makeText(getContext(), "", Toast.LENGTH_SHORT);
        mToast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 40);

        mInputMethodManager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        mDefaultMinX = mSharedPreferences.getFloat("pref_default_lower_bound", 0.0F);
        mDefaultMaxX = mSharedPreferences.getFloat("pref_default_upper_bound", 1.0F);
        mDefaultBingo = mSharedPreferences.getFloat("pref_default_bingo", 1.0F);

        // Initialize history, load from shared preferences
        mAssignmentHistory.addAll(
                mSharedPreferences.getStringSet("ASSIGNMENT_HISTORY", new HashSet<String>()));
        mIndexFavoriteInAssignmentHistory[0] = mAssignmentHistory.size();
        mEquationHistory.addAll(
                mSharedPreferences.getStringSet("EQUATION_HISTORY", new HashSet<String>()));
        mIndexFavoriteInEquationHistory[0] = mEquationHistory.size();

        // Initialize tabs
        initTabs();

        // Initialize mRecyclerView
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(layoutManager);

        mRecyclerViewAdapter = new RecyclerViewAdapter();
        mRecyclerViewAdapter.setOnItemChangeListener(new RecyclerViewAdapter.OnItemChangeListener() {
            private int lastCount;

            @Override
            public void onItemChange() {
                Set<Character> unassigned = mRecyclerViewAdapter.getUnassignedConstants();

                if (unassigned == null) {
                    enableModeTab(false);
                } else if (unassigned.size() == 0) {
                    enableModeTab(true);
                } else {
                    List<Character> list = new ArrayList<>(unassigned);
                    java.util.Collections.sort(list);

                    enableModeTab(false);

                    if (mRecyclerViewAdapter.getItemCount() > lastCount) {
                        if (getExpressionType() != ExpressionType.ASSIGNMENT) {
                            mToggleInputType.toggle();
                        }

                        mEditInputNewExpression.setText("");
                        mEditInputNewExpression.append("" + list.get(0));

                        if (getKeyboardState() == KeyboardState.NOTHING) {
                            setKeyboardState(KeyboardState.CUSTOM);
                        }
                    }
                }

                lastCount = mRecyclerViewAdapter.getItemCount();
            }
        });

        mRecyclerView.setAdapter(mRecyclerViewAdapter);
        mRecyclerView.addOnItemTouchListener(
                new SwipeableRecyclerViewTouchListener(mRecyclerView, new SwipeListener()));

        // Initialize mEditInputNewExpression
        mEditInputNewExpression.addTextChangedListener(new CustomTextWatcher());
        mEditInputNewExpression.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        mEditInputNewExpression.setRawInputType(InputType.TYPE_CLASS_TEXT);
        mEditInputNewExpression.setFilters(new InputFilter[]{new SimpleInputFilter()});
        mEditInputNewExpression.setTextIsSelectable(true);  // this will prevent IME from showing up
        mEditInputNewExpression.requestFocus();
        mEditInputNewExpression.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                return (actionId == EditorInfo.IME_ACTION_DONE || event.getKeyCode() == KeyEvent.KEYCODE_ENTER) && submitNewExpression(v);
            }
        });
        mEditInputNewExpression.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setKeyboardState(KeyboardState.CUSTOM);
            }
        });

        // Initialize custom keyboard
        Keyboard keypad = new Keyboard(getContext(), R.xml.keyboard);
        mExpressionKeypad.setKeyboard(keypad);
        mExpressionKeypad.setPreviewEnabled(false);

        // Initialize buttons
        mButtonAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitNewExpression(mEditInputNewExpression);
            }
        });

        mToggleInputType.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    makeToast("Input the equation in the text box");
                    mEditInputNewExpression.setHint(R.string.hint_input_equation);
                } else {
                    makeToast("Assign an ID in the text box");
                    mEditInputNewExpression.setHint(R.string.hint_input_assignment);
                }

                mEditInputNewExpression.setText("");
            }
        });
        mToggleInputType.setChecked(true);

        mButtonHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (getKeyboardState() == KeyboardState.NOTHING) {
                    // Popup keyboard to reserve enough height for history display
                    setKeyboardState(KeyboardState.CUSTOM);
                }
                popupHistory();
            }
        });

        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mActivity = getActivity();

        ExpressionKeypadActionListener listener = new ExpressionKeypadActionListener(mActivity);
        listener.addOnChangeModeListener(new ExpressionKeypadActionListener.OnChangeModeListener() {
            @Override
            public void onChangeMode() {
                setKeyboardState(KeyboardState.SYSTEM);
            }
        });
        mExpressionKeypad.setOnKeyboardActionListener(listener);

        setKeyboardState(KeyboardState.CUSTOM);
    }

    private void setupTabContents(View tab) {
        FloatingActionButton fab = (FloatingActionButton) tab.findViewById(R.id.fab);
        final DecimalInputView thresh1 = (DecimalInputView) tab.findViewById(R.id.threshold1);
        final DecimalInputView thresh2 = (DecimalInputView) tab.findViewById(R.id.threshold2);

        mFabs.add(fab);
        if (tab == mBisectionSubview) {
            thresh1.setDefaultValue(mDefaultMinX);
            thresh2.setDefaultValue(mDefaultMaxX);

            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getContext(), PlotActivity.class);
                    intent.putExtra("EXPRESSION", mRecyclerViewAdapter.pack());
                    intent.putExtra("THRESHOLD", new double[]{thresh1.getValue(), thresh2.getValue()});
                    startActivityForResult(intent, 0);
                }
            });
        } else if (tab == mBingoSubview) {
            thresh1.setDefaultValue(mDefaultBingo);

            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getContext(), PlotActivity.class);
                    intent.putExtra("EXPRESSION", mRecyclerViewAdapter.pack());
                    intent.putExtra("THRESHOLD", new double[]{thresh1.getValue()});
                    startActivityForResult(intent, 0);
                }
            });
        }
    }

    private void initTabs() {
        mTabHost.setup();
        FrameLayout contentView = mTabHost.getTabContentView();

        LayoutInflater inflater = LayoutInflater.from(getContext());
        mBisectionSubview = inflater.inflate(R.layout.bisection_setting_view, contentView, false);
        setupTabContents(mBisectionSubview);
        mBingoSubview = inflater.inflate(R.layout.bingo_setting_view, contentView, false);
        setupTabContents(mBingoSubview);

        TabHost.TabSpec tab1 = mTabHost.newTabSpec("bisection").setIndicator("Bisection");
        TabHost.TabSpec tab2 = mTabHost.newTabSpec("bingo").setIndicator("Bingo");

        tab1.setContent(new TabHost.TabContentFactory() {
            @Override
            public View createTabContent(String tag) {
                return mBisectionSubview;
            }
        });
        tab2.setContent(new TabHost.TabContentFactory() {
            @Override
            public View createTabContent(String tag) {
                return mBingoSubview;
            }
        });

        mTabHost.addTab(tab1);
        mTabHost.addTab(tab2);
        mTabHost.setCurrentTab(0);

        for (int i = 0; i < mTabHost.getTabWidget().getTabCount(); i++) {
            mTabHost.getTabWidget().getChildAt(i).getLayoutParams().height = 60;
        }

        enableModeTab(false);
    }

    private void enableModeTab(boolean visible) {
        boolean prevVis = mTabHost.getVisibility() == View.VISIBLE;
        mTabHost.setVisibility(visible ? View.VISIBLE : View.GONE);

        for (FloatingActionButton fab : mFabs) {
            fab.setEnabled(visible);
            fab.setBackgroundTintList(
                    ColorStateList.valueOf(
                            ContextCompat.getColor(
                                    getContext(),
                                    visible ? R.color.colorAccent : R.color.colorSilver)));
        }

        if (prevVis != visible && visible) {
            setKeyboardState(KeyboardState.NOTHING);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 0 && resultCode == Activity.RESULT_OK) {
            double savedResult = data.getDoubleExtra("LAST_RESULT", Double.NaN);
            mRecyclerViewAdapter.setResult(savedResult);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public boolean onBackPressed() {
        if (getKeyboardState() != KeyboardState.NOTHING) {
            setKeyboardState(KeyboardState.NOTHING);
            return true;
        } else {
            return !tryExit();
        }
    }

    private boolean tryExit() {
        if (mDoubleBackToExitPressedOnce) {
            return true;
        }

        mDoubleBackToExitPressedOnce = true;
        makeToast(R.string.click_twice_to_exit);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mDoubleBackToExitPressedOnce = false;
            }
        }, 2000);

        return false;
    }

    private void makeToast(String msg) {
        int[] location = new int[2];
        mEditInputNewExpression.getLocationOnScreen(location);

        mToast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, location[1]);
        mToast.setText(msg);
        mToast.show();
    }

    private void makeToast(int res) {
        makeToast(getResources().getString(res));
    }

    private void popupHistory() {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View view = inflater.inflate(R.layout.popup_window_list, null);
        ListView listView = (ListView) view.findViewById(R.id.listHistory);
        final int[] index;
        final ArrayList<String> history;

        if (getExpressionType() == ExpressionType.EQUATION) {
            history = mEquationHistory;
            index = mIndexFavoriteInEquationHistory;
        } else {
            history = mAssignmentHistory;
            index = mIndexFavoriteInAssignmentHistory;
        }

        if (history.size() == 0) {
            mPopupHistory = null;
            makeToast(mActivity.getString(R.string.no_history));
        }

        final HistoryListAdapter adapter = new HistoryListAdapter(history, index[0], getContext());
        mPopupHistory = new PopupWindow(view,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);

        mPopupHistory.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        mPopupHistory.setOutsideTouchable(true);
        mPopupHistory.setAnimationStyle(android.R.style.Animation_Dialog);
        mPopupHistory.setTouchable(true);
        mPopupHistory.setFocusable(true);

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPopupHistory.dismiss();
            }
        });

        listView.setAdapter(adapter);
        adapter.setOnItemClickedListener(new HistoryListAdapter.OnItemClickedListener() {
            // clicked on padding
            @Override
            public void onItemClicked(int position) {
                mEditInputNewExpression.setText("");
                mEditInputNewExpression.append((String) adapter.getItem(position));
                mPopupHistory.dismiss();
            }
        });

        adapter.setOnItemPinningStateChangedListener(new HistoryListAdapter.OnItemPinningStateChangedListener() {
            @Override
            public void onItemPinningStateChanged(int position, boolean pinned) {
                String item = (String) adapter.getItem(position);
                int realPosition = history.indexOf(item);

                if (realPosition >= 0) {
                    if (pinned && realPosition >= index[0]) {
                        index[0]++;
                        while (realPosition >= index[0]) {
                            Collections.swap(history, realPosition, realPosition - 1);
                            realPosition--;
                        }
                    } else if (!pinned && realPosition < index[0]) {
                        index[0]--;
                        while (realPosition < index[0]) {
                            Collections.swap(history, realPosition, realPosition + 1);
                            realPosition++;
                        }
                    }
                }
            }
        });

        mPopupHistory.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                SharedPreferences.Editor editor = mSharedPreferences.edit();

                Set<String> assignmentSet = new HashSet<>();
                Set<String> equationSet = new HashSet<>();

                assignmentSet.addAll(mAssignmentHistory.subList(0, mIndexFavoriteInAssignmentHistory[0]));
                equationSet.addAll(mEquationHistory.subList(0, mIndexFavoriteInEquationHistory[0]));

                editor.putStringSet("ASSIGNMENT_HISTORY", assignmentSet);
                editor.putStringSet("EQUATION_HISTORY", equationSet);

                editor.apply();
            }
        });

        mPopupHistory.update();
        mPopupHistory.showAsDropDown(mEditInputNewExpression, 0, -10);
    }

    static class ExpressionType {
        final static int EQUATION = 0xF0F0;
        final static int ASSIGNMENT = 0x0F0F;
    }

    private int getExpressionType() {
        return mToggleInputType.isChecked() ? ExpressionType.EQUATION : ExpressionType.ASSIGNMENT;
    }

    static class KeyboardState {
        final static int NOTHING = 0x0000;
        final static int CUSTOM = 0x0001;
        final static int SYSTEM = 0x0010;
    }

    private int getKeyboardState() {
        int state = 0;

        if (isSoftKeyboardVisible()) {
            state |= KeyboardState.SYSTEM;
        }

        if (mExpressionKeypad.getVisibility() == View.VISIBLE) {
            state |= KeyboardState.CUSTOM;
        }

        return state;
    }

    private void setKeyboardState(int state) {
        if (state != KeyboardState.NOTHING) {
            mEditInputNewExpression.requestFocus();
            mEditInputNewExpression.requestFocusFromTouch();
        }

        mExpressionKeypad.setVisibility((state & KeyboardState.CUSTOM) == 0 ? View.GONE : View.VISIBLE);

        if ((state & KeyboardState.SYSTEM) == 0) {
            mInputMethodManager.hideSoftInputFromWindow(mEditInputNewExpression.getWindowToken(), 0);
        } else {
            mInputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
            mInputMethodManager.showSoftInput(mEditInputNewExpression, InputMethodManager.SHOW_FORCED);
        }
    }

    private static float dpToPx(Context context, float valueInDp) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, valueInDp, metrics);
    }

    public boolean isSoftKeyboardVisible() {
        View rootView = mActivity.findViewById(android.R.id.content);

        if (rootView != null) {
            Rect rect = new Rect();
            rootView.getWindowVisibleDisplayFrame(rect);
            int heightDiff = rootView.getRootView().getHeight() - (rect.bottom - rect.top);
            float thresh = dpToPx(getContext(), 200);
            // if more than 200 dp, it's probably a keyboard...
            return heightDiff > thresh;
        } else {
            return false;
        }
    }
}
