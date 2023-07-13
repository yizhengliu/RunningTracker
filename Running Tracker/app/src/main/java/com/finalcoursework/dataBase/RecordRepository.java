package com.finalcoursework.dataBase;

import static com.finalcoursework.helpers.Constants.BASED_ON_AVERAGE_SPEED;
import static com.finalcoursework.helpers.Constants.BASED_ON_DATE;
import static com.finalcoursework.helpers.Constants.BASED_ON_DISTANCE;
import static com.finalcoursework.helpers.Constants.BASED_ON_TIME_SPENT;

import android.app.Application;
import android.graphics.Bitmap;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

//an interface for view models to interact with the database
public class RecordRepository {
    private final RecordDAO recordDao;
    //keep a DAO reference for query usage
    public RecordRepository(Application application) {
        RecordDataBase db = RecordDataBase.getDatabase(application);
        recordDao = db.getRecordDAO();
    }

    public LiveData<Long> getTotalTimeSpent() {
        return recordDao.getTotalTimeSpent();
    }

    public LiveData<Integer> getTotalDistance() {
        return recordDao.getTotalDistance();
    }

    public LiveData<Float> getOverallAverageSpeed() {
        return recordDao.getOverallAverageSpeed();
    }

    //get records based on the type
    public LiveData<List<Record>> getRecords(int type) {
        switch (type) {
            case BASED_ON_DATE:
                Log.d("TAG", "getRecords: return date");
                return recordDao.getRecordsSortedByDate();
            case BASED_ON_AVERAGE_SPEED:
                Log.d("TAG", "getRecords: return as");
                return recordDao.getRecordsSortedByAverageSpeed();
            case BASED_ON_DISTANCE:
                Log.d("TAG", "getRecords: return dis");
                return recordDao.getRecordsSortedByDistance();
            case BASED_ON_TIME_SPENT:
                Log.d("TAG", "getRecords: return ts");
                return recordDao.getRecordsSortedByTimeSpent();
        }
        Log.d("TAG", "getRecords: return null");
        return null;
    }
    //delete an specific record
    public void delete(Record record){
        RecordDataBase.databaseWriteExecutor.execute(()->recordDao.delete(record));
    }

    public void insert(Record record) {
        RecordDataBase.databaseWriteExecutor.execute(() -> recordDao.insert(record));
    }
    //update custom image of a record based on the id
    public void updateCus(Bitmap image, int id) {
        RecordDataBase.databaseWriteExecutor.execute(() -> recordDao.updateCus(image, id));
    }
    //update all fields in the parameter based on the id parameter
    public void updateAll(Bitmap pri,Float averageSpeed,
                          int distance, Long timeSpent, String note,
                          ArrayList<ArrayList<LatLng>> paths, long id){
        RecordDataBase.databaseWriteExecutor.execute(
                ()->recordDao.updateAll(pri, averageSpeed, distance,
                        timeSpent, note, paths, id));
    }
}
