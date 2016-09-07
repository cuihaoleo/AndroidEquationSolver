package me.cvhc.equationsolver;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.inputmethodservice.Keyboard;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
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
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.github.brnunes.swipeablerecyclerview.SwipeableRecyclerViewTouchListener;

import java.util.ArrayList;
import java.util.Arrays;

public class MainFragment extends Fragment {
    private final String LOG_TAG = MainFragment.class.getSimpleName();
    private final int HISTORY_SIZE = 10;

    private IMEDetectActivity mActivity;

    private ActionBar mActionBar;
    private RecyclerView mRecyclerView;
    private RecyclerViewAdapter mRecyclerViewAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private AutoCompleteTextView mEditInputNewExpression;

    private ArrayAdapter<String> mAutocompleteAssignmentAdapter;
    private ArrayAdapter<String> mAutocompleteEquationAdapter;

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

    private double mDefaultMinX, mDefaultMaxX, mDefaultBingo;

    public MainFragment() {
    }

    private class CustomTextWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable s) {
            String str = s.toString();

            mButtonAdd.setVisibility(str.length() > 0 ? View.VISIBLE : View.GONE);
            if (mToggleInputType.isChecked() || str.length() == 0) {
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

    private class FinishEditListener implements EditText.OnEditorActionListener {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                    event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                return submitNewExpression(v);
            } else {
                return false;
            }
        }
    }

    private boolean submitNewExpression(TextView v) {
        if (mRecyclerViewAdapter.newItem(v.getText(), mToggleInputType.isChecked())) {
            mRecyclerView.smoothScrollToPosition(mRecyclerViewAdapter.getItemCount() - 1);

            if (mToggleInputType.isChecked()) {
                mAutocompleteEquationAdapter.insert(v.getText().toString(), 0);
            } else {
                mAutocompleteAssignmentAdapter.insert(v.getText().toString(), 0);
            }

            v.setText("");
            return true;
        } else {
            makeToast(R.string.error_illegal_expression);
            return false;
        }
    }

    public static MainFragment newInstance() {
        MainFragment fragment = new MainFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @SuppressLint("ShowToast")
    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        mActionBar = ((AppCompatActivity)getActivity()).getSupportActionBar();
        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerView);
        mEditInputNewExpression = (AutoCompleteTextView) rootView.findViewById(R.id.editInputNewExpression);
        mToggleInputType = (ToggleButton) rootView.findViewById(R.id.toggleInputType);
        mButtonAdd = (Button) rootView.findViewById(R.id.buttonAdd);
        mButtonHistory = (ImageButton) rootView.findViewById(R.id.buttonHistory);
        mTabHost = (TabHost) rootView.findViewById(R.id.tabHost);
