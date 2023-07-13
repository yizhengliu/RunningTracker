package com.finalcoursework.dataBase;

import android.database.Cursor;
import android.graphics.Bitmap;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

@Dao
public interface RecordDAO {

    //insert a record and ignore if conflict happens
    //return long value indicates the id of the newly inserted record
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(Record record);

    //delete a specific record
    @Delete
    void delete(Record record);

    //get records based on conditions
    @Query("SELECT * FROM recordTable ORDER BY date DESC")
    LiveData<List<Record>> getRecordsSortedByDate();

    @Query("SELECT * FROM recordTable ORDER BY timeSpent DESC")
    LiveData<List<Record>> getRecordsSortedByTimeSpent();

    @Query("SELECT * FROM recordTable ORDER BY averageSpeed DESC")
    LiveData<List<Record>> getRecordsSortedByAverageSpeed();

    @Query("SELECT * FROM recordTable ORDER BY distance DESC")
    LiveData<List<Record>> getRecordsSortedByDistance();

    //get overall data
    @Query("SELECT SUM(timeSpent) FROM recordTable")
    LiveData<Long> getTotalTimeSpent();

    @Query("SELECT SUM(distance) FROM recordTable")
    LiveData<Integer> getTotalDistance();

    @Query("SELECT AVG(averageSpeed) FROM recordTable")
    LiveData<Float> getOverallAverageSpeed();

    //update custom image of a record based on id
    @Query("UPDATE recordTable SET customImage = :customImage WHERE id = :id")
    void updateCus(Bitmap customImage, int id);

    //update all fields apart from custom image based on id
    @Query("UPDATE recordTable SET image = :image,averageSpeed = :averageSpeed," +
            " distance = :distance, timeSpent = :timeSpent, note = :note, paths = :paths WHERE id = :id")
    void updateAll(Bitmap image, Float averageSpeed, int distance, Long timeSpent, String note,
                   ArrayList<ArrayList<LatLng>> paths, long id);
    //delete by id, and return how many records have been deleted, should always return 1
    @Query("DELETE FROM recordTable WHERE id =:id")
    int deleteById(long id);
    //return a cursor object contains all the records
    @Query("SELECT * FROM recordTable")
    Cursor getAll();
    //return a cursor object contains only the record with the id.
    @Query("SELECT * FROM recordTable WHERE id = :id")
    Cursor getById(long id);
    //update a record, return the number of records have been updated, should always be 1
    @Update
    int update(Record record);
}
