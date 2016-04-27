package me.cvhc.equationsolver;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.inputmethodservice.Keyboard;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.text.TextWatcher;
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
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.github.brnunes.swipeablerecyclerview.SwipeableRecyclerViewTouchListener;

import java.util.ArrayList;
import java.util.Arrays;

public class MainFragment extends Fragment {
    private final String LOG_TAG = MainFragment.class.getSimpleName();
    private final int HISTORY_SIZE = 10;

    private ActionBar mActionBar;
    private RecyclerView mRecyclerView;
    private RecyclerViewAdapter mRecyclerViewAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private AutoCompleteTextView mEditInputNewExpression;

    private ArrayAdapter<String> mAutocompleteAssignmentAdapter;
    private ArrayAdapter<String> mAutocompleteEquationAdapter;

    private ToggleButton mToggleInputType;
    private Button mButtonSolve;
    private Button mButtonAdd;
    private ExpressionKeypad mExpressionKeypad;
    private View mDummy;

    private Toast mToast;
    private boolean mDoubleBackToExitPressedOnce = false;

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
                }
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

    public static MainFragment newInstance(int sectionNumber) {
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
        mButtonSolve = (Button) rootView.findViewById(R.id.buttonSolve);
        mButtonAdd = (Button) rootView.findViewById(R.id.buttonAdd);
        mDummy = rootView.findViewById(R.id.dummyFocus);
        mToast = Toast.makeText(getActivity(), "", Toast.LENGTH_SHORT);

        ArrayList<String> equationHistory = new ArrayList<>();
        ArrayList<String> assignmentHistory = new ArrayList<>();

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

        mEditInputNewExpression.setAdapter(mAutocompleteAssignmentAdapter);
        mEditInputNewExpression.setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View v, MotionEvent event){
                if (event.getAction() != MotionEvent.ACTION_UP) {
                    return false;
                }

                if (mEditInputNewExpression.isFocused()) {
                    if (mEditInputNewExpression.getAdapter().getCount() > 0) {
                        mEditInputNewExpression.showDropDown();
                        return true;
                    } else {
                        return false;
                    }
                } else {
                    mEditInputNewExpression.requestFocus();
                    return true;
                }
            }
        });

        mEditInputNewExpression.setOnEditorActionListener(new FinishEditListener());
        mEditInputNewExpression.addTextChangedListener(new CustomTextWatcher());

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        int VERTICAL_ITEM_SPACE = 10;
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.addItemDecoration(new VerticalSpaceItemDecoration(VERTICAL_ITEM_SPACE));

        mLayoutManager = new LinearLayoutManager(this.getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);

        mButtonSolve.setVisibility(View.GONE);
        mButtonSolve.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), PlotActivity.class)
                        .putExtra("EXPRESSION", mRecyclerViewAdapter.pack());
                startActivityForResult(intent, 0);
            }
        });

        mButtonAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitNewExpression(mEditInputNewExpression);
            }
        });

        mRecyclerViewAdapter = new RecyclerViewAdapter();
        mRecyclerViewAdapter.setOnItemChangeListener(new RecyclerViewAdapter.OnItemChangeListener() {
            @Override
            public void onItemChange() {
                if (mRecyclerViewAdapter.isReady()) {
                    mButtonSolve.setVisibility(View.VISIBLE);
                } else {
                    mButtonSolve.setVisibility(View.GONE);
                }
            }
        });

        mRecyclerView.setAdapter(mRecyclerViewAdapter);
        mRecyclerView.addOnItemTouchListener(
                new SwipeableRecyclerViewTouchListener(mRecyclerView, new SwipeListener()));

        Keyboard keypad = new Keyboard(this.getActivity(), R.xml.keyboard);
        mEditInputNewExpression.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        mEditInputNewExpression.setRawInputType(InputType.TYPE_CLASS_TEXT);
        mEditInputNewExpression.setFilters(new InputFilter[]{new SimpleInputFilter()});
        mEditInputNewExpression.setTextIsSelectable(true);  // this will prevent IME from show up
        mEditInputNewExpression.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    showKeypad();
                } else {
                    mDummy.requestFocus();
                    hideKeypad();
                }
            }
        });

        mExpressionKeypad = (ExpressionKeypad) rootView.findViewById(R.id.keypadMainActivity);
        mExpressionKeypad.setKeyboard(keypad);
        mExpressionKeypad.setPreviewEnabled(false);

        ExpressionKeypadActionListener listener = new ExpressionKeypadActionListener(this.getActivity());
        listener.addOnChangeModeListener(new ExpressionKeypadActionListener.OnChangeModeListener() {
            @Override
            public void onChangeMode() {
                hideKeypad();
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                imm.showSoftInput(mEditInputNewExpression, 0);

                mActionBar.hide();
                Snackbar.make(mEditInputNewExpression,
                        R.string.reopen_keyboard, Snackbar.LENGTH_SHORT).show();

                // a workaround to detect system IME disappearing
                rootView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                    @Override
                    public void onLayoutChange(View v, int left, int top, int right, int bottom,
                                               int oldLeft, int oldTop, int oldRight, int oldBottom) {
                        if (bottom > oldBottom * 1.2) {
                            syncIMEState();
                            mRecyclerView.removeOnLayoutChangeListener(this);
                        }
                    }
                });
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
            mDummy.requestFocus();
        } else if (!syncIMEState()) {
            return !tryExit();
        }
        return true;
    }

    private boolean syncIMEState() {
        InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm.isAcceptingText()) {  // OS IME is open, close it
            mActionBar.show();
            imm.hideSoftInputFromWindow(mEditInputNewExpression.getWindowToken(), 0);
            mDummy.requestFocus();
        } else if (mEditInputNewExpression.isFocused()) {  // App-keyboard state is broken, reset it
            mActionBar.show();
            mDummy.requestFocus();
        } else {  // no keyboard, try exit
            return false;
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
        mToast.setText(msg);
        mToast.show();
    }

    private void makeToast(int res) {
        mToast.setText(res);
        mToast.show();    }
}
