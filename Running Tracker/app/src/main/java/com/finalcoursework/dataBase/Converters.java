package com.finalcoursework.dataBase;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.room.TypeConverter;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

public class Converters {
    //convert a bitmap object into byte array form for database to save the data(otherwise won't be accepted).
    //database does not support to save a raw bitmap object
    @TypeConverter
    public byte[] fromBitMap(Bitmap bitmap) {
        if (bitmap != null){
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            return outputStream.toByteArray();
        }
        return new byte[]{};
    }
    //translate the bytearray back into the bitmap object
    @TypeConverter
    public Bitmap toBitMap(byte[] byteArray){
        if (byteArray.length != 0)
            return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
        return null;
    }
    //here is the only usage of Json library, the main purpose is to save complex data type into the
    //data base.
    //https://github.com/google/gson, here is the link for Json library
    //Using Json library to convert the arraylist object into string to let database save(otherwise won't be accepted)
    //database does not support to save a raw ArrayList object
    @TypeConverter
    public String fromPaths(ArrayList<ArrayList<LatLng>> paths){
        if (paths != null)
            return new Gson().toJson(paths);
        return "";
    }
    //convert the pre-saved string back to the array list for further usage
    @TypeConverter
    public ArrayList<ArrayList<LatLng>> toPaths(String value) {
        if (!value.equals(""))
            return new Gson().fromJson(value,
                new TypeToken<ArrayList<ArrayList<LatLng>>>(){}.getType());
        return null;
    }

}

