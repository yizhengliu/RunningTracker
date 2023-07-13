package com.finalcoursework.helpers;

import android.Manifest;
import android.graphics.Color;

//constants
public class Constants {
    public static final int LOCATION_REQUEST_CODE = 1;
    public static final int BACKGROUND_LOCATION_REQUEST_CODE = 0;
    public static final String FINE_LOCATION_PERMISSION_STRING = Manifest.permission.ACCESS_FINE_LOCATION;
    public static final String COARSE_LOCATION_PERMISSION_STRING = Manifest.permission.ACCESS_COARSE_LOCATION;
    public static final String BACKGROUND_LOCATION_PERMISSION_STRING = Manifest.permission.ACCESS_BACKGROUND_LOCATION;

    public static final String CHANNEL_ID = "Tracker channel";
    public static final String CHANNEL_NAME = "Tracking";
    public static final int NOTIFICATION_ID = 1;
    public static final int ACTIVITY_REQUEST_CODE = 0;
    public static final int RESUME_SERVICE_REQUEST_CODE = 1;
    public static final int PAUSE_SERVICE_REQUEST_CODE = 2;
    public static final int STOP_SERVICE_REQUEST_CODE = 3;
    public static final int UPDATE_INTERVAL_IN_MILLISECOND = 2000;
    public static final int PATH_COLOR = Color.RED;
    public static final float PATH_WIDTH = 5f;
    public static final float ZOOMING_CONSTANT = 18f;
    public static final Long TIMER_INTERVAL = 300L;
    public static final int RESUME = 1;
    public static final int PAUSE = 2;
    public static final int STOP = 3;
    public static final float PADDING_RATIO = 0.05f;

    public static final int BASED_ON_DATE = 0;
    public static final int BASED_ON_AVERAGE_SPEED = 1;
    public static final int BASED_ON_DISTANCE = 2;
    public static final int BASED_ON_TIME_SPENT = 3;
}
