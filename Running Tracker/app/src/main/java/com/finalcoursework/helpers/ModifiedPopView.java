package com.finalcoursework.helpers;

import android.annotation.SuppressLint;
import android.content.Context;
import android.widget.TextView;

import com.finalcoursework.R;
import com.finalcoursework.dataBase.Record;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;


import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

//popup window when user click on a point of the graph in the statistics activity
//there is a api bug that the first time a point is pressed, whe window does not show properly,
//so user will need to at least click twice (only one time), then all operation will be performed
//correctly
@SuppressLint("ViewConstructor")
public class ModifiedPopView extends MarkerView {
    private final List<Record> records;
    private final TextView date;
    private final TextView distance;
    private final TextView averageSpeed;
    private final TextView timeSpent;

    public ModifiedPopView(List<Record> records, Context context, int layoutId){
        super(context, layoutId);
        this.records = records;
        date = findViewById(R.id.datePop);
        distance = findViewById(R.id.distancePop);
        averageSpeed = findViewById(R.id.averageSpeedPop);
        timeSpent = findViewById(R.id.timeSpentPop);
    }

    //indicates where should the pop window shows
    @Override
    public MPPointF getOffset() {
        return new MPPointF(-getWidth() / 1.2f, -getHeight() + 5);
    }

    //initialize the actual view of the pop-up window
    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        super.refreshContent(e, highlight);
        if (e == null)
            return;
        Record record = records.get((int) e.getX());
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(record.getDate());
        SimpleDateFormat dateFormat= new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        date.setText(String.format(Locale.getDefault(),"Start Time: %s", dateFormat.format(calendar.getTime())));

        averageSpeed.setText(String.format(Locale.getDefault(),"Average Speed: %.2f Km/h", record.getAverageSpeed()));

        distance.setText(String.format(Locale.getDefault(),"Distance Traveled: %.3f Km", record.getDistance() / 1000f));

        long temp = record.getTimeSpent();
        long hour = TimeUnit.MILLISECONDS.toHours(temp);
        temp -= TimeUnit.HOURS.toMillis(hour);
        long minute = TimeUnit.MILLISECONDS.toMinutes(temp);
        temp -= TimeUnit.MINUTES.toMillis(minute);
        long second = TimeUnit.MILLISECONDS.toSeconds(temp);
        timeSpent.setText(String.format(Locale.getDefault(),"Time spent: %dH %dM %dS",hour,minute,second));
    }
}
