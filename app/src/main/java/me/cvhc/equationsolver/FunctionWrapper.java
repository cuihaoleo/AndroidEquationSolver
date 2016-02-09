package me.cvhc.equationsolver;

import com.androidplot.xy.XYSeries;

public class FunctionWrapper implements XYSeries {
    public interface MathFunction {
        double call(double x);
    }

    MathFunction function;
    int seriesSize;
    double upperBound = 0, lowerBound = 0;
    double step = 0;

    public FunctionWrapper(MathFunction f, int size) {
        function = f;
        seriesSize = size;
    }

    public void setBound(Double l, Double u) {
        if (l != null) { lowerBound = l; }
        if (u != null) { upperBound = u; }
        step = (upperBound - lowerBound) / seriesSize;
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
        return lowerBound + step * index;
    }

    @Override
    public Number getY(int index) {
        return function.call(getX(index).doubleValue());
    }

    @Override
    public String getTitle() {
        return "What title?";
    }
}
