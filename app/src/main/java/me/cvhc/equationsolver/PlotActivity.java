package me.cvhc.equationsolver;

import android.graphics.Color;
import android.graphics.PointF;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.view.View.OnTouchListener;

import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.XYPlot;


public class PlotActivity extends AppCompatActivity implements OnTouchListener {

    private static final int SERIES_SIZE = 200;
    private XYPlot plot;
    private Button resetButton;
    private Evaluator2SeriesWrapper mainSeries = null;
    private PointF minXY;
    private PointF maxXY;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plot);

        plot = (XYPlot) findViewById(R.id.plot);
        plot.setOnTouchListener(this);

        plot.getGraphWidget().setTicksPerRangeLabel(2);
        plot.getGraphWidget().setTicksPerDomainLabel(2);

        plot.getLegendWidget().setVisible(false);
        plot.getTitleWidget().setVisible(false);

        mainSeries = new Evaluator2SeriesWrapper(new Evaluator2SeriesWrapper.MathFunction() {
            @Override
            public double call(double x) {
                return Math.sin(x);
            }
        }, 500);
        mainSeries.setBound(0.0, 8 * Math.PI);

        plot.addSeries(mainSeries, new LineAndPointFormatter(Color.rgb(50, 0, 0), null, null, null));
        plot.calculateMinMaxVals();
        minX = plot.getCalculatedMinX().doubleValue();
        maxX = plot.getCalculatedMaxX().doubleValue();

        plot.setUserRangeOrigin(0);

        plot.redraw();

    }

    private double minX;
    private double maxX;

    static final int NONE = 0;
    static final int ONE_FINGER_DRAG = 1;
    static final int TWO_FINGERS_DRAG = 2;
    int mode = NONE;

    PointF firstFinger;
    double distBetweenFingers;

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
        clampToDomainBounds(domainSpan);
    }

    private void scroll(double pan) {
        double domainSpan = maxX - minX;
        double step = domainSpan / plot.getWidth();
        double offset = pan * step;

        minX = minX + offset;
        maxX = maxX + offset;

        clampToDomainBounds(domainSpan);
    }

    private void clampToDomainBounds(double domainSpan) {
        double leftBoundary = mainSeries.getX(0).doubleValue();
        double rightBoundary = mainSeries.getX(mainSeries.size() - 1).doubleValue();
        Double newLower = null, newUpper = null;

        if (minX < leftBoundary) { newLower = (double)minX; }
        if (maxX > rightBoundary) { newUpper = (double)maxX; }

        mainSeries.setBound(minX, maxX);
    }

    private double spacing(MotionEvent event) {
        double x = event.getX(0) - event.getX(1);
        double y = event.getY(0) - event.getY(1);
        return Math.hypot(x, y);
    }
}
