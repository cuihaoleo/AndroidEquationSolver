package me.cvhc.equationsolver;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

public class SolveTask extends AsyncTask<FunctionWrapper.MathFunction, Double, Double> {
    private Activity parentActivity;
    private double lowerROI, upperROI;
    private ProgressDialog progressDialog;
    FunctionWrapper.MathFunction func;

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

    @Override
    protected Double doInBackground(FunctionWrapper.MathFunction... functions) {
        if (functions.length != 1) {
            throw new RuntimeException();
        }

        func = functions[0];
        Boolean signIsPositive = null;
        Double savedX1 = null, savedX2 = null;
        double partition = 1.0;
        double width = upperROI - lowerROI;
        Double result = null;

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

        if (result == null) {
            new AlertDialog.Builder(parentActivity)
                    .setTitle(R.string.result)
                    .setMessage(R.string.no_result)
                    .setPositiveButton(android.R.string.yes, null)
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .show();
        } else {
            double error = func.call(result);
            ListView listViewIDs = (ListView)parentActivity.findViewById(R.id.listViewVariables);

            View view = listViewIDs.getChildAt(-listViewIDs.getFirstVisiblePosition());
            if(view != null) {
                TextView textViewAssignment = (TextView)view.findViewById(R.id.textViewAssignment);
                textViewAssignment.setText(String.format("= %s", result.toString()));
            }

            AlertDialog.Builder alert = new AlertDialog.Builder(parentActivity)
                    .setTitle(R.string.solution)
                    .setPositiveButton(android.R.string.yes, null)
                    .setIconAttribute(android.R.attr.dialogIcon);

            DecimalSettingView displayView = new DecimalSettingView(alert.getContext());
            displayView.setInputValue(result);
            displayView.setEditable(false);
            displayView.setWarning(String.format("Error = " + parentActivity.getString(R.string.format_bound), error));

            alert.setView(displayView);
            alert.show();
        }
    }
}