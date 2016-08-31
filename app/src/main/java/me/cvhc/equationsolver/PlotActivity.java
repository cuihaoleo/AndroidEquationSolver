package me.cvhc.equationsolver;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PointF;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;
import com.androidplot.xy.XYStepMode;

import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.HashMap;


public class PlotActivity extends AppCompatActivity implements OnTouchListener {

    private DecimalInputView textUpperBound, textLowerBound;
    private XYPlot plot;
    private CheckBox checkXLogScale;
    private HashMap<Character, String> anotherSide;
    private SharedPreferences sharedPreferences;

    private FunctionWrapper mainSeries = null;
    private double minX, maxX;
    private double defaultMinX, defaultMaxX;
    private double maxAbsY;
    private int nZero = 0;

    private static double SCALE_YAXIS = 1.12;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plot);

        // load preferences
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        int prefPlot = sharedPreferences.getInt("pref_plot_samples", 200);

        // initialize View objects
        Button buttonApply = (Button)findViewById(R.id.buttonApply);
        Button buttonReset = (Button)findViewById(R.id.buttonReset);
        textLowerBound = (DecimalInputView)findViewById(R.id.textLowerBound);
        textUpperBound = (DecimalInputView)findViewById(R.id.textUpperBound);
        checkXLogScale = (CheckBox)findViewById(R.id.checkXLogScale);
        plot = (XYPlot)findViewById(R.id.plot);

        assert buttonApply != null;
        assert buttonReset != null;
        assert textLowerBound != null;
        assert textUpperBound != null;
        assert checkXLogScale != null;
        assert plot != null;

        // read data from Intent object
        Intent intent = getIntent();
        final ExpressionCalculator eval = new ExpressionCalculator();
        anotherSide = (HashMap<Character, String>)intent.getSerializableExtra("EXPRESSION");
        for (Character c: anotherSide.keySet()) {
            eval.setVariable(c, anotherSide.get(c));
        }

        double[] threshold = intent.getDoubleArrayExtra("THRESHOLD");
        if (threshold.length == 2) {
            // Bisection mode
            java.util.Arrays.sort(threshold);
            defaultMinX = minX = threshold[0];
            defaultMaxX = maxX = threshold[1];
        } else {
            // find an appropriate range
            double[] range = findBingoRange(threshold[0], eval);
            java.util.Arrays.sort(range);
            defaultMinX = minX = range[0];
            defaultMaxX = maxX = range[1];
        }

        // auto enable Log scale
        if (minX > 0 && maxX > 0 && Math.log10(maxX / minX) > 6) {
            checkXLogScale.setChecked(true);
        }

        DecimalInputView.OnValueChangedListener valueChangedListener = new DecimalInputView.OnValueChangedListener() {
            @Override
            public void onValueChanged(Number val) {
                double[] tmp = { textLowerBound.getValue(), textUpperBound.getValue() };
                java.util.Arrays.sort(tmp);

                minX = checkXLogScale.isChecked() ? logScale(tmp[0]) : tmp[0];
                maxX = checkXLogScale.isChecked() ? logScale(tmp[1]) : tmp[1];

                resetY();
                updatePlotBound();
            }
        };

        textLowerBound.setDialogTitle(getString(R.string.lower_bound_of_roi));
        textLowerBound.setDefaultValue(minX);
        textLowerBound.setOnValueChangedListener(valueChangedListener);
        textUpperBound.setDialogTitle(getString(R.string.upper_bound_of_roi));
        textUpperBound.setDefaultValue(maxX);
        textUpperBound.setOnValueChangedListener(valueChangedListener);

        // listeners
        buttonApply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String warning = null;
                if (nZero == 0) {
                    warning = "Probably no zero point in select range.";
                } else if (nZero > 1) {
                    warning = "It seems there are multiple zero points in select range.";
                }

                if (warning != null) {
                    new AlertDialog.Builder(PlotActivity.this)
                            .setTitle(android.R.string.dialog_alert_title)
                            .setMessage(warning)
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    submitSelectRange();
                                }
                            })
                            .setNegativeButton(android.R.string.no, null)
                            .setIconAttribute(android.R.attr.alertDialogIcon)
                            .show();
                } else {
                    submitSelectRange();
                }
            }
        });

        buttonReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                minX = checkXLogScale.isChecked() ? logScale(defaultMinX) : defaultMinX;
                maxX = checkXLogScale.isChecked() ? logScale(defaultMaxX) : defaultMaxX;

                if (minX >= maxX || !isNormal(minX) || !isNormal(maxX)) {
                    minX = checkXLogScale.isChecked() ? logScale(1e-14) : 0.0;
                    maxX = checkXLogScale.isChecked() ? 0.0 : 1.0;
                }

                resetY();
                updatePlotBound();
            }
        });

        abstract class CustomFormat extends NumberFormat {
            @Override
            abstract public StringBuffer format(double v, StringBuffer b, FieldPosition f);
            @Override
            public StringBuffer format(long v, StringBuffer b, FieldPosition f) { return null; }
            @Override
            public Number parse(String s, ParsePosition p) { return null; }
        }

        checkXLogScale.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    minX = logScale(minX);
                    maxX = logScale(maxX);
                    if (!isNormal(minX) || !isNormal(maxX)) {
                        minX = logScale(Math.pow(10, -14.0));
                        maxX = 0.0;
                    }
                } else {
                    minX = logScaleRecover(minX);
                    maxX = logScaleRecover(maxX);
                }

                mainSeries.resetCache();
                resetY();
                updatePlotBound();
            }
        });

        // plot UI settings
        plot.setOnTouchListener(this);
        plot.setDomainStep(XYStepMode.SUBDIVIDE, 5);
        plot.setDomainValueFormat(new CustomFormat() {
            @Override
            public StringBuffer format(double value, StringBuffer buffer, FieldPosition field) {
                double output = checkXLogScale.isChecked() ? logScaleRecover(value) : value;
                return new StringBuffer(String.format("%6.4g", output));
            }
        });

        plot.setRangeStep(XYStepMode.SUBDIVIDE, 7);
        plot.setRangeValueFormat(new CustomFormat() {
            @Override
            public StringBuffer format(double value, StringBuffer buffer, FieldPosition field) {
                return new StringBuffer(String.format("%6.4g", value));
            }
        });

        plot.getLegendWidget().setVisible(false);
        plot.getTitleWidget().setVisible(false);
        plot.centerOnRangeOrigin(0.0);

        mainSeries = new FunctionWrapper(new FunctionWrapper.MathFunction() {
            @Override
            public double call(double x) {
                if (checkXLogScale.isChecked()) { x = logScaleRecover(x); }

                eval.setVariable('x', x);
                ExpressionCalculator.OptionUnion op = eval.evaluate(' ');

                return op.getValue();
            }
        }, prefPlot);

        plot.addSeries(new XYSeries() {
            final static int DIVIDE = 8;

            @Override
            public int size() {
                return DIVIDE + 1;
            }

            @Override
            public Number getX(int index) {
                return minX + (maxX - minX) / DIVIDE * index;
            }

            @Override
            public Number getY(int index) {
                return 0.0;
            }

            @Override
            public String getTitle() {
                return null;
            }
        }, new LineAndPointFormatter(Color.rgb(255, 0, 0), null, null, null));

        resetY();
        plot.addSeries(mainSeries, new LineAndPointFormatter(Color.rgb(50, 0, 0), null, null, null));
        updatePlotBound();

        if (mainSeries.isAllInvalid()) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.error)
                    .setMessage(R.string.cannot_evaluate_at_the_range)
                    .setPositiveButton(android.R.string.yes, null)
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .show();
        }
    }



    private double[] findBingoRange(double fromX, final ExpressionCalculator eval) {
        FunctionWrapper.MathFunction func = new FunctionWrapper.MathFunction() {
            @Override
            public double call(double x) {
                eval.setVariable('x', x);
                ExpressionCalculator.OptionUnion op = eval.evaluate(' ');
                return op.getValue();
            }
        };

        double x1 = fromX, y1 = func.call(fromX);
        double[] result = new double[]{0, fromX};
        long round = 0;

        while (isNormal(x1) && round < 100) {
            double x2 = x1, y2 = Double.NaN;
            double inc = Math.ulp(x1);
            round ++;

            while (isNormal(x2)) {
                x2 += inc;
                inc *= 10.0;

                if (y1 != (y2 = func.call(x2))) {
                    break;
                }
            }

            double x_inter = (x2*y1 - x1*y2) / (y1-y2);
            double y_inter = func.call(x_inter);
            if (!isNormal(x_inter)) {
                break;
            } else if (Math.signum(y1) * Math.signum(y_inter) == -1) {
                result[0] = x1;
                result[1] = x_inter;
                break;
            }

            x1 = x_inter;
            y1 = y_inter;
        }

        return result;
    }

    private void submitSelectRange() {
        double realMinX = getRealMinX();
        double realMaxX = getRealMaxX();

        final ExpressionCalculator eval = new ExpressionCalculator();
        for (Character c: anotherSide.keySet()) {
            eval.setVariable(c, anotherSide.get(c));
        }

        SolveTask task = new SolveTask(this, realMinX, realMaxX);

        // Don't use AsyncTask.get() to retrieve result, it will block main UI
        task.setOnResultListener(new SolveTask.OnResultListener() {
            @Override
            public void onResult(Double result) {
                if (result != null) {
                    Intent resultData = new Intent();
                    resultData.putExtra("LAST_RESULT", result);
                    PlotActivity.this.setResult(Activity.RESULT_OK, resultData);
                }
            }
        });

        task.execute(new FunctionWrapper.MathFunction() {
            @Override
            public double call(double x) {
                eval.setVariable('x', x);
                return eval.evaluate(' ').getValue();
            }
        });
    }

    private void updatePlotBound() {
        double realMinX = getRealMinX();
        double realMaxX = getRealMaxX();

        textLowerBound.setValue(realMinX);
        textUpperBound.setValue(realMaxX);

        mainSeries.setBound(minX, maxX);
        plot.setDomainBoundaries(minX, maxX, BoundaryMode.FIXED);

        nZero = mainSeries.getNZero();
        plot.setRangeBoundaries(-maxAbsY, maxAbsY, BoundaryMode.FIXED);

        int color_id = R.color.colorPermission;
        if (mainSeries.isAllInvalid() || nZero != 1) {
            color_id = R.color.colorProhibition;
        }

        int color = ContextCompat.getColor(plot.getContext(), color_id);
        plot.getGraphWidget().getGridBackgroundPaint().setColor(color);
        plot.redraw();
    }

    private static final int NONE = 0;
    private static final int ONE_FINGER_DRAG = 1;
    private static final int TWO_FINGERS_DRAG = 2;
    private int mode = NONE;

    private PointF firstFinger;
    private double distBetweenFingers;

    @Override
    public boolean onTouch(View arg0, MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN: // Start gesture
                firstFinger = new PointF(event.getX(), event.getY());
                mode = ONE_FINGER_DRAG;
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                mode = NONE;
                break;
            case MotionEvent.ACTION_POINTER_DOWN: // second finger
                distBetweenFingers = spacing(event);
                // the distance check is done to avoid false alarms
                if (distBetweenFingers > 5f) {
                    mode = TWO_FINGERS_DRAG;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (mode == ONE_FINGER_DRAG) {
                    PointF oldFirstFinger = firstFinger;
                    firstFinger = new PointF(event.getX(), event.getY());
                    scroll(oldFirstFinger.x - firstFinger.x);
                } else if (mode == TWO_FINGERS_DRAG) {
                    double oldDist = distBetweenFingers;
                    distBetweenFingers = spacing(event);

                    if (onDirectionX(event)) {
                        zoom(oldDist / distBetweenFingers);
                    } else {
                        maxAbsY *= oldDist / distBetweenFingers;
                    }
                }

                updatePlotBound();
                break;
        }
        return true;
    }

    private void resetY() {
        mainSeries.setBound(minX, maxX);
        if (mainSeries.isAllInvalid()) {
            maxAbsY = 1.0;
        } else {
            maxAbsY = Math.max(Math.abs(mainSeries.getMaxY()), Math.abs(mainSeries.getMinY()));
            maxAbsY *= SCALE_YAXIS;
        }
    }

    private void zoom(double scale) {
        double domainSpan = maxX - minX;
        double domainMidPoint = maxX - domainSpan / 2.0f;
        double offset = domainSpan * scale / 2.0f;

        double min = domainMidPoint - offset;
        double max = domainMidPoint + offset;

        min = Math.min(min, mainSeries.getX(mainSeries.size() - 1).doubleValue());
        max = Math.max(max, mainSeries.getX(0).doubleValue());

        double realMinX = checkXLogScale.isChecked() ? logScaleRecover(min) : min;
        double realMaxX = checkXLogScale.isChecked() ? logScaleRecover(max) : max;
        if (min < max && isNormal(realMinX) && isNormal(realMaxX)) {
            minX = min;
            maxX = max;
        }
    }

    private void scroll(double pan) {
        double domainSpan = maxX - minX;
        double step = domainSpan / plot.getWidth();
        double offset = pan * step;

        double min = minX + offset;
        double max = maxX + offset;

        double realMinX = checkXLogScale.isChecked() ? logScaleRecover(min) : min;
        double realMaxX = checkXLogScale.isChecked() ? logScaleRecover(max) : max;
        if (min < max && isNormal(realMinX) && isNormal(realMaxX)) {
            minX = min;
            maxX = max;
        }
    }

    private double spacing(MotionEvent event) {
        double x = event.getX(0) - event.getX(1);
        double y = event.getY(0) - event.getY(1);
        return Math.hypot(x, y);
    }

    private boolean onDirectionX(MotionEvent event) {
        double x = event.getX(0) - event.getX(1);
        double y = event.getY(0) - event.getY(1);
        return Math.abs(y) < Math.abs(x);
    }

    private static double logScale(double val) {
        return Math.log(val);
    }

    private static double logScaleRecover(double val) {
        return Math.exp(val);
    }

    private double getRealMinX() {
        return checkXLogScale.isChecked() ? logScaleRecover(minX) : minX;
    }

    private double getRealMaxX() {
        return checkXLogScale.isChecked() ? logScaleRecover(maxX) : maxX;
    }

    private boolean isNormal(double n) {
        return !(Double.isInfinite(n) || Double.isNaN(n));
    }
}
