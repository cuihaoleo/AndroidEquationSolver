package me.cvhc.equationsolver;

import android.content.Context;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;
import com.androidplot.xy.XYSeriesFormatter;


public class ExtendedXYPlotZoomPan extends XYPlot implements OnTouchListener {
    public ExtendedXYPlotZoomPan(Context context, String title) {
        super(context, title);
        setOnTouchListener(this);
    }

    public ExtendedXYPlotZoomPan(Context context, String title, RenderMode mode) {
        super(context, title, mode);
        setOnTouchListener(this);
    }

    public ExtendedXYPlotZoomPan(Context context, AttributeSet attributes) {
        super(context, attributes);
        setOnTouchListener(this);
    }

    public ExtendedXYPlotZoomPan(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setOnTouchListener(this);
    }

    private double minX;
    private double maxX;

    static final int NONE = 0;
    static final int ONE_FINGER_DRAG = 1;
    static final int TWO_FINGERS_DRAG = 2;
    Evaluator2SeriesWrapper mainSeries = null;

    int mode = NONE;

    PointF firstFinger;
    double distBetweenFingers;
    boolean stopThread = false;

    public synchronized boolean addSeries(Evaluator2SeriesWrapper series, XYSeriesFormatter formatter) {
        boolean retVal;

        mainSeries = series;
        retVal = super.addSeries(mainSeries, formatter);

        minX = mainSeries.getLowerBound();
        maxX = mainSeries.getUpperBound();

        calculateMinMaxVals();
        minX = getCalculatedMinX().doubleValue();
        maxX = getCalculatedMaxX().doubleValue();

        return retVal;

    }

    @Override
    public boolean onTouch(View arg0, MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN: // Start gesture
                firstFinger = new PointF(event.getX(), event.getY());
                mode = ONE_FINGER_DRAG;
                stopThread = true;
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
                    setDomainBoundaries(minX, maxX, BoundaryMode.FIXED);
                    redraw();

                } else if (mode == TWO_FINGERS_DRAG) {
                    double oldDist = distBetweenFingers;
                    distBetweenFingers = spacing(event);
                    zoom(oldDist / distBetweenFingers);
                    setDomainBoundaries(minX, maxX, BoundaryMode.FIXED);
                    redraw();
                }
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
        double step = domainSpan / getWidth();
        double offset = pan * step;

        minX = minX + offset;
        maxX = maxX + offset;

        clampToDomainBounds(domainSpan);
    }

    private void clampToDomainBounds(double domainSpan) {
        double leftBoundary = mainSeries.getX(0).doubleValue();
        double rightBoundary = mainSeries.getX(mainSeries.size() - 1).doubleValue();
        Double newLower = null, newUpper = null;

        // enforce left scroll boundary:
        if (minX < leftBoundary) {
            newLower = (double)minX;
        }
        if (maxX > rightBoundary) {
            newUpper = (double)maxX;
        }

        mainSeries.setBound(minX, maxX);
    }

    private double spacing(MotionEvent event) {
        double x = event.getX(0) - event.getX(1);
        double y = event.getY(0) - event.getY(1);
        return Math.hypot(x, y);
    }
}