package me.cvhc.equationsolver;

import android.util.Pair;

import com.androidplot.xy.XYSeries;

import java.util.LinkedList;
import java.util.ListIterator;


public class FunctionWrapper implements XYSeries {
    public interface MathFunction {
        double call(double x);
    }

    private MathFunction function;
    private int seriesSize;
    private double upperBound = 0, lowerBound = 0;
    private double step = 0;
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

    public void setBound(Double l, Double u) {
        if (l != null) { lowerBound = l; }
        if (u != null) { upperBound = u; }
        step = (upperBound - lowerBound) / seriesSize;

        ListIterator<Pair<Double,Double>> iter = seriesCache.listIterator();
        Double x = lowerBound;

        series = new Pair[seriesSize];
        allInvalid = true;
        nZero = 0;
        minY = Double.POSITIVE_INFINITY;
        maxY = Double.NEGATIVE_INFINITY;

        for (int i=0; i<seriesSize; i++, x+=step) {
            double lower = x - step/2.0;
            double upper = x + step/2.0;
            Pair<Double, Double> cur = null;

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
                    iter.add(cur = series[i] = p);
                    break;
                }
            }

            double cur_y = series[i].second;
            if (!(Double.isNaN(cur_y) || Double.isInfinite(cur_y))) {
                if (i > 0) {
                    double pre_y = series[i - 1].second;
                    if (cur_y == 0 || (cur_y * pre_y < 0.0 && Math.abs(cur_y) < 1.0)) {
                        nZero++;
                    }
                }

                if (cur_y > maxY) { maxY = cur_y; };
                if (cur_y < minY) { minY = cur_y; };
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
        //return lowerBound + step * index;
        return series[index].first;
    }

    @Override
    public Number getY(int index) {
        //return function.call(getX(index).doubleValue());
        Double d = series[index].second;
        if (Double.isNaN(d) || Double.isInfinite(d)) {
            return allInvalid ? 0 : null;
        } else {
            return series[index].second;
        }
    }

    @Override
    public String getTitle() {
        return "What title?";
    }
}
