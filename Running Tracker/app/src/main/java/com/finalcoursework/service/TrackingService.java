package com.finalcoursework.service;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static com.finalcoursework.helpers.Constants.ACTIVITY_REQUEST_CODE;
import static com.finalcoursework.helpers.Constants.CHANNEL_ID;
import static com.finalcoursework.helpers.Constants.CHANNEL_NAME;
import static com.finalcoursework.helpers.Constants.NOTIFICATION_ID;
import static com.finalcoursework.helpers.Constants.PAUSE;
import static com.finalcoursework.helpers.Constants.PAUSE_SERVICE_REQUEST_CODE;
import static com.finalcoursework.helpers.Constants.RESUME;
import static com.finalcoursework.helpers.Constants.RESUME_SERVICE_REQUEST_CODE;
import static com.finalcoursework.helpers.Constants.STOP;
import static com.finalcoursework.helpers.Constants.STOP_SERVICE_REQUEST_CODE;
import static com.finalcoursework.helpers.Constants.TIMER_INTERVAL;
import static com.finalcoursework.helpers.Constants.UPDATE_INTERVAL_IN_MILLISECOND;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;

import com.finalcoursework.R;
import com.finalcoursework.dataBase.Record;
import com.finalcoursework.ui.TrackActivity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.model.LatLng;

