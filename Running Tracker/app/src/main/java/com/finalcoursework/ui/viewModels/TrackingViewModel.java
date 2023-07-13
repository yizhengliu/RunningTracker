package com.finalcoursework.ui.viewModels;

import static com.finalcoursework.helpers.Constants.BASED_ON_DATE;

import android.app.Application;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.finalcoursework.dataBase.Record;
import com.finalcoursework.dataBase.RecordRepository;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

public class TrackingViewModel extends AndroidViewModel {
    private final RecordRepository repository;
    public final LiveData<List<Record>> records;

    public TrackingViewModel(@NonNull Application application) {
        super(application);
        repository = new RecordRepository(application);
        records = repository.getRecords(BASED_ON_DATE);
    }

    //insert a new record if the last record is not today's
    public void insert(Record record) {
        repository.insert(record);
    }

    //if the last record is today's record then update today's one
    public void updateAll(Bitmap pri, Float averageSpeed,
                          int distance, Long timeSpent, String note,
                          ArrayList<ArrayList<LatLng>> paths, long id) {
        repository.updateAll(pri, averageSpeed, distance,
                timeSpent, note, paths, id);
    }
}
