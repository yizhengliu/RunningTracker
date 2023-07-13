package com.finalcoursework.ui.viewModels;

import static com.finalcoursework.helpers.Constants.BASED_ON_DATE;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.finalcoursework.dataBase.Record;
import com.finalcoursework.dataBase.RecordRepository;

import java.util.List;
//keep a reference of the live data for handler to observe and use
public class StatisticsViewModel  extends AndroidViewModel {
    public final LiveData<Long> totalTimeSpent;
    public final LiveData<Integer> totalDistance;
    public final LiveData<Float> overallAverageSpeed;
    public final LiveData<List<Record>> records;


    public StatisticsViewModel(@NonNull Application application) {
        super(application);
        RecordRepository repository = new RecordRepository(application);
        records = repository.getRecords(BASED_ON_DATE);
        totalTimeSpent = repository.getTotalTimeSpent();
        overallAverageSpeed = repository.getOverallAverageSpeed();
        totalDistance = repository.getTotalDistance();
    }
}
