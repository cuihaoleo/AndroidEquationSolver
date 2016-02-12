package me.cvhc.equationsolver;

import android.util.Pair;

import com.androidplot.xy.XYSeries;

import java.util.LinkedList;
import java.util.ListIterator;


public class FunctionWrapper implements XYSeries {
    public interface MathFunction {
        double call(double x);
    }

    MathFunction function;
    int seriesSize;
    double upperBound = 0, lowerBound = 0;
    double step = 0;
    boolean allInvalid = true;

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

            if (allInvalid && !(Double.isNaN(cur.second) || Double.isInfinite(cur.second))) {
                allInvalid = false;
            }
        }

        seriesCache.addFirst(new Pair<Double,Double>(Double.NEGATIVE_INFINITY, null));
    }

    public double getLowerBound() {
        return lowerBound;
    }

    public double getUpperBound() {
        return upperBound;
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
