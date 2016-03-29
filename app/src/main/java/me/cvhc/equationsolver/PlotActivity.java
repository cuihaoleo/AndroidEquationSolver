package me.cvhc.equationsolver;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PointF;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.TextView;

import com.androidplot.Plot;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYStepMode;

import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.HashMap;


public class PlotActivity extends AppCompatActivity implements OnTouchListener {

    private TextView textUpperBound, textLowerBound;
    private TextView textLargeWarning;
    private XYPlot plot;
    private CheckBox checkXLogScale, checkYLogScale;
    SharedPreferences sharedPreferences;

    private FunctionWrapper mainSeries = null;
    private double minX, maxX;
    private int nZero = 0;

    private static double SCALE_YAXIS = 1.12;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plot);

        // initialize View objects
        Button buttonApply = (Button) findViewById(R.id.buttonApply);
        ImageButton buttonReset = (ImageButton)findViewById(R.id.buttonReset);
        textLowerBound = (TextView)findViewById(R.id.textLowerBound);
        textUpperBound = (TextView)findViewById(R.id.textUpperBound);
        textLargeWarning = (TextView)findViewById(R.id.textLargeWarning);
        checkXLogScale = (CheckBox)findViewById(R.id.checkXLogScale);
        checkYLogScale = (CheckBox)findViewById(R.id.checkYLogScale);
        plot = (XYPlot)findViewById(R.id.plot);

        // read data from Intent object
        Intent intent = getIntent();

        minX = intent.getDoubleExtra("LOWER_BOUND", -1.0F);
        maxX = intent.getDoubleExtra("UPPER_BOUND", 1.0F);
        String leftPart = intent.getStringExtra("LEFT_PART");
        String rightPart = intent.getStringExtra("RIGHT_PART");
        final Character variable = intent.getCharExtra("VARIABLE", 'x');
        HashMap<Character, Double> constValues = (HashMap<Character, Double>)intent.getSerializableExtra("CONSTANT_VALUES");

        // load preferences
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        int prefPlot = sharedPreferences.getInt("pref_plot_samples", 200);

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
                float min = sharedPreferences.getFloat("pref_default_lower_bound", 0.0F);
                float max = sharedPreferences.getFloat("pref_default_upper_bound", 1.0F);

                minX = checkXLogScale.isChecked() ? logScale(min) : min;
                maxX = checkXLogScale.isChecked() ? logScale(max) : min;

                if (minX >= maxX || Double.isInfinite(minX) || Double.isNaN(minX)
                        || Double.isInfinite(maxX) || Double.isNaN(maxX)) {
                    minX = checkXLogScale.isChecked() ? logScale(1e-14) : 0.0;
                    maxX = checkXLogScale.isChecked() ? 0.0 : 1.0;
                }

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
                    if (Double.isNaN(minX) || Double.isNaN(maxX)
                            || Double.isInfinite(minX) || Double.isInfinite(maxX)) {
                        minX = logScale(Math.pow(10, -14.0));
                        maxX = 0.0;
                    }
                } else {
                    minX = logScaleRecover(minX);
                    maxX = logScaleRecover(maxX);
                }

                mainSeries.resetCache();
                updatePlotBound();
            }
        });

        checkYLogScale.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mainSeries.resetCache();
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
                double output = checkYLogScale.isChecked() ? log1pScaleRecover(value) : value;
                return new StringBuffer(String.format("%6.4g", output));
            }
        });

        plot.getLegendWidget().setVisible(false);
        plot.getTitleWidget().setVisible(false);
        plot.centerOnRangeOrigin(0.0);

        // construct the series to plot
        final ExpressionEvaluator left = new ExpressionEvaluator(leftPart);
        final ExpressionEvaluator right = new ExpressionEvaluator(rightPart);

        left.updateVariables(constValues);
        right.updateVariables(constValues);

        mainSeries = new FunctionWrapper(new FunctionWrapper.MathFunction() {
            @Override
            public double call(double x) {
                if (checkXLogScale.isChecked()) { x = logScaleRecover(x); }

                left.setVariable(variable, x);
                right.setVariable(variable, x);

                double y = left.getValue() - right.getValue();
                return checkYLogScale.isChecked() ? log1pScale(y) : y;
            }
        }, prefPlot);

        plot.addSeries(mainSeries, new LineAndPointFormatter(Color.rgb(50, 0, 0), null, null, null));
        updatePlotBound();
    }

    private void submitSelectRange() {
        Intent resultIntent = new Intent();
        double rminX = checkXLogScale.isChecked() ? logScaleRecover(minX) : minX;
        double rmaxX = checkXLogScale.isChecked() ? logScaleRecover(maxX) : maxX;
        resultIntent.putExtra("LOWER_BOUND", rminX);
        resultIntent.putExtra("UPPER_BOUND", rmaxX);
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    private void updatePlotBound() {
        double rminX = checkXLogScale.isChecked() ? logScaleRecover(minX) : minX;
        double rmaxX = checkXLogScale.isChecked() ? logScaleRecover(maxX) : maxX;

        textLowerBound.setText(String.format(getString(R.string.format_bound), rminX));
        textUpperBound.setText(String.format(getString(R.string.format_bound), rmaxX));

        mainSeries.setBound(minX, maxX);
        plot.setDomainBoundaries(minX, maxX, BoundaryMode.FIXED);

        double minY = mainSeries.isAllInvalid() ? 1.0/SCALE_YAXIS : mainSeries.getMinY();
        double maxY = mainSeries.isAllInvalid() ? 1.0/SCALE_YAXIS : mainSeries.getMaxY();
        double scale = SCALE_YAXIS * Math.max(Math.abs(minY), Math.abs(maxY));

        nZero = mainSeries.getNZero();
        plot.setRangeBoundaries(-scale, scale, BoundaryMode.FIXED);

        if (mainSeries.isAllInvalid()) {
            int color = ContextCompat.getColor(plot.getContext(), R.color.colorProhibition);
            plot.getGraphWidget().getGridBackgroundPaint().setColor(color);
            textLargeWarning.setVisibility(View.VISIBLE);
            textLargeWarning.setText("No valid value in this range.");
        } else {
            int color_id = nZero > 0 ? R.color.colorPermission : R.color.colorProhibition;
            int color = ContextCompat.getColor(plot.getContext(), color_id);
            plot.getGraphWidget().getGridBackgroundPaint().setColor(color);
            textLargeWarning.setVisibility(View.INVISIBLE);
        }

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
                    zoom(oldDist / distBetweenFingers);
                }

                updatePlotBound();
                break;
        }
        return true;
    }

    private void zoom(double scale) {
        double domainSpan = maxX - minX;
        double domainMidPoint = maxX - domainSpan / 2.0f;
        double offset = domainSpan * scale / 2.0f;

        double min = domainMidPoint - offset;
        double max = domainMidPoint + offset;

        min = Math.min(min, mainSeries.getX(mainSeries.size() - 1).doubleValue());
        max = Math.max(max, mainSeries.getX(0).doubleValue());

        double rminX = checkXLogScale.isChecked() ? logScaleRecover(min) : min;
        double rmaxX = checkXLogScale.isChecked() ? logScaleRecover(max) : max;
        if (min < max && !Double.isInfinite(rminX) && !Double.isNaN(rminX)
                && !Double.isInfinite(rmaxX) && !Double.isNaN(rmaxX)) {
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

        double rminX = checkXLogScale.isChecked() ? logScaleRecover(min) : min;
        double rmaxX = checkXLogScale.isChecked() ? logScaleRecover(max) : max;
        if (min < max && !Double.isInfinite(rminX) && !Double.isNaN(rminX)
                && !Double.isInfinite(rmaxX) && !Double.isNaN(rmaxX)) {
            minX = min;
            maxX = max;
        }
    }

    private double spacing(MotionEvent event) {
        double x = event.getX(0) - event.getX(1);
        double y = event.getY(0) - event.getY(1);
        return Math.hypot(x, y);
    }

    // log1p and log scale method
    private static double log1pScale(double val) {
        return Math.signum(val) * Math.log1p(Math.abs(val));
    }

    private static double log1pScaleRecover(double val) {
        return Math.signum(val) * Math.expm1(Math.abs(val));
    }

    private static double logScale(double val) {
        return Math.log(val);
    }

    private static double logScaleRecover(double val) {
        return Math.exp(val);
    }
}
