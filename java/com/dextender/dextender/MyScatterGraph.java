package com.dextender.dextender;

import android.content.Context;
import android.graphics.Color;
import android.util.DisplayMetrics;
import android.util.TypedValue;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.LineChart;
import org.achartengine.chart.PointStyle;
import org.achartengine.chart.ScatterChart;
import org.achartengine.model.TimeSeries;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

//======================================================================================
// Class : MyScatterGraph
// Author: NOT Mike LiVolsi - Various authors including publisher of the class
// Purpose: Graph the BG data that we have stored in our local database.
//          This information is passed as an LONG and INT array for the X/Y coordinates
//
//======================================================================================
public class MyScatterGraph {
    public GraphicalView getView(Context context, int inRecordCount, int inHoursBack, long[] argX, int[] argY, int[] averageY, int argWarnHigh,  int argWarnLow,
                                 boolean argSmartFlag,
                                 boolean argAverageFlag,
                                 int argSmartHigh) {

        DisplayMetrics metrics = context.getResources().getDisplayMetrics();


        float val14 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14, metrics);


        //-------------------------------------------------------------------
        // Values coming in from the database that's being passed as an arg
        //-------------------------------------------------------------------
        MyTools tools = new MyTools();

        XYSeries   BGseries    = new TimeSeries("Blood Glucose Values\n");                          // Scatter plot these guys
        XYSeries   lowBGvals   = new TimeSeries("Low");
        XYSeries   highBGvals  = new TimeSeries("High");
        TimeSeries avgSeries   = new TimeSeries("30 Minute moving Average");                        // our calculate moving average
        TimeSeries lowSeries   = new TimeSeries("\nLow Range\n");                                   // Low will be a timeSeries
        TimeSeries highSeries  = new TimeSeries("High Range\n");                                    // high will be a timeseries
        TimeSeries smartSeries = new TimeSeries("Elevated Range\n");



        for (int i = 0; i < inRecordCount; i++) {                                                   // we now add to the respective series
            BGseries.add(argX[i], argY[i]);
            //if((averageY[i] > 0) && argAverageFlag) {
            //    avgSeries.add(argX[i], averageY[i]);
            //}
            //-------------------------------------------------
            // Overwrite white dots with yellow and red
            //-------------------------------------------------
            if(argY[i] >= argWarnHigh) {
                highBGvals.add(argX[i], argY[i]);
            }
            if(argY[i] <= argWarnLow ) {
                lowBGvals.add(argX[i], argY[i]);
            }
            if(argAverageFlag) {
                avgSeries.add(argX[i], averageY[i]);
            }

        }

        if(inRecordCount > 0) {

            if(argWarnHigh <= 400) {
                highSeries.add(System.currentTimeMillis() / 1000 - (60 * 60 * inHoursBack), argWarnHigh);
                highSeries.add(System.currentTimeMillis() / 1000, argWarnHigh);
            }
            if(argWarnLow > 0) {
                lowSeries.add(System.currentTimeMillis() / 1000 - (60 * 60 * inHoursBack), argWarnLow);
                lowSeries.add(System.currentTimeMillis() / 1000, argWarnLow);
            }
            smartSeries.add(System.currentTimeMillis()/1000 - (60*60*inHoursBack), argSmartHigh);
            smartSeries.add(System.currentTimeMillis()/1000, argSmartHigh);
        }

