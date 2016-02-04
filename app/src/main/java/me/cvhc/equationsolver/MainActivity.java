package me.cvhc.equationsolver;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    TextView textViewEquation;
    ListView listIDs;
    ArrayAdapter<String> adapterVariables;
    ArrayList<String> listVariables = new ArrayList<String>();

    private static final List<Character> VARIALBE_CHARS = Arrays.asList('x', 'y', 'z');
    private final String LOG_TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textViewEquation = (TextView)findViewById(R.id.textViewEquation);
        listIDs = (ListView)findViewById(R.id.listViewVariables);

        adapterVariables = new ArrayAdapter<String>(this,
                R.layout.list_view_variables, listVariables);
        listIDs.setAdapter(adapterVariables);
        listIDs.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                Toast.makeText(MainActivity.this, "hello", Toast.LENGTH_SHORT).show();
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
                }
                else {
                    textViewEquation.setBackgroundResource(android.R.color.transparent);

                    HashSet<Character> ids = new HashSet<>();
                    char var = '\0';

                    for (Character c: left.getProperty().Variables) { ids.add(c); }
                    for (Character c: right.getProperty().Variables) { ids.add(c); }

                    for (Character c: VARIALBE_CHARS) {
                        if (ids.contains(c)) {
                            var = c;
                            ids.remove(c);
                            break;
                        }
                    }

                    ArrayList<Character> idlist = new ArrayList<>(ids);
                    Collections.sort(idlist);
                    if (var == '\0') {
                        var = idlist.get(0);
                        idlist.remove(0);
                    }

                    listVariables.clear();
                    listVariables.add("Variable " + var);
                    for (Character id: idlist) {
                        listVariables.add("Constant " + id);
                    }
                    //listVariables = new ArrayList<String>(ids);

                    adapterVariables.notifyDataSetChanged();

                }
            }
        });
    }
}
