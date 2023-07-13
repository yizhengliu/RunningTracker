package com.finalcoursework.helpers;

import static android.app.Activity.RESULT_OK;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.finalcoursework.R;
import com.finalcoursework.databinding.ActivityPreviewBinding;
import com.finalcoursework.ui.PreviewActivity;

import java.io.IOException;
//handler class for preview activity binding
public class PreviewUtility {
    //return a item touch helper, which allows the user to swipe a record view to delete it
    public ItemTouchHelper getItemTouchHelper(ActivityPreviewBinding binding, Context context){
        return new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT|ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                alterDeleteDialog(binding, context, viewHolder);
              }
        });
    }
    //alter an dialog to ensure user really want to delete a record or accidentally swiped
    private void alterDeleteDialog(ActivityPreviewBinding binding, Context context, RecyclerView.ViewHolder viewHolder){
        AlertDialog alertDialog = new AlertDialog.Builder(context).create();
        alertDialog.setTitle("Delete record");
        alertDialog.setMessage("Do you delete the current record?");
        alertDialog.setIcon(R.drawable.ic_baseline_delete_24);
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Yes", (dialog, which) -> {
            binding.getViewModel().delete(binding.getRecordAdapter().getDataAt(viewHolder.getAdapterPosition()));
            Toast.makeText(context, "deleted successfully", Toast.LENGTH_LONG).show();
        });

        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel", (dialog, which) ->
                binding.getRecordAdapter().notifyItemChanged(viewHolder.getAdapterPosition()));
        alertDialog.show();
    }
    //activity result launcher that will take the user to their own gallery to let them choose a picture to attach on a record
    public ActivityResultLauncher<Intent> getLauncher(PreviewActivity activity, Context context, ActivityPreviewBinding binding){
        return activity.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null){
                        Uri uri = result.getData().getData();
                        Bitmap bitmap = null;
                        ImageDecoder.Source source = ImageDecoder.createSource(activity.getContentResolver(), uri);
                        try {
                            bitmap = ImageDecoder.decodeBitmap(source);
                        } catch (IOException e) {
                            Toast.makeText(context, "Failed to load the picture", Toast.LENGTH_LONG).show();
                        }
                        if (bitmap != null){
                            binding.getViewModel().updateCus(bitmap, binding.getPosition());
                            Toast.makeText(context, "Image uploaded", Toast.LENGTH_LONG).show();
                        }
                    }else
                        Toast.makeText(context, "Cancelled", Toast.LENGTH_LONG).show();
                });
    }

    public boolean isMyServiceRunning(Class<?> serviceClass, Activity activity) {
        ActivityManager manager = (ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
