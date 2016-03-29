package me.cvhc.equationsolver;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

public class MainFragment extends Fragment {

    private EditText textEquation;
    private TextView labelROILower, labelROIUpper;
    private double lowerROI, upperROI;
    private ListView listViewIDs;
    private SettingIDAdapter settingIDAdapter;
    private ExpressionEvaluator leftEval, rightEval;
    private HashSet<Character> usedIDs = new HashSet<>();
    private double defaultLowerBound, defaultUpperBound;
    private SharedPreferences sharedPreferences;
    private final String LOG_TAG = MainActivity.class.getSimpleName();

    private class SimpleEquationInputFilter implements InputFilter {
        public CharSequence filter(CharSequence source, int start, int end,
                                   Spanned dest, int dstart, int dend) {
            boolean meetEqual = dest.toString().indexOf('=') != -1;

            for (int i = start; i < end; i++) {
                char c = source.charAt(i);
                if (c == '=') {
                    if (meetEqual) {
                        Toast.makeText(getActivity(), R.string.error_multiple_equal_sign, Toast.LENGTH_SHORT).show();
                        return "";
                    }
                } else if (!Character.isLetter(c) && !Character.isDigit(c)
                        && ".=+-*/^() ".indexOf(c) == -1) {
                    Toast.makeText(getActivity(), R.string.error_illegal_char, Toast.LENGTH_SHORT).show();
                    return "";
                }
            }
            return null;
        }
    }

    private boolean prepareEquation() {
        if (leftEval == null || rightEval == null) {
            new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.error)
                    .setMessage(R.string.error_invalid_equation)
                    .setPositiveButton(android.R.string.yes, null)
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .show();
            return false;
        }