;
        mToast = Toast.makeText(getActivity(), "", Toast.LENGTH_SHORT);
        mToast.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL, 0, 40);

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        mDefaultMinX = mSharedPreferences.getFloat("pref_default_lower_bound", 0.0F);
        mDefaultMaxX = mSharedPreferences.getFloat("pref_default_upper_bound", 1.0F);
        mDefaultBingo = mSharedPreferences.getFloat("pref_default_bingo", 1.0F);

        ArrayList<String> equationHistory = new ArrayList<>();
        ArrayList<String> assignmentHistory = new ArrayList<>();

        initTabs();

        if (savedInstanceState != null) {
            String[] saved = (String[])savedInstanceState.getSerializable("EQUATION_HISTORY");
            if (saved != null) {
                equationHistory.addAll(Arrays.asList(saved));
            }

            saved = (String[])savedInstanceState.getSerializable("ASSIGNMENT_HISTORY");
            if (saved != null) {
                assignmentHistory.addAll(Arrays.asList(saved));
            }
        }

        mAutocompleteEquationAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_list_item_1, equationHistory);
        mAutocompleteAssignmentAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_list_item_1, assignmentHistory);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        int VERTICAL_ITEM_SPACE = 10;
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.addItemDecoration(new VerticalSpaceItemDecoration(VERTICAL_ITEM_SPACE));

        mLayoutManager = new LinearLayoutManager(this.getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);

        mButtonAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitNewExpression(mEditInputNewExpression);
            }
        });

        mButtonHistory.setOnClickListener(new View.OnClickListener() {
            private boolean visible = false;

            @Override
            public void onClick(View view) {
                if (visible) {
                    mEditInputNewExpression.dismissDropDown();
                    visible = false;
                } else if (mEditInputNewExpression.getAdapter().getCount() > 0) {
                    mEditInputNewExpression.showDropDown();
                    visible = true;
                } else {
                    makeToast("No history available.");
                }
            }
        });

        mRecyclerViewAdapter = new RecyclerViewAdapter();
        mRecyclerViewAdapter.setOnItemChangeListener(new RecyclerViewAdapter.OnItemChangeListener() {
            @Override
            public void onItemChange() {
                FloatingActionButton fab = (FloatingActionButton)mTabHost.getCurrentView().findViewById(R.id.fab);
                if (mRecyclerViewAdapter.isReady()) {
                    enableFabs();
                } else {
                    disableFabs();
                }
            }
        });

        mRecyclerView.setAdapter(mRecyclerViewAdapter);
        mRecyclerView.addOnItemTouchListener(
                new SwipeableRecyclerViewTouchListener(mRecyclerView, new SwipeListener()));

        Keyboard keypad = new Keyboard(this.getActivity(), R.xml.keyboard);
        mExpressionKeypad = (ExpressionKeypad) rootView.findViewById(R.id.keypadMainActivity);
        mExpressionKeypad.setKeyboard(keypad);
        mExpressionKeypad.setPreviewEnabled(false);

        mEditInputNewExpression.setAdapter(mAutocompleteAssignmentAdapter);
        mEditInputNewExpression.setOnEditorActionListener(new FinishEditListener());
        mEditInputNewExpression.addTextChangedListener(new CustomTextWatcher());
        mEditInputNewExpression.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        mEditInputNewExpression.setRawInputType(InputType.TYPE_CLASS_TEXT);
        mEditInputNewExpression.setFilters(new InputFilter[]{new SimpleInputFilter()});
        mEditInputNewExpression.setTextIsSelectable(true);  // this will prevent IME from show up
        mEditInputNewExpression.requestFocus();
        mEditInputNewExpression.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!mActivity.isSoftKeyboardVisible() && mExpressionKeypad.getVisibility() == View.GONE) {
                    showKeypad();
                }
            }
        });

        ExpressionKeypadActionListener listener = new ExpressionKeypadActionListener(this.getActivity());
        listener.addOnChangeModeListener(new ExpressionKeypadActionListener.OnChangeModeListener() {
            @Override
            public void onChangeMode() {
                hideKeypad();
                mEditInputNewExpression.requestFocus();
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                imm.showSoftInput(mEditInputNewExpression, InputMethodManager.SHOW_IMPLICIT);
                Snackbar.make(mEditInputNewExpression,
                        R.string.reopen_keyboard, Snackbar.LENGTH_SHORT).show();
            }
        });
        mExpressionKeypad.setOnKeyboardActionListener(listener);

        mToggleInputType.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    makeToast("Input the equation in the text box");
                    mEditInputNewExpression.setAdapter(mAutocompleteEquationAdapter);
                    mEditInputNewExpression.setHint(R.string.hint_input_equation);
                } else {
                    makeToast("Assign an ID in the text box");
                    mEditInputNewExpression.setAdapter(mAutocompleteAssignmentAdapter);
                    mEditInputNewExpression.setHint(R.string.hint_input_assignment);
                }

                mEditInputNewExpression.setText("");
            }
        });
        mToggleInputType.setChecked(true);

        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mActivity = (IMEDetectActivity) getActivity();
        mActivity.setOnSoftKeyboardVisibilityChangedListener(new IMEDetectActivity.OnSoftKeyboardVisibilityChangedListener() {
            @Override
            public void onSoftKeyboardVisibilityChanged(boolean currentState) {
                Log.d(LOG_TAG, "IME State: " + currentState);
                if (currentState) {
                    hideKeypad();
                    mActionBar.hide();
                } else {
                    mActionBar.show();
                }
            }
        });
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
                    Intent intent = new Intent(getActivity(), PlotActivity.class);
                    intent.putExtra("EXPRESSION", mRecyclerViewAdapter.pack());
                    intent.putExtra("THRESHOLD", new double[]{ thresh1.getValue(), thresh2.getValue() });
                    startActivityForResult(intent, 0);
                }
            });
        } else if (tab == mBingoSubview) {
            thresh1.setDefaultValue(mDefaultBingo);

            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getActivity(), PlotActivity.class);
                    intent.putExtra("EXPRESSION", mRecyclerViewAdapter.pack());
                    intent.putExtra("THRESHOLD", new double[]{ thresh1.getValue() });
                    startActivityForResult(intent, 0);
                }
            });
        }
    }

    private void initTabs() {
        mTabHost.setup();
        FrameLayout contentView = mTabHost.getTabContentView();

        LayoutInflater inflater = LayoutInflater.from(getActivity());
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

        disableFabs();
    }

    private void disableFabs() {
        for (FloatingActionButton fab: mFabs) {
            fab.setEnabled(false);
            fab.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(getContext(), R.color.colorSilver)));
        }
    }

    private void enableFabs() {
        for (FloatingActionButton fab: mFabs) {
            fab.setEnabled(true);
            fab.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(getContext(), R.color.colorAccent)));
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        String[] assignmentHistory = new String[HISTORY_SIZE];
        String[] equationHistory = new String[HISTORY_SIZE];

        for (int i=0; i<HISTORY_SIZE && i<mAutocompleteAssignmentAdapter.getCount(); i++) {
            assignmentHistory[i] = mAutocompleteAssignmentAdapter.getItem(i);
        }

        for (int i=0; i<HISTORY_SIZE && i<mAutocompleteEquationAdapter.getCount(); i++) {
            equationHistory[i] = mAutocompleteEquationAdapter.getItem(i);
        }

        outState.putSerializable("ASSIGNMENT_HISTORY", assignmentHistory);
        outState.putSerializable("EQUATION_HISTORY", equationHistory);
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

    private void showKeypad() {
        if (!isKeypadOn()) {
            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
            mExpressionKeypad.setVisibility(View.VISIBLE);
            mActionBar.hide();
        }
    }

    private void hideKeypad() {
        if (isKeypadOn()) {
            mExpressionKeypad.setVisibility(View.GONE);
            mActionBar.show();
        }
    }

    private boolean isKeypadOn() {
        return mExpressionKeypad.getVisibility() == View.VISIBLE;
    }

    public boolean onBackPressed() {
        if (mExpressionKeypad.getVisibility() == View.VISIBLE) {  // app keypad is open
            hideKeypad();
        } else if (!syncIMEState()) {
            return !tryExit();
        }
        return true;
    }

    private boolean syncIMEState() {
        InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (mActivity.isSoftKeyboardVisible() && imm.isAcceptingText()) {  // OS IME is open, close it
            imm.hideSoftInputFromWindow(mEditInputNewExpression.getWindowToken(), 0);
        } else if (mExpressionKeypad.getVisibility() == View.GONE) {
            if (!mActionBar.isShowing()) {
                // App-keyboard state is broken, reset it
                mActionBar.show();
            } else {  // no keyboard, try exit
                return false;
            }
        }
        return true;
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
        mTabHost.getLocationOnScreen(location);

        mToast.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL, 0, location[1]);
        mToast.setText(msg);
        mToast.show();
    }

    private void makeToast(int res) {
        int[] location = new int[2];
        mTabHost.getLocationOnScreen(location);

        mToast.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL, 0, location[1]);
        mToast.setText(res);
        mToast.show();
    }
}
