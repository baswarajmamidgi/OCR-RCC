package com.baswarajmamidgi.vnredu.threeRReader;

import android.app.Application;
import android.os.Environment;
import android.util.SparseArray;

import net.danlew.android.joda.JodaTimeAndroid;

import org.joda.time.DateTime;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class OCRApp extends Application {

    private static List<Record> demoData;
    private static SparseArray<Record> demoMap;

    @Override
    public void onCreate() {
        super.onCreate();
        JodaTimeAndroid.init(this);
        Random r = new Random();
        demoData = new ArrayList<Record>();
        demoMap = new SparseArray<Record>();
        File directory = new File(Environment.getExternalStorageDirectory(), "3RReader/images");

        if(directory.exists()) {
            File[] files = directory.listFiles();
            try {


                if (files.length > 0) {
                    for (int i = 0; i < files.length; i++) {
                        Record model = new Record();
                        DateTime dateTime = new DateTime();
                        dateTime = dateTime.minusDays(r.nextInt(30));
                        model.dateTime = dateTime.toDate();
                        model.label = files[i].getName();
                        model.pathToImage = files[i].getAbsolutePath();

                        demoData.add(model);
                        demoMap.put(model.id, model);
                    }
                }
            }catch (Exception e)
            {
                e.getLocalizedMessage();
            }
        }


    }

    public static final List<Record> getData() {
        return new ArrayList<Record>(demoData);
    }



    public static final List<Record> removeItemFromList(int position) {
        demoMap.remove(demoData.get(position).id);
        demoData.remove(position);

        return new ArrayList<Record>(demoData);
    }


}