        if (inRecordCount >= 1) {
            XYMultipleSeriesDataset bgDataSet = new XYMultipleSeriesDataset();                      // this guy acts like a collector of all the series
            bgDataSet.addSeries(BGseries);                                                          // so add the series above to the collector;
            bgDataSet.addSeries(lowBGvals);
            bgDataSet.addSeries(highBGvals);
            if (argAverageFlag) {
                bgDataSet.addSeries(avgSeries);
            }
            bgDataSet.addSeries(lowSeries);
            bgDataSet.addSeries(highSeries);
            if (argSmartFlag) {
                bgDataSet.addSeries(smartSeries);
            }

            // !!!!! REMEMBER - SERIES MUST BE RENDERED IN THE SAME ORDER
            // !!!!! THAT THEY ARE ADDED ABOVE !!!!
            //-------------------------------------------------------
            // Series 1 renderer - the white scatter graph points
            //-------------------------------------------------------
            XYSeriesRenderer renderer1 = new XYSeriesRenderer();                                    // This gives a line it's properties
            renderer1.setColor(Color.WHITE);
            renderer1.setPointStyle(PointStyle.CIRCLE);
            renderer1.setFillPoints(true);
            renderer1.setChartValuesTextSize(8);

            //-------------------------------------------------------
            // Series 1L renderer - the white scatter graph points
            //-------------------------------------------------------
            XYSeriesRenderer renderer1l = new XYSeriesRenderer();                                   // This gives a line it's properties
            renderer1l.setColor(Color.YELLOW);
            renderer1l.setPointStyle(PointStyle.CIRCLE);
            renderer1l.setFillPoints(true);
            renderer1l.setChartValuesTextSize(8);

            //-------------------------------------------------------
            // Series 1H renderer - the white scatter graph points
            //-------------------------------------------------------
            XYSeriesRenderer renderer1h = new XYSeriesRenderer();                                   // This gives a line it's properties
            renderer1h.setColor(Color.RED);
            renderer1h.setPointStyle(PointStyle.CIRCLE);
            renderer1h.setFillPoints(true);
            renderer1h.setChartValuesTextSize(8);

            //----------------------------------------------
            // Series 2 renderer - Green line for average
            //-----------------------------------------------
            XYSeriesRenderer renderer2 = new XYSeriesRenderer();                                    // This gives a line it's properties
            renderer2.setColor(Color.GREEN);
            renderer2.setLineWidth(4);
            renderer2.setChartValuesTextSize(8);

            //----------------------------------------------
            // Series 3 renderer - Red line
            //-----------------------------------------------
            XYSeriesRenderer renderer3 = new XYSeriesRenderer();                                    // This gives a line it's properties
            renderer3.setColor(Color.RED);
            renderer3.setLineWidth(2);
            renderer3.setChartValuesTextSize(8);

            //----------------------------------------------
            // Series 4 renderer
            //-----------------------------------------------
            XYSeriesRenderer renderer4 = new XYSeriesRenderer();                                    // This gives a line it's properties
            renderer4.setColor(Color.YELLOW);
            renderer4.setLineWidth(2);
            renderer4.setChartValuesTextSize(8);

            //----------------------------------------------
            // Series 5 renderer - Smart series - blue line
            //-----------------------------------------------
            XYSeriesRenderer renderer5 = new XYSeriesRenderer();
            renderer5.setColor(Color.CYAN);
            renderer5.setLineWidth(2);
            renderer5.setChartValuesTextSize(8);



            XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer();
            mRenderer.setXAxisMin((System.currentTimeMillis() / 1000) - (60 * 60 * inHoursBack));   // min time is 12 hours ago
            mRenderer.setXAxisMax(System.currentTimeMillis() / 1000);                               // min time is 12 hours ago
            mRenderer.setYAxisMin(0);
            mRenderer.setYAxisMax(400);
            mRenderer.setLegendTextSize(val14);
            mRenderer.setShowLegend(false);                                                         // turn off legend
            mRenderer.setShowLabels(true);
            mRenderer.setAxisTitleTextSize(20);
            mRenderer.setChartTitleTextSize(20);

            mRenderer.setApplyBackgroundColor(true);
            mRenderer.setBackgroundColor(Color.BLACK);                                        // Was TRANSPARENT / "CLEAR"
            mRenderer.setMarginsColor(Color.BLACK);                                                 // Was TRANSPARENT
            mRenderer.setMarginsColor(Color.argb(0x00, 0x01, 0x01, 0x01));
            mRenderer.setYTitle("Bg", 0);
            mRenderer.setZoomButtonsVisible(false);
            mRenderer.setZoomEnabled(false,false);
            mRenderer.setPanEnabled(false, true);
            mRenderer.setLabelsTextSize(18);
            mRenderer.setXLabels(0);

            int modulator=12;                            // after how many readings do we post the time
            switch (inHoursBack) {
                case 3:  modulator=6;
                         mRenderer.setPointSize(6.0f);
                         break;
                case 6:  modulator=12;
                         mRenderer.setPointSize(5.0f);
                         break;
                case 12: modulator=24;
                         mRenderer.setPointSize(4.0f);
                         break;
                case 24: modulator=48;
                         mRenderer.setPointSize(3.0f);
                         break;
            }
            for (int i = 0; i < inRecordCount; i++) {
                if(tools.modulo(i,modulator) == 0) mRenderer.addXTextLabel(argX[i],tools.epoch2FmtTime(argX[i],"h:mm a"));
            }
            mRenderer.addXTextLabel(argX[(inRecordCount - 1)], tools.epoch2FmtTime(argX[(inRecordCount - 1)], "h:mm a")); // last one put time
                                                                            // only show 4 labels in the X


            mRenderer.addSeriesRenderer(renderer1);                                                      // Now add the rendered series to our collector
            mRenderer.addSeriesRenderer(renderer1h);
            mRenderer.addSeriesRenderer(renderer1l);
            if(argAverageFlag) mRenderer.addSeriesRenderer(renderer2);
            mRenderer.addSeriesRenderer(renderer3);
            mRenderer.addSeriesRenderer(renderer4);
            if(argSmartFlag)   mRenderer.addSeriesRenderer(renderer5);


            //==========================================================================
            // Do we show the turquoise line ?
            //==========================================================================
            if (argSmartFlag) {
                if(argAverageFlag) {
                    String[] types = new String[]{ScatterChart.TYPE, ScatterChart.TYPE, ScatterChart.TYPE,
                            LineChart.TYPE, LineChart.TYPE, LineChart.TYPE, LineChart.TYPE};
                    return ChartFactory.getCombinedXYChartView(context, bgDataSet, mRenderer, types);
                }
                else {
                    String[] types = new String[]{ScatterChart.TYPE, ScatterChart.TYPE, ScatterChart.TYPE,
                             LineChart.TYPE, LineChart.TYPE, LineChart.TYPE};
                    return ChartFactory.getCombinedXYChartView(context, bgDataSet, mRenderer, types);
                }
            }
            else {
                if(argAverageFlag) {
                    String[] types = new String[]{ScatterChart.TYPE, ScatterChart.TYPE, ScatterChart.TYPE,
                            LineChart.TYPE, LineChart.TYPE, LineChart.TYPE};
                    return ChartFactory.getCombinedXYChartView(context, bgDataSet, mRenderer, types);
                }
                else {
                    String[] types = new String[]{ScatterChart.TYPE, ScatterChart.TYPE, ScatterChart.TYPE, LineChart.TYPE, LineChart.TYPE};
                    return ChartFactory.getCombinedXYChartView(context, bgDataSet, mRenderer, types);
                }
            }
        }
        else {
            return null;
        }
    }
}
