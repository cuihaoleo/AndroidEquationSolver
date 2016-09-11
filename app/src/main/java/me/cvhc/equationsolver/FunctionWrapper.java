package me.cvhc.equationsolver;

import android.util.Pair;

import com.androidplot.xy.XYSeries;

import java.util.LinkedList;
import java.util.ListIterator;


/**
 * FunctionWrapper wraps an unary math function (MathFunction class) to an
 * XYSeries object, so the function can be plotted by AndroidPlot library.
 *
 * The series is sampled in the interval [lowerBound, upperBound]. To support
 * dynamic change of plot range, call setBound(l, u) to edit the interval and
 * re-sample the series.
 */
public class FunctionWrapper implements XYSeries {

    // A command pattern interface to wrap an unary math function
    public interface MathFunction {
        double call(double x);
    }

    private MathFunction function;
    private int seriesSize;
    private double upperBound = 0, lowerBound = 0;
    private boolean allInvalid = true;
    private int nZero = 0;
    private double minY, maxY;

    private Pair<Double,Double>[] series;
    private LinkedList<Pair<Double,Double>> seriesCache = new LinkedList<>();

    public FunctionWrapper(MathFunction f, int size) {
        function = f;
        seriesSize = size;
        seriesCache.addFirst(new Pair<Double,Double>(Double.NEGATIVE_INFINITY, null));
        seriesCache.addLast(new Pair<Double, Double>(Double.POSITIVE_INFINITY, null));
    }

    /**
     * Set the bound of sampling and (re-)sample the series. To save time, old
     * sampling results will be reused as many as possible. So we don't require
     * to sample uniformly, but one sample in a uniformly partitioned interval.
     *
     * @param l Lower bound of sampling
     * @param u Upper bound of sampling
     */
    public void setBound(Double l, Double u) {
        if (l != null) { lowerBound = l; }
        if (u != null) { upperBound = u; }
        double step = Math.max((upperBound - lowerBound) / seriesSize, Math.ulp(0));

        ListIterator<Pair<Double,Double>> iter = seriesCache.listIterator();
        Double x = lowerBound;

        series = new Pair[seriesSize];
        allInvalid = true;
        nZero = 0;
        minY = Double.POSITIVE_INFINITY;
        maxY = Double.NEGATIVE_INFINITY;

        for (int i=0; i<seriesSize; i++, x+= step) {
            double lower = x - step /2.0;
            double upper = x + step /2.0;
            Pair<Double, Double> cur;

            while (iter.hasNext()) {
                cur = iter.next();
                if (cur.first <= lower) {
                    iter.remove();
                } else if (cur.first < upper) {
                    series[i] = cur;
                    break;
                } else {
                    Pair<Double, Double> p = new Pair<>(x, function.call(x));
                    iter.previous();
                    iter.add(series[i] = p);
                    break;
                }
            }

            double cur_y = series[i].second;
            if (!(Double.isNaN(cur_y) || Double.isInfinite(cur_y))) {
                if (i > 0) {
                    double pre_y = series[i - 1].second;
                    if (cur_y == 0 || cur_y * pre_y < 0.0) {
                        nZero++;
                    }
                }

                if (cur_y > maxY) { maxY = cur_y; }
                if (cur_y < minY) { minY = cur_y; }
                if (allInvalid) { allInvalid = false; }
            }
        }

        seriesCache.addFirst(new Pair<Double, Double>(Double.NEGATIVE_INFINITY, null));
    }

    public void resetCache() {
        seriesCache.clear();
        seriesCache.addFirst(new Pair<Double,Double>(Double.NEGATIVE_INFINITY, null));
        seriesCache.addLast(new Pair<Double, Double>(Double.POSITIVE_INFINITY, null));
    }

    public double getLowerBound() {
        return lowerBound;
    }

    public double getUpperBound() {
        return upperBound;
    }

    public double getMinY() {
        return minY;
    }

    public double getMaxY() {
        return maxY;
    }

    public int getNZero() {
        return nZero;
    }

    public boolean isAllInvalid() {
        return allInvalid;
    }

    @Override
    public int size() {
        return seriesSize;
    }

    @Override
    public Number getX(int index) {
        return series[index].first;
    }

    @Override
    public Number getY(int index) {
        Double d = series[index].second;
        if (Double.isNaN(d) || Double.isInfinite(d)) {
            return allInvalid ? 0 : null;
        } else {
            return series[index].second;
        }
    }

    @Override
    public String getTitle() {
        return "";
    }
}