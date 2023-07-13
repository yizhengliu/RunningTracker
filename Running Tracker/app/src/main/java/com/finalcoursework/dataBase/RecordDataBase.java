package com.finalcoursework.dataBase;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.finalcoursework.R;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

//the database allows exporting for providing content. it has a record table and a type converter
@Database(entities = {Record.class}, version = 3, exportSchema = true)
@TypeConverters(Converters.class)
public abstract class RecordDataBase extends RoomDatabase {
    //abstraction of SQL instructions
    public abstract RecordDAO getRecordDAO();
    //set number to 10 to insure it handles all the query.
    public static final ExecutorService databaseWriteExecutor = Executors.newFixedThreadPool(10);
    //singleton, because we only want one database at a time for an application
    private static volatile RecordDataBase INSTANCE;
    //keep a static reference of initial image rather than the context reference to prevent
    //memory leakage
    private static Bitmap initialImage;

    public static RecordDataBase getDatabase(final Context context){
        if (INSTANCE == null){
            synchronized (RecordDataBase.class){
                INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                        RecordDataBase.class, "record_database")
                        .fallbackToDestructiveMigration()
                        .addCallback(createCallback)
                        .build();
                initialImage = BitmapFactory.decodeResource(context.getResources(),
                        R.drawable.download);
            }
        }
        return INSTANCE;
    }

    //create the database with an element in it. It will be easier for you check the spinner functionality, the reason
    //for this is because my app are used to track record in each day, say if today already has a record, then
    //I will update it based on the existing one. Therefore you may need to test my app using two days.
    //Here I provide a record with a different date, for you to check the functionality easier :)
    private static final RoomDatabase.Callback createCallback = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
            //practically this will be deleted, because we only want user to have their own data
            databaseWriteExecutor.execute(()->{
                RecordDAO dao = INSTANCE.getRecordDAO();
                //a random date but is before the initialize date
                Long date = System.currentTimeMillis()-3*7*24*60*60*1000;
                Record record = new Record(initialImage,date,9.4848F,94,
                        1000L * 60 ,"Fake record, only for checking spinner functionality",null,null);
                dao.insert(record);
            });
            //=======================================================================delete above
        }
    };
}
