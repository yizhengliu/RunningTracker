package com.finalcoursework.dataBase;

import static com.finalcoursework.contentProvider.RecordProviderContract.AVERAGE_SPEED;
import static com.finalcoursework.contentProvider.RecordProviderContract.CUSTOM_IMAGE;
import static com.finalcoursework.contentProvider.RecordProviderContract.DATE;
import static com.finalcoursework.contentProvider.RecordProviderContract.DISTANCE;
import static com.finalcoursework.contentProvider.RecordProviderContract.IMAGE;
import static com.finalcoursework.contentProvider.RecordProviderContract.NOTE;
import static com.finalcoursework.contentProvider.RecordProviderContract.PATHS;
import static com.finalcoursework.contentProvider.RecordProviderContract.TIME_SPENT;

import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;

//database table
@Entity(tableName = "recordTable")
public class Record {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "ID")
    long id;

    //Google map snapshot shows the route user has travelled
    Bitmap image;
    //date of a record started recording
    Long date;
    //average speed of a record
    Float averageSpeed;
    //distance travelled of a record
    int distance;
    //time spent of a record
    Long timeSpent;
    //user note of a record
    String note;
    //paths recorded by the service for further usage of a record
    ArrayList<ArrayList<LatLng>> paths;
    //user chosen custom image of a record
    Bitmap customImage;

    public Record(Bitmap image, Long date, Float averageSpeed, int distance, Long timeSpent,
                  String note, ArrayList<ArrayList<LatLng>> paths, Bitmap customImage) {
        this.image = image;
        this.date = date;
        this.averageSpeed = averageSpeed;
        this.distance = distance;
        this.timeSpent = timeSpent;
        this.note = note;
        this.paths = paths;
        this.customImage = customImage;
    }
    //return a new record object from the parameter, if nothing matches then the default one will be
    //returned
    public static Record fromContentValues(ContentValues contentValues) {
        Bitmap image = null;
        Long date = 0L;
        Float averageSpeed = 0F;
        int distance = 0;
        Long timeSpent = 0L;
        String note = "No annotation";
        ArrayList<ArrayList<LatLng>> paths = new ArrayList<>();
        Bitmap customImage = null;
        if (contentValues != null) {
            if (contentValues.containsKey(IMAGE))
                image = BitmapFactory.decodeByteArray(contentValues.getAsByteArray(IMAGE), 0,
                        contentValues.getAsByteArray(IMAGE).length);
            if (contentValues.containsKey(DATE))
                date = contentValues.getAsLong(DATE);
            if (contentValues.containsKey(AVERAGE_SPEED))
                averageSpeed = contentValues.getAsFloat(AVERAGE_SPEED);
            if (contentValues.containsKey(DISTANCE))
                distance = contentValues.getAsInteger(DISTANCE);
            if (contentValues.containsKey(TIME_SPENT))
                timeSpent = contentValues.getAsLong(TIME_SPENT);
            if (contentValues.containsKey(NOTE))
                note = contentValues.getAsString(NOTE);
            if (contentValues.containsKey(PATHS))
                paths = new Gson().fromJson(contentValues.getAsString(PATHS),
                        new TypeToken<ArrayList<ArrayList<LatLng>>>() {
                        }.getType());
            if (contentValues.containsKey(CUSTOM_IMAGE))
                customImage = BitmapFactory.decodeByteArray(
                        contentValues.getAsByteArray(CUSTOM_IMAGE), 0,
                        contentValues.getAsByteArray(CUSTOM_IMAGE).length);
        }
        return new Record(image, date, averageSpeed, distance, timeSpent, note, paths, customImage);
    }
    //set id for update query to update the correct one, because new record created from the fromContentValues function does not have an ID
    public void setId(long id) {
        this.id = id;
    }

    public Bitmap getImage() {
        return image;
    }

    public Float getAverageSpeed() {
        return averageSpeed;
    }

    public int getDistance() {
        return distance;
    }

    public long getId() {
        return id;
    }

    public Long getTimeSpent() {
        return timeSpent;
    }

    public Long getDate() {
        return date;
    }

    public String getNote() {
        return note;
    }

    public ArrayList<ArrayList<LatLng>> getPaths() {
        return paths;
    }

    public Bitmap getCustomImage() {
        return customImage;
    }



}
