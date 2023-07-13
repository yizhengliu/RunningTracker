package com.finalcoursework.helpers;

import static android.content.Context.BIND_AUTO_CREATE;
import static com.finalcoursework.helpers.Constants.BACKGROUND_LOCATION_PERMISSION_STRING;
import static com.finalcoursework.helpers.Constants.FINE_LOCATION_PERMISSION_STRING;
import static com.finalcoursework.helpers.Constants.PADDING_RATIO;
import static com.finalcoursework.helpers.Constants.PATH_COLOR;
import static com.finalcoursework.helpers.Constants.PATH_WIDTH;
import static com.finalcoursework.helpers.Constants.STOP;
import static com.finalcoursework.helpers.Constants.UPDATE_INTERVAL_IN_MILLISECOND;
import static com.finalcoursework.helpers.Constants.ZOOMING_CONSTANT;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.IBinder;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.finalcoursework.R;
import com.finalcoursework.dataBase.Record;
import com.finalcoursework.databinding.ActivityTrackBinding;
import com.finalcoursework.service.ICallBack;
import com.finalcoursework.service.TrackingService;
import com.finalcoursework.ui.PermissionCheckingActivity;
import com.finalcoursework.ui.PreviewActivity;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class TrackingUtility {
    //observe the live data
    public void observeData(ActivityTrackBinding binding,Context context, LifecycleOwner owner, Activity activity) {
        binding.getDatabaseViewModel().records.observe(owner, records -> {
            Log.d("TAG", "observeData: pbserved");
            //if there is no record and the service has not been started yet
            if (records.isEmpty() && binding.getTrackingBinderInterface() == null) {
                Log.d("TAG", "observeData: inside========");
                binding.startRecording.setVisibility(View.VISIBLE);
                return;
            }
            if (records.isEmpty())
                return;
            Record lastRecord = records.get(0);
            //if there is a record that was recorded today and service has not been started yet,
            // then load the record
            if (binding.getTrackingBinderInterface() == null && isToday(lastRecord)) {
                Log.d("TAG", "observeData: set Data");
                binding.setLastRecord(lastRecord);
                Long temp = lastRecord.getTimeSpent();
                Long hour = TimeUnit.MILLISECONDS.toHours(temp);
                temp -= TimeUnit.HOURS.toMillis(hour);
                Long minute = TimeUnit.MILLISECONDS.toMinutes(temp);
                temp -= TimeUnit.MINUTES.toMillis(minute);
                Long second = TimeUnit.MILLISECONDS.toSeconds(temp);
                binding.displayTime.setText(activity.getString(R.string.displayTime, hour, minute, second));
                Log.d("TAG", activity.getString(R.string.displayTime, hour, minute, second));
                binding.displayDistance.setText(activity.getString(R.string.displayDistance, lastRecord.getDistance()));
                binding.setPreSetString(lastRecord.getNote());
                binding.startRecording.setVisibility(View.VISIBLE);
                Toast.makeText(context, "today's record has been loaded.", Toast.LENGTH_LONG).show();
                return;
            }
            //if the service has been started and the last record is today's, it mean user backed
            // to the activity, so set the last record to make sure everything works fine
            if (binding.getTrackingBinderInterface() != null && isToday(lastRecord))
                binding.setLastRecord(lastRecord);

            //if there is a record but it was a previous recorded one and the service has not been
            //started yet
            // then do nothing when the service is initialized
            if (binding.getTrackingBinderInterface() == null)
                binding.startRecording.setVisibility(View.VISIBLE);
        });
    }

    //when service is connected
    public ServiceConnection connectionProvider(Activity activity, ActivityTrackBinding binding, Context context) {
        return new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                binding.setTrackingBinderInterface(((TrackingService.TrackingBinder) iBinder));
                //if the last record is today, reset all the parameter in the service
                if (binding.getLastRecord() != null &&
                        binding.getTrackingBinderInterface().isFirstTime()) {
                    binding.toggle.performClick();
                    binding.getTrackingBinderInterface().setParameters(binding.getLastRecord());
                    binding.toggle.performClick();
                }
                binding.getTrackingBinderInterface().setIsConnected(true);
                binding.getTrackingBinderInterface().registerCallback(callBack);
                //draw all the paths on to the map
                drawAllPaths(binding.getTrackingBinderInterface().getPaths(), binding);
                Log.d("tag", "connected");
                //if the service is tracking then we don't need to move camera at the activity side
                if (binding.getTrackingBinderInterface().isTracking())
                    stopTrackingCurrentPosition(binding);
                else {
                    //if the service is not tracking, then ask for the current distance and time
                    //to display, because the call back methods are not called when the service is
                    //paused
                    binding.displayDistance.setText(binding.getTrackingBinderInterface().getCurrentDistance());
                    binding.displayTime.setText(binding.getTrackingBinderInterface().getCurrentTime());
                }
                //if the service has started and tracked some paths, based on the current state,
                //change the visibility of buttons
                if (!binding.getTrackingBinderInterface().getPaths().isEmpty()) {
                    binding.startRecording.setVisibility(View.INVISIBLE);
                    binding.toggle.setVisibility(View.VISIBLE);
                    binding.stop.setVisibility(View.VISIBLE);
                    if (!binding.getTrackingBinderInterface().isTracking())
                        binding.toggle.setText(R.string.ResumeRecording);
                }
                //I saved the state whether the alter dialog is showing inside the service,
                //because the dialog will then be called and displayed when the map is prepared to
                //get a snapshot to make sure the data will be saved into the database correctly.
                //the service should always be there unless user pressed those stop buttons. So
                //it also make sense in this way.
                //if the dialog was altering, altering again because this is new activity
                if (binding.getTrackingBinderInterface().isAltering())
                    alterSaveDialog(binding, context, activity,
                            binding.getTrackingBinderInterface().getAnnotation());
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                Log.d("tag", "onServiceDisconnected: disconnected");
                binding.getTrackingBinderInterface().setIsConnected(false);
                binding.getTrackingBinderInterface().unregisterCallback();
                binding.setTrackingBinderInterface(null);
            }

            final ICallBack callBack = new ICallBack() {
                //draw individual paths on to the map
                @Override
                public void pathsChanged(ArrayList<LatLng> lastPath) {
                    activity.runOnUiThread(() -> drawPathAndMoveCamera(lastPath, binding));
                }
                //display how long user has been recorded
                @Override
                public void timeChanged(String result) {
                    activity.runOnUiThread(() -> binding.displayTime.setText(result));
                }
                //display how far user has been recorded
                @Override
                public void distanceChanged(String result) {
                    activity.runOnUiThread(() -> binding.displayDistance.setText(result));
                }
                //when button is pressed from the notification
                @Override
                public void stateChanged(int state) {
                    activity.runOnUiThread(() -> {
                        if (state == STOP)
                            binding.stop.performClick();
                        else
                            binding.toggle.performClick();
                    });
                }
            };
        };
    }

    //change camera location locally, tracing where user is.
    public LocationCallback locationCallbackProvider(Activity activity, ActivityTrackBinding binding, Context context) {
        return new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                changeCameraPositionWithoutService(locationResult.getLastLocation(), activity, binding, context);
            }
        };
    }

    //return value indicate if a type of permission is lacking, parameter indicates whether the permission
    //type is background location or fine location. If the permission is lacking,
    public Boolean isPermissionLacking(Boolean isFineLocation, Context context, Activity activity) {
        if (isFineLocation && ContextCompat.checkSelfPermission(context,
                FINE_LOCATION_PERMISSION_STRING) != PackageManager.PERMISSION_GRANTED) {
            Intent PermissionActivityIntent = new Intent(context, PermissionCheckingActivity.class);
            activity.startActivity(PermissionActivityIntent);
            return true;
        }
        if (!isFineLocation && ContextCompat.checkSelfPermission(context,
                BACKGROUND_LOCATION_PERMISSION_STRING) != PackageManager.PERMISSION_GRANTED) {
            Log.d("tag", "isPermissionLacking: background ungranted");
            Toast.makeText(context, "Background location permission have not been granted", Toast.LENGTH_LONG).show();
            Intent PermissionActivityIntent = new Intent(context, PermissionCheckingActivity.class);
            PermissionActivityIntent.putExtra("background", true);
            activity.startActivity(PermissionActivityIntent);
            return true;
        }
        //permission are granted
        return false;
    }

    //when the start recording button is pressed, start the service
    public void onClickStart(Activity activity, ActivityTrackBinding binding, Context context) {
        if (!isPermissionLacking(false, context, activity)) {
            startService(context, activity, binding);
            stopTrackingCurrentPosition(binding);
            binding.startRecording.setVisibility(View.INVISIBLE);
            binding.toggle.setVisibility(View.VISIBLE);
            binding.stop.setVisibility(View.VISIBLE);
        }
    }

    //if the service is currently running, pause it and stop tracking current position
    // (because service will handle that),
    // if it is currently paused then resume it and start tracking current position.
    public void onClickToggle(Activity activity, ActivityTrackBinding binding, Context context) {
        if (binding.toggle.getText().toString().equals(activity.getResources().getString(R.string.PauseRecording))) {
            binding.getTrackingBinderInterface().pauseService();
            startTrackingCurrentPosition(binding, activity, context);
            binding.toggle.setText(R.string.ResumeRecording);
        } else {
            binding.getTrackingBinderInterface().resumeService();
            stopTrackingCurrentPosition(binding);
            binding.toggle.setText(R.string.PauseRecording);
        }
    }

    //start tracking the current position in the activity if the permission is allowed.
    // If the permission is not allowed, then go to the permission activity and finish the current one.
    public void startTrackingCurrentPosition(ActivityTrackBinding binding, Activity activity, Context context) {
        try {
            binding.getFusedLocationClient().requestLocationUpdates(new LocationRequest.Builder(
                            Priority.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL_IN_MILLISECOND).build(),
                    binding.getLocationCallBack(),
                    Looper.getMainLooper());
        } catch (SecurityException e) {
            activity.startActivity(new Intent(context, PermissionCheckingActivity.class));
            activity.finish();
        }
    }

    //when stop button is pressed show the alter window
    public void onClickStop(ActivityTrackBinding binding, Context context, Activity activity) {
        binding.getTrackingBinderInterface().pauseService();
        startTrackingCurrentPosition(binding, activity, context);
        binding.toggle.setText(R.string.ResumeRecording);
        Log.d("tag", "onCreate: stop pressed");
        ArrayList<ArrayList<LatLng>> paths = binding.getTrackingBinderInterface().getPaths();
        //if there is a path that has been recorded
        if (!paths.get(paths.size() - 1).isEmpty()) {
            alterSaveDialog(binding, context, activity, binding.getPreSetString());
            binding.getTrackingBinderInterface().setIsAltering(true);
        } else {
            //if nothing was recorded, then just unbind and destroy the service
            binding.getTrackingBinderInterface().stopService();
            activity.unbindService(binding.getServiceConnection());
            binding.setIsBound(false);
            startTrackingCurrentPosition(binding, activity, context);
            binding.startRecording.setVisibility(View.VISIBLE);
            binding.toggle.setVisibility(View.INVISIBLE);
            binding.stop.setVisibility(View.INVISIBLE);
            Toast.makeText(context, "Your Action is too quick, please try again", Toast.LENGTH_LONG).show();
        }
    }

    //alter the saving dialog
    public void alterSaveDialog(ActivityTrackBinding binding, Context context, Activity activity, String string) {
        AlertDialog alertDialog = new AlertDialog.Builder(context).create();
        final EditText noteInput = new EditText(context);
        //save the information in case something happens, e.g. configuration change.
        noteInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                binding.getTrackingBinderInterface().setAnnotation(charSequence.toString());
            }
        });
        noteInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_AUTO_CORRECT);
        if (string != null) noteInput.setText(string);
        else noteInput.setText(R.string.annotation);
        alertDialog.setView(noteInput);
        alertDialog.setTitle("Save record?");
        //Unbind and stop the service,if user want to save the record,
        // then go back to preview activity (although there is no
        // one), because we want user to check their new record
        alertDialog.setMessage("Do you want to save the current record and go back?");
        alertDialog.setIcon(R.drawable.ic_baseline_save_24);
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Yes", (dialog, which) -> {
            zoomForRecord(binding.getTrackingBinderInterface().getPaths(), binding);
            saveRecordToDataBase(binding, context,
                    noteInput.getText().toString().equals(activity.getResources().getString(R.string.annotation))
                            ? "No annotation" : noteInput.getText().toString());
            binding.getTrackingBinderInterface().stopService();
            activity.unbindService(binding.getServiceConnection());
            binding.setIsBound(false);
            activity.startActivity(new Intent(activity, PreviewActivity.class));
            activity.finish();
        });
        //not save, unbind and destroy the service and finish the current activity, because no new record needs
        //to be shown to the user
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Don't save and quit", (dialog, which) -> {
            binding.getTrackingBinderInterface().stopService();
            activity.unbindService(binding.getServiceConnection());
            binding.setIsBound(false);
            activity.finish();
        });
        //if this is cancelled, it means user accidentally pressed the button, then do nothing (
        // service has already been paused).
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel", (dialog, which) ->
                binding.getTrackingBinderInterface().setIsAltering(false));
        binding.setSaveDialog(alertDialog);
        alertDialog.show();
    }

    //updating the current position inside the activity, let the map zoom and move
    // to the current position of the user
    private void changeCameraPositionWithoutService(Location newLocation, Activity activity, ActivityTrackBinding binding, Context context) {
        if (isPermissionLacking(true, context, activity))
            activity.finish();
        if (binding.getMyMap() != null) {
            binding.getMyMap().animateCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(newLocation.getLatitude(), newLocation.getLongitude()),
                    ZOOMING_CONSTANT));
        }
    }

    //for all the paths, draw it on the map as red lines, when user finish their record, these
    //red lines will indicates where user travelled via google map snapshot.
    private void drawAllPaths(ArrayList<ArrayList<LatLng>> paths, ActivityTrackBinding binding) {
        for (ArrayList<LatLng> path : paths)
            if (binding.getMyMap() != null)
                binding.getMyMap().addPolyline(new PolylineOptions()
                        .color(PATH_COLOR)
                        .width(PATH_WIDTH)
                        .addAll(path));
    }

    //stop tracking the current position since the service is starting to tracking it
    private void stopTrackingCurrentPosition(ActivityTrackBinding binding) {
        binding.getFusedLocationClient().removeLocationUpdates(binding.getLocationCallBack());
    }

    //based on the last path, move the camera to the last location and draw the path on the map
    private void drawPathAndMoveCamera(ArrayList<LatLng> lastPath, ActivityTrackBinding binding) {
        //if there is a path and the last path has at least two locations
        if (lastPath != null && lastPath.size() > 1) {
            LatLng startPointOfThePath = lastPath.get(lastPath.size() - 2);
            LatLng endPointOfThePath = lastPath.get(lastPath.size() - 1);
            if (binding.getMyMap() != null) {
                binding.getMyMap().addPolyline(new PolylineOptions()
                        .color(PATH_COLOR)
                        .width(PATH_WIDTH)
                        .add(startPointOfThePath)
                        .add(endPointOfThePath));
                binding.getMyMap().animateCamera(CameraUpdateFactory.newLatLngZoom(
                        endPointOfThePath,
                        ZOOMING_CONSTANT));
            }
        }
    }

    //start the service as a foreground service
    private void startService(Context context, Activity activity, ActivityTrackBinding binding) {
        Intent serviceIntent = new Intent(context, TrackingService.class);
        serviceIntent.setAction("startRecording");
        activity.startForegroundService(serviceIntent);
        binding.setIsBound(activity.bindService(serviceIntent, binding.getServiceConnection(), BIND_AUTO_CREATE));
    }

    //move and zoom the camera for snapshot of the new record, and this will make sure all the paths
    //are contained in the snapshot.
    private void zoomForRecord(ArrayList<ArrayList<LatLng>> paths, ActivityTrackBinding binding) {
        LatLngBounds.Builder bounds = new LatLngBounds.Builder();
        for (ArrayList<LatLng> path : paths)
            for (LatLng joint : path)
                bounds.include(joint);
        if (binding.getMyMap() != null) {
            binding.getMyMap().moveCamera(CameraUpdateFactory.newLatLngBounds(
                    bounds.build(),
                    binding.mapView.getWidth(),
                    binding.mapView.getHeight(),
                    (int) (binding.mapView.getHeight() * PADDING_RATIO)
            ));
        }
    }

    //query view model to save the pre-recorded data
    private void saveRecordToDataBase(ActivityTrackBinding binding, Context context, String note) {
        //if it is updating today's record, then query the update function
        if (binding.getMyMap() != null && binding.getTrackingBinderInterface().isSet()) {
            binding.getMyMap().snapshot(snapShot -> {
                int distance = binding.getTrackingBinderInterface().getDistance();
                //get only 1 decimal place, so that when the data is rendered it would be too long
                Long timeSpent = binding.getTrackingBinderInterface().getTimeSpent();
                float averageSpeed = (distance / 1000f) / (timeSpent / 1000f / 60 / 60);
                ArrayList<ArrayList<LatLng>> paths = binding.getTrackingBinderInterface().getPaths();
                binding.getDatabaseViewModel().updateAll(snapShot, averageSpeed, distance, timeSpent, note, paths,
                        binding.getLastRecord().getId());
                Intent backToPreview = new Intent(context, PreviewActivity.class);
                context.startActivity(backToPreview);
                Toast.makeText(context, "today's record has been updated", Toast.LENGTH_LONG).show();
            });
            return;
        }
        //if it is a new record, query the insert function
        if (binding.getMyMap() != null) {
            binding.getMyMap().snapshot(snapShot -> {
                int distance = binding.getTrackingBinderInterface().getDistance();
                //get only 1 decimal place, so that when the data is rendered it would be too long
                Long timeSpent = binding.getTrackingBinderInterface().getTimeSpent();
                float averageSpeed = (distance / 1000f) / (timeSpent / 1000f / 60 / 60);
                Long date = binding.getTrackingBinderInterface().getStartTime();
                Record record = new Record(snapShot, date, averageSpeed, distance, timeSpent, note,
                        binding.getTrackingBinderInterface().getPaths(), null);
                binding.getDatabaseViewModel().insert(record);
                Toast.makeText(context, "New record has been added", Toast.LENGTH_LONG).show();
                Intent backToPreview = new Intent(context, PreviewActivity.class);
                context.startActivity(backToPreview);
            });
        }
        //if the map element is not initialized
        else
            Toast.makeText(context, "Failed", Toast.LENGTH_LONG).show();
    }

    //check if the last record is today's record
    private boolean isToday(Record record) {
        Calendar today = Calendar.getInstance();
        Calendar lastRecord = Calendar.getInstance();
        today.setTimeInMillis(System.currentTimeMillis());
        lastRecord.setTimeInMillis(record.getDate());
        return today.get(Calendar.DAY_OF_YEAR) == lastRecord.get(Calendar.DAY_OF_YEAR) &&
                today.get(Calendar.YEAR) == lastRecord.get(Calendar.YEAR);
    }
}
