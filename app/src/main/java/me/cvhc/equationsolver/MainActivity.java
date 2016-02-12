package me.cvhc.equationsolver;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.InputFilter;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    TextView textViewEquation;
    ListView listViewIDs;
    SettingIDAdapter settingIDAdapter;

    ExpressionEvaluator leftEval, rightEval;
    HashSet<Character> usedIDs = new HashSet<>();

    private final String LOG_TAG = MainActivity.class.getSimpleName();

    private InputFilter simpleInputFilter = new InputFilter() {
        public CharSequence filter(CharSequence source, int start, int end,
                                   Spanned dest, int dstart, int dend) {
            boolean meetEqual = dest.toString().indexOf('=') != -1;

            for (int i = start; i < end; i++) {
                char c = source.charAt(i);
                if (c == '=') {
                    if (meetEqual) {
                        Toast.makeText(MainActivity.this, R.string.error_multiple_equal_sign, Toast.LENGTH_SHORT).show();
                        return "";
                    }
                } else if (!Character.isLetter(c) && !Character.isDigit(c)
                        && "=+-*/^() ".indexOf(c) == -1) {
                    Toast.makeText(MainActivity.this, R.string.error_illegal_char, Toast.LENGTH_SHORT).show();
                    return "";
                }
            }
            return null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button buttonPlot = (Button) findViewById(R.id.buttonPlot);
        buttonPlot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, PlotActivity.class);
                final HashMap<Character, Double> constValues = settingIDAdapter.resolveIDs();

                if (constValues == null) {
                    // TODO: give some error message
                    return;
                }

                intent.putExtra("LEFT_PART", leftEval.toString());
                intent.putExtra("RIGHT_PART", rightEval.toString());
                intent.putExtra("CONSTANT_VALUES", constValues);
                intent.putExtra("VARIABLE", settingIDAdapter.getVariable());

                startActivityForResult(intent, 0);
            }
        });

        textViewEquation = (TextView) findViewById(R.id.textViewEquation);
        listViewIDs = (ListView) findViewById(R.id.listViewVariables);

        settingIDAdapter = new SettingIDAdapter(this, usedIDs);
        listViewIDs.setAdapter(settingIDAdapter);
        listViewIDs.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                final Character id = (Character)settingIDAdapter.getItem(position);
                final Character variable = settingIDAdapter.getVariable();

                if (position == 0) {
                    AlertDialog.Builder selector = new AlertDialog.Builder(MainActivity.this);

                    final ArrayList<Character> items= new ArrayList<>(usedIDs);
                    String[] strings = new String[items.size()];
                    Collections.sort(items);
                    for (int i=0; i<items.size(); i++) {
                        strings[i] = items.get(i) + " as variable";
                    }

                    selector.setSingleChoiceItems(strings, 0, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int position) {
                            Character selection = items.get(position);

                            if (selection != variable) {
                                settingIDAdapter.setVariable(selection);
                                settingIDAdapter.notifyDataSetChanged();
                            }

                            Log.d(LOG_TAG, "User chose " + selection + " as variable");
                            dialog.dismiss();
                        }

                    });

                    selector.setTitle("Variable of the equation");
                    selector.show();
                } else {
                    AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
                    final EditText input = new EditText(MainActivity.this);
                    String exp = settingIDAdapter.getExpression(id);

                    alert.setTitle("Setting ID");
                    alert.setView(input);

                    input.setText(exp == null ? "" : exp);
                    input.setSingleLine(true);
                    input.setFilters(new InputFilter[]{simpleInputFilter});

                    final AlertDialog dialog = alert.create();

                    input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                        @Override
                        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                            if (actionId == EditorInfo.IME_ACTION_DONE) {
                                ExpressionEvaluator eval;
                                eval = new ExpressionEvaluator(input.getText().toString());
                                if (eval.isError()) {
                                    Toast.makeText(MainActivity.this, R.string.error_illegal_expression, Toast.LENGTH_SHORT).show();
                                    return true;
                                } else {
                                    settingIDAdapter.assignID(id, eval);
                                    settingIDAdapter.notifyDataSetChanged();
                                    Log.d(LOG_TAG, "Setting " + id + " to " + eval.toString());
                                    dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
                                    dialog.dismiss();
                                    return false;
                                }
                            } else {
                                return false;
                            }
                        }

                    });

                    dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                    dialog.show();
                }
            }
        });

        textViewEquation.setFilters(new InputFilter[]{simpleInputFilter});
        textViewEquation.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String eq = s.toString();
                String[] part = eq.split("=", 2);

                Log.d(LOG_TAG, "Equation: " + eq);
                if (part.length != 2) {
                    return;
                }

                ExpressionEvaluator left = new ExpressionEvaluator(part[0]);
                ExpressionEvaluator right = new ExpressionEvaluator(part[1]);

                Log.d(LOG_TAG, "left part: " + part[0]);
                Log.d(LOG_TAG, "right part: " + part[1]);

                if (left.isError() || right.isError()) {
                    textViewEquation.setBackgroundResource(R.color.colorRedAlert);
                } else {
                    textViewEquation.setBackgroundResource(android.R.color.transparent);

                    leftEval = left;
                    rightEval = right;

                    usedIDs.clear();
                    usedIDs.addAll(left.getProperty().Variables);
                    usedIDs.addAll(right.getProperty().Variables);

                    settingIDAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (resultCode == RESULT_OK) {
            double lower = data.getDoubleExtra("LOWER_BOUND", 0.0);
            double upper = data.getDoubleExtra("UPPER_BOUND", 0.0);
            Log.d(LOG_TAG, "Lower bound: " + lower);
            Log.d(LOG_TAG, "Upper bound: " + upper);
        }
    }
}
