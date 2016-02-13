package me.cvhc.equationsolver;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.PointF;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

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
    private Button buttonApply, buttonCancel;
    private XYPlot plot;
    private CheckBox checkXLogScale, checkYLogScale;

    private FunctionWrapper mainSeries = null;
    private double minX;
    private double maxX;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plot);

        Intent intent = getIntent();

        double lowerBound = intent.getDoubleExtra("LOWER_BOUND", -1.0);
        double upperBound = intent.getDoubleExtra("UPPER_BOUND", 1.0);
        String leftPart = intent.getStringExtra("LEFT_PART");
        String rightPart = intent.getStringExtra("RIGHT_PART");
        final ExpressionEvaluator left = new ExpressionEvaluator(leftPart);
        final ExpressionEvaluator right = new ExpressionEvaluator(rightPart);
        final Character variable = intent.getCharExtra("VARIABLE", 'x');

        HashMap<Character, Double> constValues = (HashMap<Character, Double>)intent.getSerializableExtra("CONSTANT_VALUES");
        left.updateVariables(constValues);
        right.updateVariables(constValues);

        textLowerBound = (TextView)findViewById(R.id.textLowerBound);
        textUpperBound = (TextView)findViewById(R.id.textUpperBound);
        buttonApply = (Button)findViewById(R.id.buttonApply);
        buttonCancel = (Button)findViewById(R.id.buttonCancel);
        checkXLogScale = (CheckBox)findViewById(R.id.checkXLogScale);
        checkYLogScale = (CheckBox)findViewById(R.id.checkYLogScale);

        buttonApply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("LOWER_BOUND", minX);
                resultIntent.putExtra("UPPER_BOUND", maxX);
                setResult(RESULT_OK, resultIntent);
                finish();
            }
        });

        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent resultIntent = new Intent();
                setResult(RESULT_CANCELED, resultIntent);
                finish();
            }
        });

        abstract class CustomFormat extends NumberFormat {
            @Override
            abstract public StringBuffer format(double v, StringBuffer b, FieldPosition f);
            @Override
            public StringBuffer format(long v, StringBuffer b, FieldPosition f) { return null; }
            @Override
            public Number parse(String s, ParsePosition p) { return null; }
        };

        checkXLogScale.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mainSeries.resetCache();
                if (isChecked) {
                    minX = logScale(minX);
                    maxX = logScale(maxX);
                } else {
                    minX = logScaleRecover(minX);
                    maxX = logScaleRecover(maxX);
                }
                mainSeries.setBound(minX, maxX);
                plot.setDomainBoundaries(minX, maxX, BoundaryMode.FIXED);
                plot.redraw();
            }
        });

        checkYLogScale.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mainSeries.resetCache();
                mainSeries.setBound(minX, maxX);
                plot.redraw();
            }
        });

        plot = (XYPlot)findViewById(R.id.plot);
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
                double output = checkYLogScale.isChecked() ? logScaleRecover(value) : value;
                return new StringBuffer(String.format("%6.4g", output));
            }
        });

        plot.getLegendWidget().setVisible(false);
        plot.getTitleWidget().setVisible(false);

        mainSeries = new FunctionWrapper(new FunctionWrapper.MathFunction() {
            @Override
            public double call(double x) {
                if (checkXLogScale.isChecked()) { x = logScaleRecover(x); }

                left.setVariable(variable, x);
                right.setVariable(variable, x);

                double y = left.getValue() - right.getValue();
                return checkYLogScale.isChecked() ? logScale(y) : y;
            }
        }, 500);
        mainSeries.setBound(lowerBound, upperBound);

        plot.addSeries(mainSeries, new LineAndPointFormatter(Color.rgb(50, 0, 0), null, null, null));
        plot.calculateMinMaxVals();
        plot.centerOnRangeOrigin(0.0);

        minX = plot.getCalculatedMinX().doubleValue();
        maxX = plot.getCalculatedMaxX().doubleValue();

        textLowerBound.setText(String.format(getString(R.string.format_bound), minX));
        textUpperBound.setText(String.format(getString(R.string.format_bound), maxX));

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

                textLowerBound.setText(String.format(getString(R.string.format_bound), minX));
                textUpperBound.setText(String.format(getString(R.string.format_bound), maxX));

                plot.setDomainBoundaries(minX, maxX, BoundaryMode.FIXED);
                plot.redraw();
                break;
        }
        return true;
    }

    private void zoom(double scale) {
        double domainSpan = maxX - minX;
        double domainMidPoint = maxX - domainSpan / 2.0f;
        double offset = domainSpan * scale / 2.0f;

        minX = domainMidPoint - offset;
        maxX = domainMidPoint + offset;

        minX = Math.min(minX, mainSeries.getX(mainSeries.size() - 1).doubleValue());
        maxX = Math.max(maxX, mainSeries.getX(0).doubleValue());
        mainSeries.setBound(minX, maxX);
    }

    private void scroll(double pan) {
        double domainSpan = maxX - minX;
        double step = domainSpan / plot.getWidth();
        double offset = pan * step;

        minX = minX + offset;
        maxX = maxX + offset;
        mainSeries.setBound(minX, maxX);
    }

    private double spacing(MotionEvent event) {
        double x = event.getX(0) - event.getX(1);
        double y = event.getY(0) - event.getY(1);
        return Math.hypot(x, y);
    }

    private static double logScale(double val) {
        return Math.signum(val) * Math.log10(Math.abs(val)+1.0);
    }

    private static double logScaleRecover(double val) {
        return Math.signum(val) * Math.pow(10, val-1.0);
    }
}