        if (!settingIDAdapter.resolveIDs()) {
            new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.error)
                    .setMessage(R.string.indeterminate_constants)
                    .setPositiveButton(android.R.string.yes, null)
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .show();
            return false;
        }

        if (lowerROI - lowerROI != 0.0 || upperROI - upperROI != 0.0 || lowerROI >= upperROI) {
            new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.error)
                    .setMessage(R.string.invalid_search_range)
                    .setPositiveButton(android.R.string.yes, null)
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .show();
            return false;
        }

        return true;
    }

    public static MainFragment newInstance() {
        MainFragment fragment = new MainFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        // initialize View objects
        labelROILower = (TextView)rootView.findViewById(R.id.labelROILower);
        labelROIUpper = (TextView)rootView.findViewById(R.id.labelROIUpper);
        textEquation = (EditText)rootView.findViewById(R.id.textEquation);
        listViewIDs = (ListView)rootView.findViewById(R.id.listViewVariables);

        Button buttonPlot = (Button)rootView.findViewById(R.id.buttonPlot);
        Button buttonSolve = (Button)rootView.findViewById(R.id.buttonSolve);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        reloadPreferences();
        updateROI(defaultLowerBound, defaultUpperBound);

        buttonPlot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (prepareEquation()) {
                    Intent intent = new Intent(getActivity(), PlotActivity.class)
                            .putExtra("LEFT_PART", leftEval.toString())
                            .putExtra("RIGHT_PART", rightEval.toString())
                            .putExtra("LOWER_BOUND", lowerROI)
                            .putExtra("UPPER_BOUND", upperROI)
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

                    new SolveTask(getActivity(), lowerROI, upperROI).execute(new FunctionWrapper.MathFunction() {
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

        settingIDAdapter = new SettingIDAdapter(getActivity(), usedIDs);
        listViewIDs.setAdapter(settingIDAdapter);

        listViewIDs.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0) {
                    Character idChar = (Character) settingIDAdapter.getItem(position);
                    settingIDAdapter.assignID(idChar, null);
                    settingIDAdapter.notifyDataSetChanged();
                    return true;
                }
                return false;
            }
        });

        listViewIDs.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                final Character id = (Character) settingIDAdapter.getItem(position);
                final Character variable = settingIDAdapter.getVariable();

                if (position == 0) {
                    AlertDialog.Builder selector = new AlertDialog.Builder(getActivity());

                    final ArrayList<Character> items = new ArrayList<>(usedIDs);
                    String[] strings = new String[items.size()];
                    Collections.sort(items);
                    for (int i = 0; i < items.size(); i++) {
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

                    selector.setTitle(R.string.choose_variable_id);
                    selector.show();
                } else {
                    final ExpressionSettingView settingView = new ExpressionSettingView(getActivity());
                    final AlertDialog dialog = new AlertDialog.Builder(getActivity())
                            .setTitle(R.string.setting_id)
                            .setView(settingView)
                            .create();

                    settingView.setPrefixText(id + " = ");
                    settingView.setText(settingIDAdapter.getExpression(id));

                    settingView.setNegativeButtonListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dialog.dismiss();
                        }
                    });

                    settingView.setPositiveButtonListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ExpressionEvaluator eval = settingView.getExpression();
                            if (eval != null) {
                                settingIDAdapter.assignID(id, eval);
                                settingIDAdapter.notifyDataSetChanged();
                                Log.d(LOG_TAG, "Setting " + id + " to " + eval.toString());
                            }
                            dialog.dismiss();
                        }
                    });

                    dialog.show();
                    dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
            }
        });

        textEquation.setFilters(new InputFilter[]{new SimpleEquationInputFilter()});
        textEquation.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String eq = s.toString();
                if (eq.length() == 0) {
                    textEquation.setError(null);
                    return;
                }

                String[] part = eq.split("=", 2);
                Log.d(LOG_TAG, "Equation: " + eq);
                if (part.length != 2) {
                    textEquation.setError(getString(R.string.error_invalid_equation));
                    return;
                }

                ExpressionEvaluator left = new ExpressionEvaluator(part[0].trim());
                ExpressionEvaluator right = new ExpressionEvaluator(part[1].trim());

                if (left.isError() || right.isError()) {
                    leftEval = rightEval = null;
                    textEquation.setError(getString(R.string.error_illegal_expression));
                } else {
                    leftEval = left;
                    rightEval = right;

                    usedIDs.clear();
                    usedIDs.addAll(leftEval.getProperty().Variables);
                    usedIDs.addAll(rightEval.getProperty().Variables);

                    settingIDAdapter.notifyDataSetChanged();
                }
            }
        });

        abstract class updateROIListener implements View.OnClickListener {
            private String title;

            public updateROIListener(String s) {
                title = s;
            }

            abstract void updateValue(double val);

            abstract double getValue();

            abstract double getDefault();

            abstract boolean testValue(double val);

            @Override
            public void onClick(View v) {
                final TextView label = (TextView)v;
                final DecimalSettingView settingView = new DecimalSettingView(getActivity());
                settingView.setInputValue(getValue());
                settingView.setDefaultValue(getDefault());

                AlertDialog.Builder alert = new AlertDialog.Builder(getActivity())
                        .setTitle(title)
                        .setView(settingView)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                double val = settingView.getInputValue().doubleValue();
                                updateValue(val);
                                String s = String.format(getString(R.string.format_bound), val);
                                label.setText(s);
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, null);

                AlertDialog dialog = alert.create();
                dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                dialog.show();

                final Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                settingView.setOnInputValueChangedListener(new DecimalSettingView.OnInputValueChangedListener() {
                    @Override
                    public void onInputValueChanged(Number number) {
                        double val = number.doubleValue();
                        if (testValue(val)) {
                            positiveButton.setEnabled(!(Double.isNaN(val) || Double.isInfinite(val)));
                        } else {
                            settingView.setWarning(getString(R.string.error_lower_bound_higher_than_upper_bound));
                            positiveButton.setEnabled(false);
                        }
                    }
                });
            }
        }

        labelROILower.setOnClickListener(new updateROIListener(getString(R.string.lower_bound_of_roi)) {
            @Override double getDefault() { return defaultLowerBound; }
            @Override void updateValue(double val) { updateROI(val, null); }
            @Override double getValue() { return lowerROI; }
            @Override boolean testValue(double val) { return val < upperROI; }
        });

        labelROIUpper.setOnClickListener(new updateROIListener(getString(R.string.upper_bound_of_roi)) {
            @Override double getDefault() { return defaultUpperBound; }
            @Override void updateValue(double val) { updateROI(null, val); }
            @Override double getValue() { return upperROI; }
            @Override boolean testValue(double val) { return val > lowerROI; }
        });

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        reloadPreferences();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        updateROI(data.getDoubleExtra("LOWER_BOUND", 0.0), data.getDoubleExtra("UPPER_BOUND", 0.0));
    }

    private void reloadPreferences() {
        // load default plotting boundary settings
        double boundThreshold1 = sharedPreferences.getFloat("pref_default_lower_bound", 0.0F);
        double boundThreshold2 = sharedPreferences.getFloat("pref_default_upper_bound", 1.0F);

        if (boundThreshold1 == boundThreshold2) {
            // get "next" float bigger than boundThreshold1
            long bits = Double.doubleToLongBits(boundThreshold1);
            bits++;
            boundThreshold2 = Double.longBitsToDouble(bits);
        }

        defaultLowerBound = Math.min(boundThreshold1, boundThreshold2);
        defaultUpperBound = Math.max(boundThreshold1, boundThreshold2);
    }

    private void updateROI(Double lower, Double upper) {
        if (lower != null) {
            lowerROI = lower;
            labelROILower.setText(String.format(getString(R.string.format_bound), lower));
        }

        if (upper != null) {
            upperROI = upper;
            labelROIUpper.setText(String.format(getString(R.string.format_bound), upper));
        }
    }
}
