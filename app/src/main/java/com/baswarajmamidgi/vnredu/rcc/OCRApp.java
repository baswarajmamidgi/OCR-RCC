package com.baswarajmamidgi.vnredu.rcc;

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
        String path = Environment.getExternalStorageDirectory().toString()+"/rcc/images";
        File directory=new File(path);
        if(directory.exists()) {
            File[] files = directory.listFiles();
            if (files.length > 0) {
                for (int i = 0; i < files.length; i++) {
                    Record model = new Record();
                    DateTime dateTime = new DateTime();
                    dateTime = dateTime.minusDays(r.nextInt(30));
                    model.dateTime = dateTime.toDate();
                    model.label = files[i].getName();
                    model.pathToImage=files[i].getAbsolutePath();

                    demoData.add(model);
                    demoMap.put(model.id, model);
                }
            }
        }


    }

    public static final List<Record> getData() {
        return new ArrayList<Record>(demoData);
    }



    public static final List<Record> removeItemFromList(int position) {
        demoData.remove(position);
        demoMap.remove(demoData.get(position).id);
        return new ArrayList<Record>(demoData);
    }


}
