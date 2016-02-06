package me.cvhc.equationsolver;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

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

    HashMap<Character, Double> dictConstant = new HashMap<>();

    private static final List<Character> VARIALBE_CHARS = Arrays.asList('x', 'y', 'z');
    private final String LOG_TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textViewEquation = (TextView) findViewById(R.id.textViewEquation);
        listViewIDs = (ListView) findViewById(R.id.listViewVariables);

        adapterVariables = new ArrayAdapter<String>(this,
                R.layout.list_view_variables, listVariables);
        listViewIDs.setAdapter(adapterVariables);
        listViewIDs.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                Toast.makeText(MainActivity.this, "hello", Toast.LENGTH_SHORT).show();

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
                                onChangeVariable(variable);
                            }

                            Log.d(LOG_TAG, "User chose " + selection + " as variable");
                            dialog.dismiss();
                        }

                    });

                    selector.setTitle("Variable of the equation");
                    selector.show();
                } else {
                    int id_index = position - 1;
                    final char id = listIDs.get(id_index);

                    AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
                    alert.setTitle("Setting ID");
                    final EditText input = new EditText(MainActivity.this);
                    input.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL);
                    input.setRawInputType(Configuration.KEYBOARD_12KEY);
                    alert.setView(input);

                    alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int n) {
                            Double number = 0.0;
                            //Put actions for OK button here
                            try {
                                number = Double.parseDouble(input.getText().toString());
                            } catch (NumberFormatException e) {
                                dialog.dismiss();
                            }

                            dictConstant.put(id, number);
                            onSettingConstant(id);
                            Log.d(LOG_TAG, "Setting " + id + " to " + number);
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
                        ids.add(VARIALBE_CHARS.get(0));
                    }

                    for (Character c : VARIALBE_CHARS) {
                        if (ids.contains(c)) {
                            variable = c;
                            break;
                        }
                    }

                    listIDs = new ArrayList<>(ids);
                    Collections.sort(listIDs);
                    if (variable == '\0') {
                        variable = listIDs.get(0);
                    }
                    onChangeVariable(variable);

                }
            }
        });
    }

    protected void onChangeVariable(char variable) {
        listVariables.clear();
        listVariables.add("Variable " + variable);

        for (Character id: listIDs) {
            if (id != variable) {
                String s = "Constant " + id;
                if (dictConstant.containsKey(id)) {
                    s += " = " + dictConstant.get(id);
                }
                listVariables.add(s);
            }
        }

        adapterVariables.notifyDataSetChanged();
    }

    protected void onSettingConstant(char constant) {
        listVariables.clear();
        listVariables.add("Variable " + variable);

        for (Character id: listIDs) {
            if (id != variable) {
                String s = "Constant " + id;
                if (dictConstant.containsKey(id)) {
                    s += " = " + dictConstant.get(id);
                }
                listVariables.add(s);
            }
        }

        adapterVariables.notifyDataSetChanged();
    }
}
