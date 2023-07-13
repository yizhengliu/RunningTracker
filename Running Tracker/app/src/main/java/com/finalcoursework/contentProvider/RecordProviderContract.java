package com.finalcoursework.contentProvider;

//contracts for external storage access
public class RecordProviderContract {
    public static final String AUTHORITY = "psyyl.com.finalcourseworkMDP.finalcourserwork.Recordprovider";

    public static final String IMAGE = "image";
    public static final String DATE = "date";
    public static final String AVERAGE_SPEED = "average_speed";
    public static final String DISTANCE = "distance";
    public static final String TIME_SPENT = "time_spent";
    public static final String NOTE = "note";
    public static final String PATHS = "paths";
    public static final String CUSTOM_IMAGE = "custom_image";

    public static final String CONTENT_TYPE_SINGLE = "android.cursor.item";
    public static final String CONTENT_TYPE_MULTIPLE = "android.cursor.dir";
}
