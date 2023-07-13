package com.finalcoursework.ui;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import com.finalcoursework.R;
import com.finalcoursework.databinding.ActivityStatisticsBinding;
import com.finalcoursework.helpers.StatisticsUtility;
import com.finalcoursework.ui.viewModels.StatisticsViewModel;


public class StatisticsActivity extends AppCompatActivity {
    ActivityStatisticsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_statistics);
        binding.setHelper(new StatisticsUtility());
        binding.setViewModel(new ViewModelProvider(this,
                (ViewModelProvider.Factory) ViewModelProvider.AndroidViewModelFactory
                        .getInstance(this.getApplication())).get(StatisticsViewModel.class));
        binding.getHelper().observeData(binding, StatisticsActivity.this, this);
        binding.getHelper().initiateBarChart(binding);
        //whether to show the help information
        binding.helperButton1.setOnClickListener(view -> {
            if (binding.guide1.getVisibility() == View.VISIBLE)
                binding.guide1.setVisibility(View.INVISIBLE);
            else
                binding.guide1.setVisibility(View.VISIBLE);
        });
    }
    //save UI state
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt("isShowing", binding.guide1.getVisibility());
        super.onSaveInstanceState(outState);
    }
    //load UI state
    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        binding.guide1.setVisibility(savedInstanceState.getInt("isShowing"));
        super.onRestoreInstanceState(savedInstanceState);
    }
}