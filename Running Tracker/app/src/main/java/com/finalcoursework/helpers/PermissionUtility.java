package com.finalcoursework.helpers;

import static com.finalcoursework.helpers.Constants.BACKGROUND_LOCATION_PERMISSION_STRING;
import static com.finalcoursework.helpers.Constants.BACKGROUND_LOCATION_REQUEST_CODE;
import static com.finalcoursework.helpers.Constants.COARSE_LOCATION_PERMISSION_STRING;
import static com.finalcoursework.helpers.Constants.FINE_LOCATION_PERMISSION_STRING;
import static com.finalcoursework.helpers.Constants.LOCATION_REQUEST_CODE;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.finalcoursework.BuildConfig;
import com.finalcoursework.R;
import com.finalcoursework.databinding.ActivityPermissionBinding;
import com.finalcoursework.ui.TrackActivity;

//this class is a handler to check the permission of the application using the permission activity
public class PermissionUtility {
    //check the permission, if granted, then go to next step, if user denied the permission, show
    //rational to inform why permissions are needed, and it gives user a chance to go to the setting
    //page if they denied accidentally even after the rational has shown.
    public void checkLocationPermission(Context context, Activity activity, ActivityPermissionBinding binding) {
        if (!binding.getIsBackground()) {
            String[] permissions = new String[]{FINE_LOCATION_PERMISSION_STRING,
                    COARSE_LOCATION_PERMISSION_STRING};
            if (isGranted(context, FINE_LOCATION_PERMISSION_STRING) &&
                    isGranted(context, COARSE_LOCATION_PERMISSION_STRING)) {
                //if the permissions are granted then use them
                goToTrackingActivity(context, activity);
            } else if (shouldShowPermissionRationale(activity, FINE_LOCATION_PERMISSION_STRING) ||
                    shouldShowPermissionRationale(activity, COARSE_LOCATION_PERMISSION_STRING)) {
                //if user has denied the permission show dialog to explain why
                alterDialog(permissions, LOCATION_REQUEST_CODE, context, activity, binding);
            } else {
                //permission has not been asked yet
                requestLocationPermission(permissions, LOCATION_REQUEST_CODE, activity);
            }
        } else {
            String[] permission = new String[]{BACKGROUND_LOCATION_PERMISSION_STRING};
            if (isGranted(context, BACKGROUND_LOCATION_PERMISSION_STRING)) {
                //if the permission is granted then use back to the previous activity
                activity.finish();
            } else if (shouldShowPermissionRationale(activity, BACKGROUND_LOCATION_PERMISSION_STRING)) {
                //if user has denied the permission show dialog to explain why
                alterDialog(permission, BACKGROUND_LOCATION_REQUEST_CODE, context, activity, binding);
            } else {
                //permission has not been asked yet
                requestLocationPermission(permission, BACKGROUND_LOCATION_REQUEST_CODE, activity);
            }
        }
    }

    //if fine location permissions have been granted, go to the tracking activity
    public void goToTrackingActivity(Context context, Activity activity) {
        Log.d("g53mdp", "fine location permission granted");
        Intent intent = new Intent(context, TrackActivity.class);
        activity.startActivity(intent);
        activity.finish();
    }

    //check if a type of permission should show rationale
    public boolean shouldShowPermissionRationale(Activity activity, String permissionType) {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permissionType);
    }

    //go to the setting activity for user to enable permissions
    public void goToSetting(Activity activity, ActivityPermissionBinding binding) {
        activity.startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + BuildConfig.APPLICATION_ID)));
        if (!binding.getIsBackground()) {
            binding.PermissionWarning.setVisibility(View.INVISIBLE);
            binding.GoToSetting.setVisibility(View.INVISIBLE);
            binding.permissionButton.setVisibility(View.VISIBLE);
        }
    }

    //actually request the permission with permission code at run time
    private void requestLocationPermission(String[] permissionTypes, int permissionCode, Activity activity) {
        ActivityCompat.requestPermissions(activity, permissionTypes, permissionCode);
    }

    //alter an dialog to show why permission is needed
    private void alterDialog(String[] permissionType, int permissionCode, Context context, Activity activity, ActivityPermissionBinding binding) {
        AlertDialog alertDialog = new AlertDialog.Builder(context).create();
        alertDialog.setTitle("Permission needed");
        if (permissionCode == LOCATION_REQUEST_CODE) {
            alertDialog.setMessage("Location is needed in order to tracking your current position");
            //request permission at run time
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Ok", (dialog, which) -> requestLocationPermission(permissionType, permissionCode, activity));
        } else {
            alertDialog.setMessage("Background location tracking is needed in order to track your location" +
                    " when this application is not foreground. To allow background location, please go to the \"permission\" section and select the \"allow all the time\" option");
            //go to setting directly, there is a bug when using ActivityCompat.requestPermissions() for
            //background location permission, so provide the instructions and let user go to the setting menu directly.
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Ok", (dialog, which) -> goToSetting(activity, binding));
            //Here is the formal way, but with appearance bug, this only works for the first time

            //alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Ok",
            //        (dialog, which) -> requestLocationPermission(permissionType, permissionCode));
        }
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel", (dialog, which) -> {
        });
        alertDialog.setIcon(R.drawable.ic_baseline_security_24);
        binding.setRationaleDialog(alertDialog);
        alertDialog.show();
    }

    //check if a type of permission is granted
    private boolean isGranted(Context context, String permissionType) {
        return ContextCompat.checkSelfPermission(context, permissionType) == PackageManager.PERMISSION_GRANTED;
    }

}
