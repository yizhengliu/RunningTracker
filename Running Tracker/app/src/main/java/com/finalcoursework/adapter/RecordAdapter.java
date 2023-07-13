package com.finalcoursework.adapter;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.finalcoursework.R;
import com.finalcoursework.dataBase.Record;
import com.finalcoursework.databinding.ActivityPreviewBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
//Recycler view adapter
public class RecordAdapter extends RecyclerView.Adapter<RecordAdapter.RecordViewHolder>{

    private List<Record> data;
    private final LayoutInflater layoutInflater;
    private final ActivityPreviewBinding binding;

    //initialize member variables
    public RecordAdapter(Context context, ActivityPreviewBinding binding){
        this.data = new ArrayList<>();
        this.layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.binding = binding;
    }

    //inflate and return the view holder
    @NonNull
    @Override
    public RecordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = layoutInflater.inflate(R.layout.record_view, parent, false);
        return new RecordViewHolder(itemView, binding);
    }

    //when bind, initialize the view detail of the layout
    @Override
    public void onBindViewHolder(@NonNull RecordViewHolder holder, int position) {
        holder.bind(data.get(position));
    }
    //return how many items in the recycler view
    @Override
    public int getItemCount() {
        return data.size();
    }

    //everytime the user choose the sort type, data will be reset and will be rendered on to the recycler view
    @SuppressLint("NotifyDataSetChanged")
    public void setData(List<Record> newData){
        if (data != null && newData != null) {
            data.clear();
            data.addAll(newData);
            notifyDataSetChanged();
        } else {
            data = newData;
        }
    }
    //get a record at pos
    public Record getDataAt(int pos) {
        return data.get(pos);
    }

    static class RecordViewHolder extends RecyclerView.ViewHolder{
        ImageView image;
        TextView date;
        TextView averageSpeed;
        TextView distance;
        TextView timeSpent;
        TextView note;
        TextView id;
        //initialize view components for further usage
        public RecordViewHolder(@NonNull View itemView, ActivityPreviewBinding binding) {
            super(itemView);

            image = itemView.findViewById(R.id.image);
            date = itemView.findViewById(R.id.date);
            averageSpeed = itemView.findViewById(R.id.averageSpeed);
            distance = itemView.findViewById(R.id.distance);
            timeSpent = itemView.findViewById(R.id.timeSpent);
            note = itemView.findViewById(R.id.note);
            id = itemView.findViewById(R.id.id);
            //if a record (view holder) is clicked, go to the image picker for user to choose a custom image
            //that will be attached to the record
            itemView.setOnClickListener(view -> {
                Log.d("TAG", "onClick: itemview");
                binding.setPosition(Integer.parseInt(
                        ((TextView)itemView.findViewById(R.id.id))
                                .getText().toString()));
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                binding.getImagePickerLauncher().launch(intent);
            });
        }

        //initialize the actually view based on the values stored in the database for each record
        //bind method is used to initialize the detail of the view in the view holder
        protected void bind(final Record record) {
            if (record != null) {
                image.setImageBitmap(combineBitmap(record.getImage(), record.getCustomImage()));
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(record.getDate());
                SimpleDateFormat dateFormat= new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                date.setText(String.format(Locale.getDefault(),"Start Time: %s", dateFormat.format(calendar.getTime())));
                averageSpeed.setText(String.format(Locale.getDefault(),"Average Speed: %.2f Km/h", record.getAverageSpeed()));
                distance.setText(String.format(Locale.getDefault(),"Distance Traveled: %.3f Km", record.getDistance() / 1000f));
                long temp = record.getTimeSpent();
                long hour = TimeUnit.MILLISECONDS.toHours(temp);
                temp -= TimeUnit.HOURS.toMillis(hour);
                long minute = TimeUnit.MILLISECONDS.toMinutes(temp);
                temp -= TimeUnit.MINUTES.toMillis(minute);
                long second = TimeUnit.MILLISECONDS.toSeconds(temp);
                timeSpent.setText(String.format(Locale.getDefault(),"Time spent: %dH %dM %dS",hour,minute,second));
                note.setText(String.format(Locale.getDefault(),"Note: %s",record.getNote()));
                id.setText(String.format(Locale.getDefault(),"%d",record.getId()));
            }
        }
        //combine two images, one is map snapshot, and another is user custom image, return a final
        //bitmap object that contains these two images.
        private Bitmap combineBitmap(Bitmap prior, Bitmap custom){
            if (custom == null){
                Log.d("TAG", "combineBitmap: null");
                return prior;
            }
            int width = Math.max(custom.getWidth(), prior.getWidth());
            int height = prior.getHeight() + custom.getHeight();
            Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(result);
            canvas.drawBitmap(prior,0f, 0f, null);
            canvas.drawBitmap(custom,0f, prior.getHeight(), null);
            return result;
        }
    }
}
