package com.finalcoursework.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import com.finalcoursework.R;
import com.finalcoursework.databinding.ActivityTrackBinding;
import com.finalcoursework.helpers.TrackingUtility;
import com.finalcoursework.service.TrackingService;
import com.finalcoursework.ui.viewModels.TrackingViewModel;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.MapsInitializer;

public class TrackActivity extends AppCompatActivity {
    private ActivityTrackBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //to avoid security exception bug
        MapsInitializer.initialize(getApplicationContext(), MapsInitializer.Renderer.LATEST, (renderer)->{});
        //initialization of the content view
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_track);
        //initialization of the map and other components
        binding.setRecordingHandler(new TrackingUtility());
        binding.mapView.onCreate(savedInstanceState);
        Log.d("tag", "creating");
        binding.setLastRecord(null);
        binding.setIsBacked(false);
        binding.setPreSetString(null);
        binding.setFusedLocationClient(LocationServices.getFusedLocationProviderClient(TrackActivity.this));
        binding.mapView.getMapAsync((theMap) -> {
            //initialize the map and let it show the current position
            try {
                theMap.setMyLocationEnabled(true);
            } catch (SecurityException e) {
                //lack of permission
                startActivity(new Intent(TrackActivity.this, PermissionCheckingActivity.class));
                TrackActivity.this.finish();
            }
            binding.setMyMap(theMap);});
        binding.setDatabaseViewModel(new ViewModelProvider(this,
                (ViewModelProvider.Factory) ViewModelProvider.AndroidViewModelFactory.getInstance(this.getApplication())).get(TrackingViewModel.class));
        binding.setServiceConnection(binding.getRecordingHandler().connectionProvider(this, binding, TrackActivity.this));
        //everytime the location is received, camera will be directed to the last location.
        //There is a message spamming bug in the Logcat (might occur) with this call back and it is
        // the api problem that have not been solved. However, it seems does not affect the
        // performance of the application.
        //spamming message: exceeded sample count in FrameTime
        binding.setLocationCallBack(binding.getRecordingHandler().locationCallbackProvider(this,binding,TrackActivity.this));
        //start the recording when button is clicked, check permission before hand
        binding.startRecording.setOnClickListener(view -> binding.getRecordingHandler().onClickStart(this, binding, TrackActivity.this));
        //toggle button for resuming and pausing the service, if pause then only track the current position
        //of the user, if resume then draw the new path on the map.
        binding.toggle.setOnClickListener(view-> binding.getRecordingHandler().onClickToggle(this, binding, TrackActivity.this));
        binding.stop.setOnClickListener(view -> binding.getRecordingHandler().onClickStop(binding, TrackActivity.this, this));
        //bind to the service in case the service is still there even this activity was already gone
        Intent serviceIntent = new Intent(TrackActivity.this, TrackingService.class);
        binding.setIsBound(bindService(serviceIntent, binding.getServiceConnection(), 0));
        binding.getRecordingHandler().observeData(binding,TrackActivity.this , this,this);
        binding.getRecordingHandler().startTrackingCurrentPosition(binding, this, TrackActivity.this);
    }

    @Override
    protected void onDestroy() {
        Log.d("TAG", "onDestroy: destoryed");
        binding.mapView.onDestroy();
        //unbind service manually in case onConnectionDisconnected not called and dismiss the
        //dialog if it is still there
        if (binding.getTrackingBinderInterface() != null){
            if (binding.getTrackingBinderInterface().isAltering())
                binding.getSaveDialog().dismiss();
            binding.getTrackingBinderInterface().activityDestroyed();
        }
        if (binding.getServiceConnection() != null && binding.getIsBound()) {
            unbindService(binding.getServiceConnection());
            Log.d("TAG", "onDestroy: unbinded");
            binding.setServiceConnection(null);
        }
        super.onDestroy();
    }

    //managing the life cycle of the google map view manually
    @Override
    protected void onResume() {
        super.onResume();
        binding.mapView.onResume();
        //check the permission
        if (binding.getIsBacked()) {
            if (binding.getRecordingHandler().isPermissionLacking(true, TrackActivity.this, this))
                finish();
            binding.setIsBacked(false);
        }
     }

    @Override
    protected void onStart() {
        Log.d("tag", "onStart: ");
        super.onStart();
        binding.mapView.onStart();
    }

    @Override
    protected void onStop() {
        Log.d("tag", "onStop: ");
        super.onStop();
        binding.mapView.onStop();
    }

    @Override
    protected void onPause() {
        Log.d("tag", "onPause: ");
        super.onPause();
        binding.mapView.onPause();
        binding.setIsBacked(true);
    }

    @Override
    public void onLowMemory() {
        Log.d("tag", "onLowMemory: ");
        super.onLowMemory();
        binding.mapView.onLowMemory();
    }
}