import android.os.Looper;
import android.os.RemoteCallbackList;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.ArrayList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class TrackingService extends Service {
    //semaphore to avoid call back concurrency issue
    private final Semaphore callBackSemaphore = new Semaphore(1);
    private FusedLocationProviderClient fusedLocationClient;
    //location callback, add each location point
    private final LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(@NonNull LocationResult locationResult) {
            if (locationResult.getLastLocation() != null)
                addPathJoint(locationResult.getLastLocation());
            if (lastLocation != null)
                distanceTraveled += locationResult.getLastLocation().distanceTo(lastLocation);
            callDistanceBack();
            lastLocation = locationResult.getLastLocation();
        }
    };
    private RemoteCallbackList<TrackingBinder> remoteCallbackList = new RemoteCallbackList<>();
    //paths that user has travelled during the recording, a path means a series of location user
    //has travelled
    private ArrayList<ArrayList<LatLng>> paths = new ArrayList<>();
    //number of activities connected to the service
    //since new activity will be created before the last one has been destroyed, so we use this
    //to track the connection
    private int numConnected = 0;
    //the last location user was at
    private Location lastLocation = null;
    //the start time of the service
    private Long serviceStartedTime;
    //total distance travelled during the recording
    private float distanceTraveled = 0f;
    //time spent in previous paths
    private Long timeInPreviousLaps = 0L;
    //start time of the new path
    private Long lapStartedTime;
    //time spent in the current path
    private Long timeInCurrentLap = 0L;
    //whether the service is paused
    private Boolean isTracking = false;
    private Thread timeUpdateThread;
    //whether the dialog is altering, as previously mentioned, we need to save the map snapshot,
    //and the service will be still there unless user click the button on the alter window so it
    //makes sense to keep a reference here rather than using save instance method which might crash
    //the app
    private Boolean isAltering = false;
    //the annotation user has made, same reason as the reference of alter window, keep the information
    //inside the service
    private String annotation;
    //indicates whether the service is first time started by the activity
    private Boolean isFirstTime = true;
    //indicates whether parameters are reset because we have got an record today,
    //and this will lead into the query whether to update the existing record or insert a record
    private Boolean isSet = false;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new TrackingBinder();
    }

    //create a new notification channel and initialize the fused location client when the service is created.
    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW);
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE))
                .createNotificationChannel(channel);
        Log.d("TAG", "onCreate: service created");
    }
    //when receive actions, whether to start, pause or stop the service.
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getAction() != null) {
            if (intent.getAction().equals("startRecording")) {
                Log.d("TAG", "onStartCommand: stated");
                serviceStartedTime = System.currentTimeMillis();
                resumeRecording();
                startForeground(NOTIFICATION_ID, getNotificationBuilder().build());
            } else if (intent.getAction().equals("resume")) {
                if (!callNotificationActionBack(RESUME))
                    resumeRecording();
                ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(NOTIFICATION_ID, getNotificationBuilder().build());
            } else if (intent.getAction().equals("pause")) {
                if (!callNotificationActionBack(PAUSE))
                    pauseRecording();
                ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(NOTIFICATION_ID, getNotificationBuilder().build());
            } else if (intent.getAction().equals("stop")) {
                if (!callNotificationActionBack(STOP)) {
                    stopRecording();
                }
            }
        }
        super.onStartCommand(intent, flags, startId);
        //start redeliver intent, in case user lose their data if system shut down this app
        return START_REDELIVER_INTENT;
    }

    //when service is destroyed, cancel all notifications, delete the notification channel and stop
    //the service
    @Override
    public void onDestroy() {
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancelAll();
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).deleteNotificationChannel(CHANNEL_ID);
        stopForeground(true);
        super.onDestroy();
    }
    //binder class for communication between activity and the service
    public class TrackingBinder extends Binder implements IInterface {
        protected ICallBack callBack;

        @Override
        public IBinder asBinder() {
            return this;
        }

        public Long getStartTime() {
            return serviceStartedTime;
        }

        public Long getTimeSpent() {
            return timeInPreviousLaps + timeInCurrentLap;
        }

        public String getCurrentTime() {
            return formatTime(timeInPreviousLaps + timeInCurrentLap);
        }

        public String getCurrentDistance() {
            return getString(R.string.displayDistance, (int) distanceTraveled);
        }

        public int getDistance() {
            return (int) distanceTraveled;
        }
        //in case if onServiceDisconnected is not called
        public void activityDestroyed() {
            unregisterCallback();
            numConnected--;
            if (((NotificationManager) TrackingService.this.getSystemService(Context.NOTIFICATION_SERVICE))
                    .getNotificationChannel(CHANNEL_ID) != null)
                ((NotificationManager) TrackingService.this
                        .getSystemService(Context.NOTIFICATION_SERVICE))
                        .notify(NOTIFICATION_ID, getNotificationBuilder().build());
        }

        public void resumeService() {
            resumeRecording();
        }

        public void pauseService() {
            pauseRecording();
        }

        public void stopService() {
            stopRecording();
        }

        public Boolean isTracking() {
            return isTracking;
        }

        public void setIsAltering(Boolean newValue) {
            isAltering = newValue;
        }

        public Boolean isAltering() {
            return isAltering;
        }

        public void setAnnotation(String value) {
            annotation = value;
        }

        public String getAnnotation() {
            return annotation;
        }

        public ArrayList<ArrayList<LatLng>> getPaths() {
            return paths;
        }
        //indicates how many activity has been connected
        public void setIsConnected(Boolean value) {
            if (value) numConnected++;
            else numConnected--;
        }

        public boolean isFirstTime() {
            return isFirstTime;
        }

        //since the onCreate of the new activity is called before the onDestroy of the old activity,
        //the remote callback list unregister function will cause bug, therefore, we will use a new
        //remoteCallbackList to track the new activity. Thus unregisterCallback method will do nothing here
        public void unregisterCallback() {
        }
        public void registerCallback(ICallBack callBack) {
            this.callBack = callBack;
            remoteCallbackList = new RemoteCallbackList<>();
            remoteCallbackList.register(TrackingBinder.this);
            Log.d("tag", "registerCallback: length = " + remoteCallbackList.getRegisteredCallbackCount());
            callDistanceBack();
            callTimeBack(timeInPreviousLaps);
        }

        public boolean isSet() {
            return isSet;
        }
        //reset the parameter if we already have got a record for today, keep tracking to update it
        //rather than insert a new one
        public void setParameters(Record record) {
            Log.d("TAG", "setParameters: ");
            isFirstTime = false;
            distanceTraveled = record.getDistance();
            timeInPreviousLaps = record.getTimeSpent();
            paths = record.getPaths();
            isSet = true;
        }

    }

    //format the time into HH:MM:SS
    protected String formatTime(Long timeInMilli) {
        Long temp = timeInMilli;
        long hour = TimeUnit.MILLISECONDS.toHours(temp);
        temp -= TimeUnit.HOURS.toMillis(hour);
        long minute = TimeUnit.MILLISECONDS.toMinutes(temp);
        temp -= TimeUnit.MINUTES.toMillis(minute);
        Long second = TimeUnit.MILLISECONDS.toSeconds(temp);
        return getString(R.string.displayTime, hour, minute, second);
    }

    //tell activity (if there is one) button on the notification has been pressed,return true if
    //the callback method has been called successfully, false indicates no activity is connected with
    //this service
    private Boolean callNotificationActionBack(int state) {
        if (numConnected > 0) {
            try {
                callBackSemaphore.acquire();
            } catch (InterruptedException ignored) {
            }
            remoteCallbackList.beginBroadcast();
            if (remoteCallbackList.getRegisteredCallbackCount() > 0)
                remoteCallbackList.getRegisteredCallbackItem(0).callBack.stateChanged(state);
            remoteCallbackList.finishBroadcast();
            callBackSemaphore.release();
            return true;
        }
        return false;
    }

    //call back method to display how long user has been recorded on to the associated activity map.
    private void callTimeBack(Long totalRecordedTime) {
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(NOTIFICATION_ID, getNotificationBuilder().build());
        if (numConnected > 0) {
            try {
                callBackSemaphore.acquire();
            } catch (InterruptedException ignored) {
                return;
            }
            remoteCallbackList.beginBroadcast();
            if (remoteCallbackList.getRegisteredCallbackCount() > 0) {
                remoteCallbackList.getRegisteredCallbackItem(0).callBack
                        .timeChanged(formatTime(totalRecordedTime));
            }
            remoteCallbackList.finishBroadcast();
            callBackSemaphore.release();
        }
    }

    //call back method to display how far user has been recorded on to the associated activity map.
    private void callDistanceBack() {
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(NOTIFICATION_ID, getNotificationBuilder().build());
        if (numConnected > 0) {
            try {
                callBackSemaphore.acquire();
            } catch (InterruptedException ignored) {
            }
            remoteCallbackList.beginBroadcast();
            if (remoteCallbackList.getRegisteredCallbackCount() > 0) {
                Log.d("tag", "registerCallback: " + numConnected);
                remoteCallbackList.getRegisteredCallbackItem(0).callBack
                        .distanceChanged(getString(R.string.displayDistance, (int) distanceTraveled));
            }
            remoteCallbackList.finishBroadcast();
            callBackSemaphore.release();
        }
    }

    //call back method to draw the current path on to the associated activity map.
    private void callCurrentPathsBack() {
        if (numConnected > 0) {
            try {
                callBackSemaphore.acquire();
            } catch (InterruptedException ignored) {
            }
            remoteCallbackList.beginBroadcast();
            if (remoteCallbackList.getRegisteredCallbackCount() > 0) {
                Log.d("tag", "callCurrentPathsBack: " + remoteCallbackList.getRegisteredCallbackCount());
                remoteCallbackList.getRegisteredCallbackItem(0).callBack
                        .pathsChanged(paths.get(paths.size() - 1));
            }
            remoteCallbackList.finishBroadcast();
            callBackSemaphore.release();
        }
    }

    //Add a new path, start to track user's position and update the time spent.
    //if user changed permission when the application is still running, the application will be
    //automatically shut down, so no worry of lack of permission (no need to navigate back to permission
    // activity). try catch here is just to make sure everything works fine.
    private void resumeRecording() {
        Log.d("tag", "resumeRecording: resuming");
        startTimer();
        lastLocation = null;
        try {
            LocationRequest locationRequest = new
                    LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL_IN_MILLISECOND).build();
            fusedLocationClient.requestLocationUpdates(locationRequest,
                    locationCallback,
                    Looper.getMainLooper());
        } catch (SecurityException e) {
            Toast.makeText(this, "Background location permission has not been granted yet.", Toast.LENGTH_SHORT).show();
            stopRecording();
        }
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(NOTIFICATION_ID, getNotificationBuilder().build());
    }
    //start the time tracking thread and add a new path
    private void startTimer() {
        lapStartedTime = System.currentTimeMillis();
        isTracking = true;
        timeUpdateThread = getUpdateTime();
        timeUpdateThread.start();
        addEmptyPath();
    }

    //pause the service by remove the location changing callback
    private void pauseRecording() {
        Log.d("TAG", "pauseRecording: paused");
        isTracking = false;
        fusedLocationClient.removeLocationUpdates(locationCallback);
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(NOTIFICATION_ID, getNotificationBuilder().build());
    }

    //stop the recoding by remove the location changing callback, cancel all notifications and stop the service
    private void stopRecording() {
        Log.d("tag", "stopping");
        isTracking = false;
        if (timeUpdateThread != null && timeUpdateThread.isAlive())
            timeUpdateThread.interrupt();
        fusedLocationClient.removeLocationUpdates(locationCallback);
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel(NOTIFICATION_ID);
        stopForeground(true);
        this.stopSelf();
    }

    //return the notification builder that user are able to get back to the activity, to resume, pause
    // and stop the service.
    private NotificationCompat.Builder getNotificationBuilder() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_baseline_directions_run_24)
                .setContentTitle("Running Tracker")
                .setContentText(formatTime(timeInPreviousLaps + timeInCurrentLap) + "    "
                        + getString(R.string.displayDistance, (int) distanceTraveled) + " "
                        + getString(R.string.pressMe))
                .setContentIntent(backToActivityPendingIntent())
                .addAction(isTracking ? R.drawable.ic_baseline_pause_24 : R.drawable.ic_baseline_play_arrow_24,
                        isTracking ? "pause" : "resume",
                        isTracking ? pauseServicePendingIntent() : resumeServicePendingIntent())
                .addAction(R.drawable.ic_baseline_stop_24,
                        numConnected > 0 ? "stop" : "stop without saving", stopServicePendingIntent());
    }

    //add a new LatLng location into the latest path and post the value of whole paths to the activity
    //for drawing on to the map
    private void addPathJoint(Location newLocation) {
        paths.get(paths.size() - 1).add(
                new LatLng(newLocation.getLatitude(), newLocation.getLongitude()));
        callCurrentPathsBack();
    }
    //add a new path
    private void addEmptyPath() {
        paths.add(new ArrayList<>());
    }

    //pending intent for notification context, when notification is pressed, back to the tracking activity
    private PendingIntent backToActivityPendingIntent() {
        Intent backToActivityIntent = new Intent(TrackingService.this, TrackActivity.class);
        backToActivityIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        backToActivityIntent.setAction("backed");
        return PendingIntent.getActivity(this, ACTIVITY_REQUEST_CODE, backToActivityIntent, FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
    //to resume the service, action button on the notification
    private PendingIntent resumeServicePendingIntent() {
        Intent resumeIntent = new Intent(TrackingService.this, TrackingService.class).setAction("resume");
        return PendingIntent.getService(this, RESUME_SERVICE_REQUEST_CODE, resumeIntent, FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
    //to pause the service, action button on the notification
    private PendingIntent pauseServicePendingIntent() {
        Intent pauseIntent = new Intent(TrackingService.this, TrackingService.class).setAction("pause");
        return PendingIntent.getService(this, PAUSE_SERVICE_REQUEST_CODE, pauseIntent, FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
    //to stop the service, action button on the notification
    private PendingIntent stopServicePendingIntent() {
        Intent stopIntent = new Intent(TrackingService.this, TrackingService.class).setAction("stop");
        return PendingIntent.getService(this, STOP_SERVICE_REQUEST_CODE, stopIntent, FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    //return a new thread which will update the time spent
    private Thread getUpdateTime() {
        return new Thread(() -> {
            while (isTracking) {
                timeInCurrentLap = System.currentTimeMillis() - lapStartedTime;
                callTimeBack(timeInPreviousLaps + timeInCurrentLap);
                try {
                    Thread.sleep(TIMER_INTERVAL);
                } catch (InterruptedException ignored) {
                    return;
                }
            }
            timeInPreviousLaps += timeInCurrentLap;
            timeInCurrentLap = 0L;
            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(NOTIFICATION_ID, getNotificationBuilder().build());
        });
    }

}
