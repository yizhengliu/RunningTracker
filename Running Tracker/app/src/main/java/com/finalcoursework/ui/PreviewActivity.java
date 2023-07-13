package com.finalcoursework.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.finalcoursework.R;
import com.finalcoursework.adapter.RecordAdapter;
import com.finalcoursework.databinding.ActivityPreviewBinding;
import com.finalcoursework.helpers.PreviewUtility;
import com.finalcoursework.service.TrackingService;
import com.finalcoursework.ui.viewModels.PreviewViewModel;


public class PreviewActivity extends AppCompatActivity {
    private ActivityPreviewBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_preview);
        binding.setRecordAdapter(new RecordAdapter(PreviewActivity.this, binding));
        binding.setHelper(new PreviewUtility());
        //check if the service is still there, if so, go to trackActivity directly
        if (binding.getHelper().isMyServiceRunning(TrackingService.class,this)){
            startActivity(new Intent(this, TrackActivity.class));
            finish();
        }
        //image picker for updating custom image
        binding.setImagePickerLauncher(binding.getHelper().getLauncher(this, PreviewActivity.this, binding));
        binding.recyclerView.setAdapter(binding.getRecordAdapter());
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(PreviewActivity.this));
        binding.recyclerView.setItemAnimator(new DefaultItemAnimator());
        binding.recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        binding.setViewModel(new ViewModelProvider(
                this,
                (ViewModelProvider.Factory) ViewModelProvider.AndroidViewModelFactory.getInstance(this.getApplication()))
                .get(PreviewViewModel.class));
        //when the sort type changed, reset adapter for rendering the correct sorted records
        binding.getViewModel().records.observe(this, records -> binding.getRecordAdapter().setData(records));
        binding.startTracking.setOnClickListener(view -> startActivity(new Intent(PreviewActivity.this, PermissionCheckingActivity.class)));
        binding.statistics.setOnClickListener(view -> startActivity(new Intent(PreviewActivity.this, StatisticsActivity.class)));
        binding.spinner.setSelection(binding.getViewModel().getCurrentType());
        //when a new sort type is selected
        binding.spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                binding.getViewModel().setCurrentType(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        //help button to show how to use the functionality
        binding.helperButton.setOnClickListener(view -> {
            if (binding.guide.getVisibility() == View.VISIBLE)
                binding.guide.setVisibility(View.INVISIBLE);
            else
                binding.guide.setVisibility(View.VISIBLE);
        });
        //add the gestures on to the recycler view, so that user can swipe to delete a record
        binding.getHelper().getItemTouchHelper(binding, PreviewActivity.this).attachToRecyclerView(binding.recyclerView);
    }

    //save whether the UI state
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt("isShowingGuide",binding.guide.getVisibility());
        outState.putInt("spinnerPos", binding.spinner.getSelectedItemPosition());
        super.onSaveInstanceState(outState);
    }
    //load the UI state
    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        binding.guide.setVisibility(savedInstanceState.getInt("isShowingGuide"));
        binding.spinner.setSelection(savedInstanceState.getInt("spinnerPos"));
        super.onRestoreInstanceState(savedInstanceState);
    }

}