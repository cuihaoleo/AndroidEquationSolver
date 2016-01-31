package me.cvhc.equationsolver;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    TextView textViewEquation;
    ListView listViewVariables;
    ArrayAdapter<String> adapterVariables;
    ArrayList<String> listVariables = new ArrayList<String>();

    private final String LOG_TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textViewEquation = (TextView)findViewById(R.id.textViewEquation);
        listViewVariables = (ListView)findViewById(R.id.listViewVariables);

        adapterVariables = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, listVariables);
        listViewVariables.setAdapter(adapterVariables);

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

                    listVariables.clear();
                    for (Character c: left.getProperty().Variables) {
                        listVariables.add(c.toString());
                    }
                    for (Character c: right.getProperty().Variables) {
                        listVariables.add(c.toString());
                    }

                    adapterVariables.notifyDataSetChanged();

                }
            }
        });
    }
}
