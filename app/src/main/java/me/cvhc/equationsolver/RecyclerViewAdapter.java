package me.cvhc.equationsolver;

import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {
    private ArrayList<ExpressionHolder> items = new ArrayList<>();
    private ArrayList<Double> savedResults = new ArrayList<>();

    private ExpressionCalculator globalEvaluator;
    private OnItemChangeListener onItemChangeListener;
    private Integer selectedEquation = null;

    protected class ViewHolder extends RecyclerView.ViewHolder {
        public CustomCardView mCardView;

        public ViewHolder(View v) {
            super(v);
            mCardView = (CustomCardView) v;
        }
    }

    private class ExpressionHolder {
        public ExpressionRenderer expr;
        public Character id;

        public ExpressionHolder(Character id, ExpressionRenderer expr) {
            this.expr = expr;
            this.id = id;
        }
    }

    @Override
    public RecyclerViewAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.swipeable_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        ExpressionHolder h = items.get(position);
        String main;
        String sub = null;
        String warning = null;

        if (h.id != ' ') {
            main = "<b>" + h.id + " = </b>" + h.expr.toHTML();

            ExpressionCalculator.OptionUnion op = globalEvaluator.evaluate(h.id);
            if (op.getValue() != null) {
                sub = "Evaluated to " + op.getValue().toString();
            }

            holder.mCardView.setButtonVisibility(View.GONE);
        } else {
            main = "<b>F(x) = </b>" + h.expr.toHTML();
            sub = "Equation";

            holder.mCardView.setButtonVisibility(View.VISIBLE);
            holder.mCardView.setOnCheckedChangeListener(null);

            if (selectedEquation == position) {
                holder.mCardView.setChecked(true);

                if (globalEvaluator.setVariable(' ', h.expr)) {
                    Double result = savedResults.get(position);

                    if (!isReady()) {
                        ExpressionCalculator.OptionUnion op = globalEvaluator.evaluate(' ');
                        ArrayList<Character> buf = new ArrayList<>();

                        for (Character c: op.getVariable()) {
                            if (c != 'x' && !globalEvaluator.isSet(c)) {
                                buf.add(c);
                            }
                        }

                        warning = "Unknown constants: " + TextUtils.join(", ", buf);
                        sub = null;
                    } else if (result != null) {
                        sub = "Solution = " + result;
                    } else {
                        sub = "Ready to solve";
                    }
                }
            } else {
                holder.mCardView.setChecked(false);
            }

            holder.mCardView.setOnCheckedChangeListener(new CustomCardView.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(boolean checked) {
                    selectedEquation = position;
                    notifyChange();
                }
            });
        }

        holder.mCardView.setText(Html.fromHtml(main));
        holder.mCardView.setSubtext(sub);
        holder.mCardView.setWarning(warning);
    }

    public void removeItem(int position) {
        items.remove(position);
        savedResults.remove(position);

        if (selectedEquation != null && selectedEquation == position) {
            selectedEquation = null;
        } else if (selectedEquation != null && selectedEquation > position) {
            selectedEquation--;
        }

        notifyChange();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public boolean newItem(CharSequence text, boolean isEquation) {
        char id;
        String expr_str;

        if (isEquation) {  // equation
            id = ' ';
            expr_str = text.toString();
        } else {
            String[] part = text.toString().split("=", 2);
            if (part.length != 2) {
                return false;
            }

            id = part[0].trim().charAt(0);
            expr_str = part[1];

            if (!Character.isLowerCase(id) || id == 'x') {
                return false;
            }

            for (ExpressionHolder h : items) {
                if (h.id == id) {
                    return false;
                }
            }
        }

        ExpressionHolder holder;

        try {
            holder = new ExpressionHolder(id, new ExpressionRenderer(expr_str));
        } catch (Exception e) {
            return false;
        }

        items.add(holder);
        savedResults.add(null);
        boolean notified = notifyChange();

        if (!notified) {
            items.remove(holder);
        }
        return notified;
    }

    public Set<Character> getUnassignedConstants() {
        if (selectedEquation == null) {
            return null;
        }

        globalEvaluator.setVariable(' ', items.get(selectedEquation).expr);
        ExpressionCalculator.OptionUnion op = globalEvaluator.evaluate(' ');

        if (op == null) {
            return null;
        } else if (op.getValue() != null) {
            return new HashSet<>();
        } else {
            Set<Character> set = op.getVariable();
            set.remove('x');
            return set;
        }
    }

    public boolean isReady() {
        Set<Character> v = getUnassignedConstants();
        return v != null && v.size() == 0;
    }

    public boolean notifyChange() {
        if (selectedEquation == null) {
            for (int i=0; i<items.size(); i++) {
                if (items.get(i).id == ' ') {
                    selectedEquation = i;
                }
            }
        }

        globalEvaluator = new ExpressionCalculator();
        for (ExpressionHolder holder: items) {
            if (!globalEvaluator.setVariable(holder.id, holder.expr)) {
                return false;
            }
        }

        notifyDataSetChanged();
        if (onItemChangeListener != null) {
            onItemChangeListener.onItemChange();
        }

        return true;
    }

    public interface OnItemChangeListener {
        void onItemChange();
    }

    public void setOnItemChangeListener(OnItemChangeListener mListener) {
        onItemChangeListener = mListener;
    }

    public void setResult(double n) {
        if (!Double.isNaN(n)) {
            savedResults.set(selectedEquation, n);
            notifyChange();
        }
    }

    public HashMap<Character, String> pack() {
        HashMap<Character, String> result = new HashMap<>();
        for (ExpressionHolder h: items) {
            if (h.id != ' ' || items.get(selectedEquation) == h) {
                result.put(h.id, h.expr.toString());
            }
        }

        return result;
    }
}