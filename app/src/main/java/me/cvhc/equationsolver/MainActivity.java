package me.cvhc.equationsolver;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    TextView textViewEquation;
    ListView listViewIDs;
    ArrayAdapter<String> adapterVariables;

    ArrayList<String> listVariables = new ArrayList<String>();
    ArrayList<Character> listIDs = new ArrayList<>();

    Character variable = '\0';

    static final Character LEFT_ID = '\0';
    static final Character RIGHT_ID = '\1';
    HashMap<Character, ExpressionEvaluator> dictIDs = new HashMap<>();

    private static final List<Character> VARIABLE_CHARS = Arrays.asList('x', 'y', 'z');
    private final String LOG_TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button buttonPlot = (Button) findViewById(R.id.buttonPlot);
        buttonPlot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, PlotActivity.class);
                //intent.putExtra("new_variable_name","value");
                //startActivity(new Intent(MainActivity.this, PlotActivity.class));
                startActivityForResult(intent, 0);
            }
        });


        textViewEquation = (TextView) findViewById(R.id.textViewEquation);
        listViewIDs = (ListView) findViewById(R.id.listViewVariables);

        adapterVariables = new ArrayAdapter<String>(this,
                R.layout.list_view_variables, listVariables);
        listViewIDs.setAdapter(adapterVariables);
        listViewIDs.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                if (position == 0) {
                    AlertDialog.Builder selector = new AlertDialog.Builder(MainActivity.this);

                    ArrayList<String> items = new ArrayList<>();
                    for (char c : listIDs) {
                        items.add(c + " as variable");
                    }

                    String[] arr = items.toArray(new String[items.size()]);
                    selector.setSingleChoiceItems(arr, listIDs.indexOf(variable), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int position) {
                            Character selection = listIDs.get(position);

                            if (selection != variable) {
                                variable = selection;
                                onSettingID(variable);
                            }

                            Log.d(LOG_TAG, "User chose " + selection + " as variable");
                            dialog.dismiss();
                        }

                    });

                    selector.setTitle("Variable of the equation");
                    selector.show();
                } else {
                    final char id = listIDs.get(position);

                    AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
                    alert.setTitle("Setting ID");

                    final EditText input = new EditText(MainActivity.this);
                    if (dictIDs.containsKey(id)) {
                        input.setText(dictIDs.get(id).toString());
                    }
                    input.setSingleLine(true);

                    alert.setView(input);

                    alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int n) {
                            ExpressionEvaluator eval;

                            eval = new ExpressionEvaluator(input.getText().toString());
                            if (eval.isError()) {
                                dialog.dismiss();
                            }

                            dictIDs.put(id, eval);
                            onSettingID(id);
                            Log.d(LOG_TAG, "Setting " + id + " to " + eval.toString());
                        }
                    });

                    alert.setNegativeButton("Cancel", null);
                    alert.show();
                }
            }
        });

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
                    textViewEquation.setBackgroundResource(R.color.red);
                } else {
                    textViewEquation.setBackgroundResource(android.R.color.transparent);

                    HashSet<Character> ids = new HashSet<>(left.getProperty().Variables);
                    ids.addAll(right.getProperty().Variables);

                    if (ids.isEmpty()) {
                        ids.add(VARIABLE_CHARS.get(0));
                    }

                    for (Character c : VARIABLE_CHARS) {
                        if (ids.contains(c)) {
                            variable = c;
                            break;
                        }
                    }

                    dictIDs.put(LEFT_ID, left);
                    dictIDs.put(RIGHT_ID, right);

                    listIDs = new ArrayList<>(ids);
                    Collections.sort(listIDs);

                    if (variable == '\0') {
                        variable = listIDs.get(0);
                    }

                    listIDs.remove(variable);
                    listIDs.add(0, variable);

                    onSettingID(variable);
                }
            }
        });
    }

    protected void onSettingID(char set_id) {
        listVariables.clear();
        listVariables.add("Variable " + variable);

        for (Character id: listIDs) {
            if (id != variable) {
                String s = "Constant " + id;
                if (dictIDs.containsKey(id)) {
                    s += " = " + dictIDs.get(id);
                }
                listVariables.add(s);
            }
        }

        adapterVariables.notifyDataSetChanged();
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
