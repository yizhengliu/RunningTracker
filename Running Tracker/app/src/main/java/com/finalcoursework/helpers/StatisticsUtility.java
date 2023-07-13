package com.finalcoursework.helpers;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;

import androidx.lifecycle.LifecycleOwner;

import com.finalcoursework.R;
import com.finalcoursework.dataBase.Record;
import com.finalcoursework.databinding.ActivityStatisticsBinding;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;
//handler class for statistics activity
public class StatisticsUtility {
    //initialize the setting of the line chart
    public void initiateBarChart(ActivityStatisticsBinding binding) {
        binding.myChart.getXAxis().setPosition(XAxis.XAxisPosition.TOP_INSIDE);
        binding.myChart.getXAxis().setDrawLabels(false);
        binding.myChart.getXAxis().setAxisLineColor(Color.BLACK);
        binding.myChart.getXAxis().setDrawGridLines(false);
        binding.myChart.getAxisLeft().setAxisLineColor(Color.BLACK);
        binding.myChart.getAxisLeft().setTextColor(Color.BLACK);
        binding.myChart.getAxisLeft().setDrawGridLines(false);
        binding.myChart.getAxisRight().setAxisLineColor(Color.BLACK);
        binding.myChart.getAxisRight().setTextColor(Color.BLACK);
        binding.myChart.getAxisRight().setDrawGridLines(false);
        binding.myChart.getDescription().setText("old <----     Records History     ----> new");
        binding.myChart.getDescription().setTextAlign(Paint.Align.RIGHT);
        binding.myChart.getDescription().setTextSize(20f);
        binding.myChart.getLegend().setEnabled(true);
    }
    //observe the livedata in the view model, when the query from database is done, we use those
    //data to update the view
    public void observeData(ActivityStatisticsBinding binding, Context context, LifecycleOwner owner) {
        //update the overall data
        binding.getViewModel().totalTimeSpent.observe(owner, (timeSpent) -> {
            if (timeSpent != null) {
                long temp = timeSpent;
                long hour = TimeUnit.MILLISECONDS.toHours(temp);
                temp -= TimeUnit.HOURS.toMillis(hour);
                long minute = TimeUnit.MILLISECONDS.toMinutes(temp);
                temp -= TimeUnit.MINUTES.toMillis(minute);
                long second = TimeUnit.MILLISECONDS.toSeconds(temp);
                binding.timeDisplay.setText(context.getString(R.string.totalTimeDisplay, hour, minute, second));
            }
        });
        binding.getViewModel().totalDistance.observe(owner, totalDistance -> {
            if (totalDistance != null)
                binding.distanceDisplay.setText(context.getString(R.string.totalDistanceDisplay, totalDistance / 1000f, ""));
        });
        binding.getViewModel().overallAverageSpeed.observe(owner, averageSpeed -> {
            if (averageSpeed != null)
                binding.averageSpeedDisplay.setText(context.getString(R.string.overallAverageSpeed, averageSpeed, ""));
        });
        //update today's data
        binding.getViewModel().records.observe(owner, records -> {
            //if there are some record
            if (records != null && !records.isEmpty()) {
                //initialize the line chart
                //the line chart is from an android library, and the link is below.
                //https://github.com/PhilJay/MPAndroidChart
                //the only usage of the graph is here in the statistic activity.
                //however the settings of the graph still needs to be done manually. (
                // by connecting the live data from the view model)
                ArrayList<ILineDataSet> dataSets = new ArrayList<>();
                dataSets.add(getTimeSpent(records));
                dataSets.add(getDistance(records));
                dataSets.add(getAverageSpeed(records));
                binding.myChart.setData(new LineData(dataSets));
                binding.myChart.setDrawMarkers(true);
                binding.myChart.setMarker(new ModifiedPopView(reverse(records), context, R.layout.pop_view));
                binding.myChart.invalidate();
                Calendar todayCalendar = Calendar.getInstance();
                Calendar lastRecord = Calendar.getInstance();
                todayCalendar.setTimeInMillis(System.currentTimeMillis());
                lastRecord.setTimeInMillis(records.get(0).getDate());
                //if there are many records and the newest one is today's record then update the views
                //based on the best, last one before today's record
                if (records.size() > 1 &&
                        (todayCalendar.get(Calendar.DAY_OF_YEAR) == lastRecord.get(Calendar.DAY_OF_YEAR) &&
                        todayCalendar.get(Calendar.YEAR) == lastRecord.get(Calendar.YEAR))) {
                    int bestDistance = Integer.MIN_VALUE;
                    float bestAverageSpeed = Float.MIN_VALUE;
                    for (Record record : records) {
                        if (record.getDistance() > bestDistance)
                            bestDistance = record.getDistance();
                        if (record.getAverageSpeed() > bestAverageSpeed)
                            bestAverageSpeed = record.getAverageSpeed();
                    }
                    Record today = records.get(0);
                    Record last = records.get(1);
                    long temp = today.getTimeSpent();
                    long hour = TimeUnit.MILLISECONDS.toHours(temp);
                    temp -= TimeUnit.HOURS.toMillis(hour);
                    long minute = TimeUnit.MILLISECONDS.toMinutes(temp);
                    temp -= TimeUnit.MINUTES.toMillis(minute);
                    long second = TimeUnit.MILLISECONDS.toSeconds(temp);
                    String append;
                    binding.todayTimeDisplay.setText(context.getString(R.string.totalTimeDisplay, hour, minute, second));

                    boolean setted = false;
                    if (bestDistance == today.getDistance()){
                        append = "(+" + (today.getDistance() - last.getDistance()) / 1000f + ", New Record!)";
                        binding.result.setText(context.getString(R.string.newRec));
                        setted = true;
                    } else {
                        if (today.getDistance() > last.getDistance()) {
                            append = "(+" + ((today.getDistance() - last.getDistance()) / 1000f) + ", " + ((today.getDistance() - bestDistance) / 1000f) + ")";
                            binding.result.setText(context.getString(R.string.disCon));
                            setted = true;
                        }else
                            append = "(" + ((today.getDistance() - last.getDistance()) / 1000f) + ", " + ((today.getDistance() - bestDistance) / 1000f) + ")";
                    }
                    binding.todayDistanceDisplay.setText(context.getString(R.string.totalDistanceDisplay, today.getDistance() / 1000f, append));

                    if (bestAverageSpeed == today.getAverageSpeed()){
                        append = "(+" + Math.round((today.getAverageSpeed() - last.getAverageSpeed()) * 100.0) / 100f + ", New Record!)";
                        binding.result.setText(context.getString(R.string.newRec));
                        setted = true;
                    } else {
                        if (today.getAverageSpeed() > last.getAverageSpeed()) {
                            append = "(+" + Math.round((today.getAverageSpeed() - last.getAverageSpeed()) * 100.0) / 100f + ", " + Math.round((today.getAverageSpeed() - bestAverageSpeed) * 100.0) / 100f + ")";
                            binding.result.setText(context.getString(R.string.aveCon));
                            setted = true;
                        }else
                            append = "(" + Math.round((today.getAverageSpeed() - last.getAverageSpeed()) * 100.0) / 100f + ", " + Math.round((today.getAverageSpeed() - bestAverageSpeed) * 100.0) / 100f + ")";
                    }
                    if (!setted)
                        binding.result.setText(R.string.nor);
                    binding.todayAverageSpeedDisplay.setText(context.getString(R.string.overallAverageSpeed, today.getAverageSpeed(), append));
                }
                //if there is only one record and is made today
                else if(records.size() == 1&&
                        todayCalendar.get(Calendar.DAY_OF_YEAR) == lastRecord.get(Calendar.DAY_OF_YEAR) &&
                        todayCalendar.get(Calendar.YEAR) == lastRecord.get(Calendar.YEAR)){
                    Record today = records.get(0);
                    long temp = today.getTimeSpent();
                    long hour = TimeUnit.MILLISECONDS.toHours(temp);
                    temp -= TimeUnit.HOURS.toMillis(hour);
                    long minute = TimeUnit.MILLISECONDS.toMinutes(temp);
                    temp -= TimeUnit.MINUTES.toMillis(minute);
                    long second = TimeUnit.MILLISECONDS.toSeconds(temp);
                    binding.todayTimeDisplay.setText(context.getString(R.string.totalTimeDisplay, hour, minute, second));
                    binding.todayDistanceDisplay.setText(context.getString(R.string.totalDistanceDisplay, today.getDistance() / 1000f, ", New Record!"));
                    binding.todayAverageSpeedDisplay.setText(context.getString(R.string.overallAverageSpeed, today.getAverageSpeed(), ", New Record!"));
                    binding.result.setText(R.string.newRec);
                }
            }
        });
    }
    //put the time spent data of each record into the graph
    private LineDataSet getTimeSpent(List<Record> records) {
        int timeSpentColor = Color.BLUE;
        List<Entry> timeSpent = new ArrayList<>();
        for (int i = records.size() - 1, j = 0; i >= 0; i--, j++)
            timeSpent.add(new Entry(j, records.get(i).getTimeSpent() / 1000f / 60 / 60));
        LineDataSet lineDataSet = new LineDataSet(timeSpent, "Time Spent");
        lineDataSet.setDrawCircles(true);
        lineDataSet.setCircleRadius(4);
        lineDataSet.setDrawValues(false);
        lineDataSet.setLineWidth(3);
        lineDataSet.setColor(timeSpentColor);
        lineDataSet.setCircleColor(timeSpentColor);
        lineDataSet.setCubicIntensity(1f);
        lineDataSet.setFillColor(timeSpentColor);
        lineDataSet.setDrawFilled(true);
        return lineDataSet;
    }

