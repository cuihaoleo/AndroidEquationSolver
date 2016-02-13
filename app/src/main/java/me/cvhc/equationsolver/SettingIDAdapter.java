package me.cvhc.equationsolver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;


class SettingIDAdapter extends BaseAdapter {
    private static LayoutInflater inflater = null;
    private HashMap<Character, ExpressionEvaluator> assignment = new HashMap<>();
    private List<Character> usedList = null;
    private Set<Character> usedSet = null;
    private HashMap<Character, Double> calculated = null;
    private boolean allResolved = false;
    private Character variable = 'x';

    public SettingIDAdapter(Activity activity,
                            Set<Character> id) {
        inflater = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        usedSet = id;
    }

    @Override
    public int getCount() {
        return usedList == null ? 1 : (usedList.size() + 1);
    }

    @Override
    public Object getItem(int position) {
        if (position == 0) {
            return variable;
        } else {
            return usedList.get(position-1);
        }

    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (convertView == null)
            view = inflater.inflate(R.layout.list_view_setting_id, null);

        TextView textViewIDCharacter = (TextView)view.findViewById(R.id.textViewIDCharacter);
        TextView textViewAssignment = (TextView)view.findViewById(R.id.textViewAssignment);
        TextView textViewCalculated = (TextView)view.findViewById(R.id.textViewCalculated);

        if (position == 0) {
            textViewIDCharacter.setText(String.valueOf(variable));
            textViewAssignment.setText("is variable.");
            textViewCalculated.setText("Tap to change variable's ID.");
        }
        else {
            char id = usedList.get(position - 1);
            ExpressionEvaluator eval = assignment.get(id);

            resolveIDs();
            textViewIDCharacter.setText("" + id);
            textViewAssignment.setText("= " + eval);

            Double val = calculated.get(id);
            if (val == null) {
                textViewCalculated.setText("Cannot be determined yet.");
            } else {
                textViewCalculated.setText(val.toString());
            }
        }

        return view;
    }

    @Override
    public void notifyDataSetChanged() {
        boolean flag = true;
        HashSet<Character> extendedSet = new HashSet<>(usedSet);

        while (flag) {
            flag = false;
            for (Character c: usedSet) {
                if (c != variable) {
                    ExpressionEvaluator eval = assignment.get(c);
                    if (eval != null) {
                        flag = flag || extendedSet.addAll(eval.getProperty().Variables);
                    }
                }
            }
        }

        extendedSet.removeAll(usedSet);
        usedList = new ArrayList<>(usedSet);
        Collections.sort(usedList);

        if (!usedList.isEmpty() && !usedList.contains(variable)) {
            setVariable(usedList.get(0));
        }

        usedList.addAll(extendedSet);
        Collections.sort(usedList);
        usedList.remove(variable);

        super.notifyDataSetChanged();
    }

    public void assignID(char id, ExpressionEvaluator eval) {
        assignment.put(id, eval);
    }

    public String getExpression(char id) {
        return assignment.containsKey(id) ? assignment.get(id).toString() : null;
    }

    public void setVariable(char id) {
        variable = id;
    }

    public Character getVariable() {
        return variable;
    }

    public final boolean resolveIDs() {
        allResolved = true;

        calculated = new HashMap<>();
        calculated.put(variable, null);

        for (Character id: usedList) {
            if (assignment.containsKey(id)) {
                assignment.get(id).resetVariables();
            }
        }

        while (!calculated.keySet().containsAll(usedList)) {
            boolean flag = false;

            for (final Character id: usedList) {
                ExpressionEvaluator eval = assignment.get(id);

                if (id != variable && eval == null) {
                    allResolved = false;
                    continue;
                }

                if (!calculated.containsKey(id) && eval.getProperty().Determined) {
                    final double val = eval.getValue();
                    calculated.put(id, val);
                    flag = true;

                    for (final Character sid : usedList) {
                        if (sid != id && assignment.containsKey(sid)) {
                            ExpressionEvaluator e = assignment.get(sid);
                            e.updateVariables(new HashMap<Character, Double>() {{
                                put(id, val);
                            }});
                        }
                    }
                }
            }

            if (!flag) {
                allResolved = false;
                break;
            }
        }

        calculated.remove(variable);
        return allResolved;
    }

    public final HashMap<Character, Double> getResolvedIDs() {
        return allResolved ? calculated : null;
    }
}