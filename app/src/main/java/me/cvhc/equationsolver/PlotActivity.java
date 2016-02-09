package me.cvhc.equationsolver;

import android.graphics.Color;
import android.graphics.PointF;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.XYPlotZoomPan;


public class PlotActivity extends AppCompatActivity {

    private static final int SERIES_SIZE = 200;
    private ExtendedXYPlotZoomPan plot;
    private Button resetButton;
    private Evaluator2SeriesWrapper series = null;
    private PointF minXY;
    private PointF maxXY;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plot);
        resetButton = (Button) findViewById(R.id.resetButton);
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //nothing
            }
        });
        plot = (ExtendedXYPlotZoomPan) findViewById(R.id.plot);
        //mySimpleXYPlot.setOnTouchListener(this);
        plot.getGraphWidget().setTicksPerRangeLabel(2);
        plot.getGraphWidget().setTicksPerDomainLabel(2);
        plot.getGraphWidget().getBackgroundPaint().setColor(Color.TRANSPARENT);

        //plot.setDomainBoundaries(0, 10, BoundaryMode.GROW);
        //plot.getGraphWidget().setRangeValueFormat(
        //        new DecimalFormat("#####"));
        //plot.getGraphWidget().setDomainValueFormat(
        //        new DecimalFormat("#####.#"));
        plot.getGraphWidget().setRangeTickLabelWidth(80);
        plot.getGraphWidget().setDomainTickLabelWidth(40);
        //plot.setRangeLabel("");
        //plot.setDomainLabel("");

        //plot.setBorderStyle(Plot.BorderStyle.NONE, null, null);
        plot.getLegendWidget().setVisible(false);
        plot.getTitleWidget().setVisible(false);

        series = new Evaluator2SeriesWrapper(new Evaluator2SeriesWrapper.MathFunction() {
            @Override
            public double call(double x) {
                return Math.sin(x);
            }
        }, 500);
        series.setBound(0.0, 8 * Math.PI);

        plot.addSeries(series, new LineAndPointFormatter(Color.rgb(50, 0, 0), null, null, null));
        plot.redraw();

    }
}
