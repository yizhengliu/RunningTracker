package com.finalcoursework.ui.viewModels;

import static com.finalcoursework.helpers.Constants.BASED_ON_AVERAGE_SPEED;
import static com.finalcoursework.helpers.Constants.BASED_ON_DATE;
import static com.finalcoursework.helpers.Constants.BASED_ON_DISTANCE;
import static com.finalcoursework.helpers.Constants.BASED_ON_TIME_SPENT;

import android.app.Application;
import android.graphics.Bitmap;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.finalcoursework.dataBase.Record;
import com.finalcoursework.dataBase.RecordRepository;

import java.util.List;

public class PreviewViewModel extends AndroidViewModel{
    //current sort type
    private int currentType = BASED_ON_DATE;
    //keep references of livedata for further observation
    private final LiveData<List<Record>> recordsSortedByDate;
    private final LiveData<List<Record>> recordsSortedByTimeSpent;
    private final LiveData<List<Record>> recordsSortedByDistance;
    private final LiveData<List<Record>> recordsSortedByAverageSpeed;
    //if user changed the sort type by using this to notify the recycler view adapter to perform
    //correct operation
    public MediatorLiveData<List<Record>> records = new MediatorLiveData<>();
    private final RecordRepository repository;

    public PreviewViewModel(Application application) {
        super(application);
        repository = new RecordRepository(application);
        recordsSortedByAverageSpeed = repository.getRecords(BASED_ON_AVERAGE_SPEED);
        recordsSortedByDate = repository.getRecords(BASED_ON_DATE);
        recordsSortedByTimeSpent = repository.getRecords(BASED_ON_TIME_SPENT);
        recordsSortedByDistance = repository.getRecords(BASED_ON_DISTANCE);
        addSources();
    }
    //change the value based on the current sort type
    public void setCurrentType(int currentType) {
        this.currentType = currentType;
        switch (currentType){
            case BASED_ON_DATE:
                records.postValue(recordsSortedByDate.getValue());
                break;
            case BASED_ON_TIME_SPENT:
                records.postValue(recordsSortedByTimeSpent.getValue());
                break;
            case BASED_ON_DISTANCE:
                records.postValue(recordsSortedByDistance.getValue());
                break;
            case BASED_ON_AVERAGE_SPEED:
                records.postValue(recordsSortedByAverageSpeed.getValue());
                break;
        }
    }

    public int getCurrentType(){
        return currentType;
    }
    //tell what the mediator livedata to do when the current type is changed
    private void addSources(){
        records.addSource(recordsSortedByDate, result->{
            if (currentType == BASED_ON_DATE)
                records.postValue(result);
        });
        records.addSource(recordsSortedByAverageSpeed, result->{
            if (currentType == BASED_ON_AVERAGE_SPEED)
                records.postValue(result);
        });
        records.addSource(recordsSortedByTimeSpent, result->{
            if (currentType == BASED_ON_TIME_SPENT)
                records.postValue(result);
        });
        records.addSource(recordsSortedByDistance, result->{
            if (currentType == BASED_ON_DISTANCE)
                records.postValue(result);
        });
    }
    //delete a record, used when user doing the swipe operation
    public void delete(Record record){
        repository.delete(record);
    }

    //update custom image of a record
    public void updateCus(Bitmap image, int id){
        repository.updateCus(image, id);
    }
}
