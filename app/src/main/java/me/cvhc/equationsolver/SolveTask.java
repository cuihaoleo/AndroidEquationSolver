package me.cvhc.equationsolver;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import java.text.DecimalFormat;

/**
 * SolveTask performs bisection method to solve equation F(x)=0 in background.
 *
 * When running, a progress dialog is showed and allows user to cancel the task.
 * If it finds a solution, the task will end successfully and show the solution
 * in a DecimalSettingView wrapped by an AlertDialog.
 */
public class SolveTask extends AsyncTask<FunctionWrapper.MathFunction, Double, Double> {
    private Activity parentActivity;
    private double lowerROI, upperROI;
    private ProgressDialog progressDialog;
    private FunctionWrapper.MathFunction func;

    private OnResultListener mOnResultListener = null;

    private final static int MAX_PROGRESS = 1000;
    private static final int MAX_PARTITION = 16384;
    private static final double ACCEPT_ERROR = 1E-3;

    public SolveTask(Activity activity, double lower, double upper) {
        parentActivity = activity;
        lowerROI = lower;
        upperROI = upper;
    }

    @Override
    protected void onPreExecute() {
        progressDialog = new ProgressDialog(parentActivity);
        progressDialog.setMessage("Solving...");
        progressDialog.setCancelable(false);
        progressDialog.setMax(MAX_PROGRESS);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setProgressNumberFormat(null);
        progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE,
                parentActivity.getString(android.R.string.cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SolveTask.this.cancel(true);
                    }
                });
        progressDialog.show();
    }

    /**
     * Bisection method, running in background thread. It tries to solve
     * equation F(x)=0 in the interval [lowerROI, upperROI]. The function F(x)
     * is wrapped in a MathFunction object.
     *
     * @param functions The function F(x) representing the equation
     * @return Any root of F(x)=0 in the interval, can be null if no root found.
     */
    @Override
    protected Double doInBackground(FunctionWrapper.MathFunction... functions) {
        if (functions.length != 1) {
            throw new RuntimeException();
        } else {
            func = functions[0];
        }

        Boolean signIsPositive = null;
        Double savedX1 = null, savedX2 = null;
        double partition = 1.0;
        double width = upperROI - lowerROI;
        Double result = null;

        /**
         * First, find a pair of x1, x2 (savedX1 and savedX2) which make F(x1)
         * and F(x2) have opposite signs.
         *
         * If x1=upperROI and x2=lowerROI works, the loop exit immediately. Or
         * else it divides the interval [lowerROI, upperROI] to 2, 4, 8... 2^n
         * (2^n<=MAX_PARTITION) partitions and iterates every partition point to
         * detect sign change.
         *
         * If no such x1, x2 in the range, the loop can waste a lot of time.
         */
        Double y1 = func.call(lowerROI);
        if (!y1.isNaN()) {
            signIsPositive = y1 > 0;
            savedX1 = lowerROI;
        }

        while (partition <= MAX_PARTITION && savedX2 == null && result == null) {
            if (isCancelled()) break;
            publishProgress(partition / MAX_PARTITION * 0.5);

            for (int i=1; i<=partition; i+=2) {
                double x = lowerROI + i/partition*width;
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

        /**
         * Second, perform Bisection method in the interval [x1, x2]. The loop
         * stops when:
         *     1. Meet a point x where F(x)=0.0 exactly
         *     2. Bisection doesn't shrink the interval [lo, hi] (due to the
         *        precision limit of Double type)
         *
         * If final result x makes F(x) < ACCEPT_ERROR (a very small positive
         * number), it is accepted and returned. Or else null is returned.
         */
        double lo = Math.min(savedX1, savedX2);
        double hi = Math.max(savedX1, savedX2);

        double y_lo = func.call(lo);
        double y_hi = func.call(hi);
        if (y_hi == 0 || y_lo == 0) {
            result = y_hi == 0 ? hi : lo;
        }

        if (result == null && !isCancelled()) {
            double mid, y_mid;
            double w = hi - lo;
            while (true) {
                mid = (lo + hi) / 2.0;
                y_mid = func.call(mid);

                if (y_mid == 0) { break; }
                if ((y_mid < 0) == (y_lo > 0)) { hi = mid; } else { lo = mid; }

                double nw = hi - lo;
                if (nw >= w) { break; } else { w = nw; }
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

        if (mOnResultListener != null) {
            mOnResultListener.onResult(result);
        }

        if (result == null) {
            new AlertDialog.Builder(parentActivity)
                    .setTitle(R.string.result)
                    .setMessage(R.string.no_result)
                    .setPositiveButton(android.R.string.yes, null)
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .show();
        } else {
            double error = func.call(result);

            AlertDialog.Builder alert = new AlertDialog.Builder(parentActivity)
                    .setTitle(R.string.solution)
                    .setPositiveButton(android.R.string.yes, null)
                    .setIconAttribute(android.R.attr.dialogIcon);

            LayoutInflater inflater = parentActivity.getLayoutInflater();
            View view = inflater.inflate(R.layout.result_display, null);

            TextView textResult = (TextView) view.findViewById(R.id.textResult);
            textResult.setText(Html.fromHtml("<b>x = </b>" + renderNumber(result, true)));
            textResult.setTag(renderNumber(result, false));

            TextView textWarning = (TextView) view.findViewById(R.id.textWarning);
            textWarning.setText("y(x) = " + renderNumber(func.call(result), false));

            alert.setView(view);
            alert.show();
        }
    }

    public interface OnResultListener {
        void onResult(Double result);
    }

    public void setOnResultListener(OnResultListener listener) {
        mOnResultListener = listener;
    }

    private String renderNumber(double n, boolean useHtml) {
        String str1 = (new DecimalFormat("0.0#########")).format(n);
        String str2 = (new DecimalFormat("0.0#####E0")).format(n);
        String displayStr = str1.length() < str2.length() ? str1 : str2;
        ExpressionRenderer expr;

        try {
            expr = new ExpressionRenderer(displayStr);
        } catch (Exception e) {
            return displayStr;
        }

        return useHtml ? expr.toHTML() : displayStr;
    }
}