    //put the distance data of each record into the graph
    private LineDataSet getDistance(List<Record> records) {
        int distanceColor = Color.RED;
        List<Entry> distances = new ArrayList<>();
        for (int i = records.size() - 1, j = 0; i >= 0; i--, j++)
            distances.add(new Entry(j, records.get(i).getDistance() / 1000f));
        LineDataSet lineDataSet = new LineDataSet(distances, "Distance");
        lineDataSet.setDrawCircles(true);
        lineDataSet.setCircleRadius(4);
        lineDataSet.setDrawValues(false);
        lineDataSet.setLineWidth(3);
        lineDataSet.setColor(distanceColor);
        lineDataSet.setCircleColor(distanceColor);
        lineDataSet.setCubicIntensity(1f);
        lineDataSet.setFillColor(distanceColor);
        lineDataSet.setDrawFilled(true);
        return lineDataSet;
    }

    //put the average speed data of each record into the graph
    private LineDataSet getAverageSpeed(List<Record> records) {
        int averageColor = Color.GREEN;
        List<Entry> averageSpeeds = new ArrayList<>();
        for (int i = records.size() - 1, j = 0; i >= 0; i--, j++)
            averageSpeeds.add(new Entry(j, records.get(i).getAverageSpeed()));
        LineDataSet lineDataSet = new LineDataSet(averageSpeeds, "Average Speed");
        lineDataSet.setDrawCircles(true);
        lineDataSet.setCircleRadius(4);
        lineDataSet.setDrawValues(false);
        lineDataSet.setLineWidth(3);
        lineDataSet.setColor(averageColor);
        lineDataSet.setCircleColor(averageColor);
        lineDataSet.setCubicIntensity(1f);
        lineDataSet.setFillColor(averageColor);
        lineDataSet.setDrawFilled(true);
        return lineDataSet;
    }

    //reverse the list to make the graph looks more natural
    private List<Record> reverse(List<Record> records) {
        ArrayList<Record> reversed = new ArrayList<>();
        for (int i = records.size() - 1; i >= 0; i--)
            reversed.add(records.get(i));
        return reversed;
    }
}
