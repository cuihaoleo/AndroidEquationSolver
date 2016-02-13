package me.cvhc.equationsolver;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;


public class MainActivity extends AppCompatActivity {

    TextView textViewEquation;
    ListView listViewIDs;
    EditText textLower, textUpper;
    SettingIDAdapter settingIDAdapter;

    ExpressionEvaluator leftEval, rightEval;
    HashSet<Character> usedIDs = new HashSet<>();

    private double lowerBound, upperBound;

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
                        && ".=+-*/^() ".indexOf(c) == -1) {
                    Toast.makeText(MainActivity.this, R.string.error_illegal_char, Toast.LENGTH_SHORT).show();
                    return "";
                }
            }
            return null;
        }
    };

    private class SolveTask extends AsyncTask<FunctionWrapper.MathFunction, Double, Double> {
        private ProgressDialog progressDialog;
        FunctionWrapper.MathFunction func;

        private final static int MAX_PROGRESS = 1000;
        private static final int MAX_PARTITION = 16384;
        private static final double ACCEPT_ERROR = 1E-3;
        private static final double ACCEPT_WIDTH = 1E-16;

        @Override
        protected void onPreExecute() {
            progressDialog = new ProgressDialog(MainActivity.this);
            progressDialog.setMessage("Solving...");
            progressDialog.setCancelable(false);
            progressDialog.setMax(MAX_PROGRESS);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setProgressNumberFormat(null);
            progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE,
                    getString(android.R.string.cancel),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            SolveTask.this.cancel(false);
                        }
                    });
            progressDialog.show();
        }

        @Override
        protected Double doInBackground(FunctionWrapper.MathFunction... functions) {
            if (functions.length != 1) {
                throw new RuntimeException();
            }

            func = functions[0];
            Boolean signIsPositive = null;
            Double savedX1 = null, savedX2 = null;
            double partition = 1.0;
            double width = upperBound - lowerBound;
            Double result = null;

            Double y1 = func.call(lowerBound);
            if (!y1.isNaN()) {
                signIsPositive = y1 > 0;
                savedX1 = lowerBound;
            }

            while (partition <= MAX_PARTITION && savedX2 == null && result == null) {
                if (isCancelled()) break;
                publishProgress(partition / MAX_PARTITION * 0.5);

                for (int i=1; i<=partition; i+=2) {
                    double x = lowerBound + i/partition*width;
                    Double y = func.call(x);

                    if (y == 0) {
                        result = x;
                        break;
                    }

                    if (!y.isNaN()) {
                        if (signIsPositive == null) {
                            signIsPositive = y > 0;
                            savedX1 = x;
                        } else if (signIsPositive != (y > 0)) {
                            savedX2 = x;
                            break;
                        }
                    }
                }

                partition *= 2.0;
            }

            if (savedX1 == null || savedX2 == null) {
                return result;
            }

            publishProgress(0.5);

            double lo = Math.min(savedX1, savedX2);
            double hi = Math.max(savedX1, savedX2);

            double y_lo = func.call(lo);
            double y_hi = func.call(hi);
            if (y_hi == 0 || y_lo == 0) {
                result = y_hi == 0 ? hi : lo;
            }

            if (result == null && !isCancelled()) {
                double mid = 0.0, y_mid = Double.POSITIVE_INFINITY;
                while (hi - lo > ACCEPT_WIDTH) {
                    mid = (lo + hi) / 2.0;
                    y_mid = func.call(mid);

                    if (y_mid == 0) { break; }
                    if ((y_mid < 0) == (y_lo > 0)) { hi = mid; } else { lo = mid; }
                }

                result = y_mid < ACCEPT_ERROR ? mid : null;
            }

            publishProgress(1.0);

            return result;
        }

        @Override
        protected void onCancelled(Double aDouble) {
            super.onCancelled(aDouble);
            progressDialog.dismiss();
        }

        @Override
        protected void onProgressUpdate(Double... progress) {
            progressDialog.setProgress((int)(progress[0] * MAX_PROGRESS));
        }

        @Override
        protected void onPostExecute(Double result) {
            progressDialog.dismiss();

            if (result == null) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Result")
                        .setMessage("No result")
                        .setPositiveButton(android.R.string.yes, null)
                        .setIconAttribute(android.R.attr.alertDialogIcon)
                        .show();
            } else {
                double error = func.call(result);
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Result")
                        .setMessage("Result = " + result.toString())
                        .setPositiveButton(android.R.string.yes, null)
                        .setIconAttribute(android.R.attr.dialogIcon)
                        .show();
            }
        }
    }

    private boolean prepareEquation() {
        if (leftEval == null || rightEval == null) {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Error")
                    .setMessage("The input equation is invalid.")
                    .setPositiveButton(android.R.string.yes, null)
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .show();
            return false;
        }

        if (!settingIDAdapter.resolveIDs()) {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Error")
                    .setMessage("Some constants cannot be determined yet.")
                    .setPositiveButton(android.R.string.yes, null)
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .show();
            return false;
        }

        Double lower, upper;
        String lowerString = textLower.getText().toString();
        String upperString = textUpper.getText().toString();

        lower = lowerString.length() == 0 ? Double.valueOf(-1.0) : ExpressionEvaluator.eval(lowerString);
        upper = upperString.length() == 0 ? Double.valueOf(+1.0) : ExpressionEvaluator.eval(upperString);

        if (lower == null || upper == null) {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Error")
                    .setMessage("Invalid search bound.")
                    .setPositiveButton(android.R.string.yes, null)
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .show();
            return false;
        }

        lowerBound = lower;
        upperBound = upper;
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textLower = (EditText)findViewById(R.id.textLower);
        textUpper = (EditText)findViewById(R.id.textUpper);
        Button buttonPlot = (Button)findViewById(R.id.buttonPlot);
        Button buttonSolve = (Button)findViewById(R.id.buttonSolve);

        buttonPlot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (prepareEquation()) {
                    Intent intent = new Intent(MainActivity.this, PlotActivity.class)
                            .putExtra("LEFT_PART", leftEval.toString())
                            .putExtra("RIGHT_PART", rightEval.toString())
                            .putExtra("LOWER_BOUND", lowerBound)
                            .putExtra("UPPER_BOUND", upperBound)
                            .putExtra("CONSTANT_VALUES", settingIDAdapter.getResolvedIDs())
                            .putExtra("VARIABLE", settingIDAdapter.getVariable());
                    startActivityForResult(intent, 0);
                }
            }
        });

        buttonSolve.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (prepareEquation()) {
                    final ExpressionEvaluator left = new ExpressionEvaluator(leftEval.toString());
                    final ExpressionEvaluator right = new ExpressionEvaluator(rightEval.toString());
                    final Character variable = settingIDAdapter.getVariable();

                    left.updateVariables(settingIDAdapter.getResolvedIDs());
                    right.updateVariables(settingIDAdapter.getResolvedIDs());

                    new SolveTask().execute(new FunctionWrapper.MathFunction() {
                        @Override
                        public double call(double x) {
                            left.setVariable(variable, x);
                            right.setVariable(variable, x);
                            return left.getValue() - right.getValue();
                        }
                    });
                }
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

                    selector.setSingleChoiceItems(strings, items.indexOf(variable), new DialogInterface.OnClickListener() {
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
                    leftEval = rightEval = null;
                    textViewEquation.setBackgroundResource(R.color.colorRedAlert);
                } else {
                    textViewEquation.setBackgroundResource(android.R.color.transparent);

                    leftEval = left;
                    rightEval = right;

                    usedIDs.clear();
                    usedIDs.addAll(leftEval.getProperty().Variables);
                    usedIDs.addAll(rightEval.getProperty().Variables);

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

            textLower.setText(String.format(getString(R.string.format_bound), lower));
            textUpper.setText(String.format(getString(R.string.format_bound), upper));

            Log.d(LOG_TAG, "Lower bound: " + lower);
            Log.d(LOG_TAG, "Upper bound: " + upper);
        }
    }
}
