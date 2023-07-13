package com.finalcoursework.ui;

import static com.finalcoursework.helpers.Constants.BACKGROUND_LOCATION_REQUEST_CODE;
import static com.finalcoursework.helpers.Constants.COARSE_LOCATION_PERMISSION_STRING;
import static com.finalcoursework.helpers.Constants.FINE_LOCATION_PERMISSION_STRING;
import static com.finalcoursework.helpers.Constants.LOCATION_REQUEST_CODE;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import android.content.pm.PackageManager;

import com.finalcoursework.R;
import com.finalcoursework.databinding.ActivityPermissionBinding;
import com.finalcoursework.helpers.PermissionUtility;

import android.os.Bundle;
import android.util.Log;
import android.view.View;


public class PermissionCheckingActivity extends AppCompatActivity {
    private ActivityPermissionBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //initiate data binding object and set the content view
        binding =  DataBindingUtil.setContentView(this, R.layout.activity_permission);
        //check if this we want to check the background location permission
        binding.setIsBackground(false);
        //initialize handler to handle all actions
        binding.setPermissionHandler(new PermissionUtility());
        //initialize button actions
        binding.GoToSetting.setOnClickListener(view->binding.getPermissionHandler().goToSetting(this, binding));
        binding.permissionButton.setOnClickListener((view -> checkLocationPermission()));
        //change our text according to the permission type
        if (getIntent().getExtras() != null){
            binding.setIsBackground(getIntent().getExtras().getBoolean("background"));
            binding.permissionButton.setText(R.string.BackGroundPermission);
            binding.PermissionWarning.setText(R.string.BackgroundPermissionWarning);
            //when user cancel the rational dialog, they have the idea how to allow the permission
            binding.PermissionWarning.setVisibility(View.VISIBLE);
        }
        //then check permission
        checkLocationPermission();
    }

    //check if permission is allowed or denied
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_REQUEST_CODE) {// If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                    && (grantResults[0] == PackageManager.PERMISSION_GRANTED ||
                    grantResults[1] == PackageManager.PERMISSION_GRANTED)) {
                //use location
                binding.getPermissionHandler().goToTrackingActivity(PermissionCheckingActivity.this, this);
            } else {
                Log.d("g53mdp", "fine location permission denied");
                //requests have been denied even after the information providing
                //give user an opportunity to turn it on at the setting activity
                if (!binding.getPermissionHandler().shouldShowPermissionRationale(this, FINE_LOCATION_PERMISSION_STRING) &&
                        !binding.getPermissionHandler().shouldShowPermissionRationale(this,COARSE_LOCATION_PERMISSION_STRING)){
                    binding.PermissionWarning.setVisibility(View.VISIBLE);
                    binding.GoToSetting.setVisibility(View.VISIBLE);
                    binding.permissionButton.setVisibility(View.INVISIBLE);
                }
            }
        }else if(requestCode == BACKGROUND_LOCATION_REQUEST_CODE){
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                //use location and get back to previous activity
                finish();
        }
    }

    //if user come back from the setting activity, check the permission again
    @Override
    protected void onRestart() {
        super.onRestart();
        checkLocationPermission();
        Log.d("none", "onRestart: ");
    }
    //when the activity is destroyed, if the dialog is still there dismiss it
    @Override
    protected void onDestroy() {
        if (binding.getRationaleDialog() != null)
            binding.getRationaleDialog().dismiss();
        super.onDestroy();
    }

    private void checkLocationPermission() {
        binding.getPermissionHandler().checkLocationPermission(PermissionCheckingActivity.this,
                this,
                binding);
    }
}