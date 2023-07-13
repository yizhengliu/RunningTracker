package com.finalcoursework.contentProvider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.finalcoursework.dataBase.Record;
import com.finalcoursework.dataBase.RecordDataBase;

public class RecordsContentProvider extends ContentProvider {
    //use this to find if a uri is matched
    private static final UriMatcher uriMatcher;
    //define if matched what code will be returned
    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(RecordProviderContract.AUTHORITY,"records", 0);
        uriMatcher.addURI(RecordProviderContract.AUTHORITY,"records/#", 1);
        uriMatcher.addURI(RecordProviderContract.AUTHORITY,"*", 2);
    }
    @Override
    public boolean onCreate() {
        return true;
    }

    //return a Cursor object based on the match result (determine whether it contains one or many objects),
    //and notify it. If the uri is not allowed then throw exception
    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] strings, @Nullable String s, @Nullable String[] strings1, @Nullable String s1) {
        int matchResult = uriMatcher.match(uri);
        if (matchResult == 0 || matchResult == 1){
            Context context = getContext();
            if (context == null)
                return null;
            Cursor result;
            if (matchResult == 0)
                result = RecordDataBase.getDatabase(this.getContext()).getRecordDAO()
                        .getAll();
            else
                result = RecordDataBase.getDatabase(this.getContext()).getRecordDAO()
                        .getById(ContentUris.parseId(uri));
            result.setNotificationUri(context.getContentResolver(), uri);
            return result;
        }else
            throw new IllegalArgumentException("Unknown URI: " + uri);
    }

    //return whether the type based on the last segment, if it is null then it should be a multiple
    //type, if it has a number, then it should be a single type
    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        String type;
        if (uri.getLastPathSegment() == null)
            type = RecordProviderContract.CONTENT_TYPE_MULTIPLE;
        else
            type = RecordProviderContract.CONTENT_TYPE_SINGLE;
        return type;
    }

    //insert the content value into the database, exception will be thrown to indicate the error
    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues contentValues) {
        switch (uriMatcher.match(uri)){
            case 0:
                final Context context = getContext();
                if (context == null)
                    return null;
                long id = RecordDataBase.getDatabase(this.getContext()).getRecordDAO()
                        .insert(Record.fromContentValues(contentValues));
                Uri result = ContentUris.withAppendedId(uri, id);
                getContext().getContentResolver().notifyChange(result, null);
                return result;
            case 1:
                throw new IllegalArgumentException("Invalid URI: can not insert with ID: " + uri);
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }
    //delete a record based on the id, and notify.
    // If uri does not match then throw exceptions
    @Override
    public int delete(@NonNull Uri uri, @Nullable String s, @Nullable String[] strings) {
        switch (uriMatcher.match(uri)){
            case 0:
                throw new IllegalArgumentException("Invalid URI: can not delete without ID: " + uri);
            case 1:
                final Context context = getContext();
                if (context == null)
                    return 0;
                final int result = RecordDataBase.getDatabase(this.getContext()).getRecordDAO()
                        .deleteById(ContentUris.parseId(uri));
                getContext().getContentResolver().notifyChange(uri, null);
                return result;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }
    //update a record based on id, and notify.
    // If uri does not match then throw exception
    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues contentValues, @Nullable String s, @Nullable String[] strings) {
        switch (uriMatcher.match(uri)){
            case 0:
                throw new IllegalArgumentException("Invalid URI: can not update without ID: " + uri);
            case 1:
                final Context context = getContext();
                if (context == null)
                    return 0;
                Record record = Record.fromContentValues(contentValues);
                record.setId(ContentUris.parseId(uri));
                final int result = RecordDataBase.getDatabase(this.getContext()).getRecordDAO()
                        .update(record);
                context.getContentResolver().notifyChange(uri, null);
                return result;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }
}
