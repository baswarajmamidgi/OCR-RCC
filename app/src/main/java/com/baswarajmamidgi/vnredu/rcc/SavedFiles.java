package com.baswarajmamidgi.vnredu.rcc;


import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

import static android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION;
import static android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION;


/**
 * A simple {@link Fragment} subclass.
 */
public class SavedFiles extends AppCompatActivity implements AbsListView.MultiChoiceModeListener {
   private ArrayList<String> offlinefiles=new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private ArrayList<String> toDelete=new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_savedfiles);
        Toolbar toolbar= (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("Saved Files");
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        ListView fileslist = (ListView) findViewById(R.id.saved_docs);
        fileslist.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        fileslist.setMultiChoiceModeListener(this);
        String path = Environment.getExternalStorageDirectory().toString()+"/rcc/pdf";
        File directory=new File(path);
        if(directory.exists()) {
            File[] files = directory.listFiles();
            if (files.length > 0) {
                for (int i = 0; i < files.length; i++) {
                    offlinefiles.add(files[i].getName());
                    Log.i("Files", "FileName:" + files[i].getName());
                }
            }
        }
        adapter=new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,offlinefiles);


        fileslist.setAdapter(adapter);

        fileslist.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                view(offlinefiles.get(position));

            }
        });




    }





    public void view(String filename)
    {
        File file=new File(Environment.getExternalStorageDirectory()+"/vnrvjiet/documents/"+filename);
        Uri path= FileProvider.getUriForFile(this,BuildConfig.APPLICATION_ID + ".provider",file);

        Intent pdfintent=new Intent(Intent.ACTION_VIEW);
        String type=getFileExt(path).toLowerCase();
        switch (type) {

            case "pdf": {
                pdfintent.setDataAndType(path, "application/pdf");
                break;
            }
            case "jpg": {
                pdfintent.setDataAndType(path, "image/*");
                break;
            }
            case "png": {
                pdfintent.setDataAndType(path, "image/*");
                break;
            }

            case "xlsx": {
                pdfintent.setDataAndType(path, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                break;
            }
            case "xls": {
                pdfintent.setDataAndType(path, "application/vnd.ms-excel");
                break;
            }
            case "doc": {
                pdfintent.setDataAndType(path, "application/msword");
                break;
            }
            case "ppt": {
                pdfintent.setDataAndType(path, "application/vnd.ms-powerpoint");
                break;
            }
            case "pptx": {
                pdfintent.setDataAndType(path, "application/vnd.openxmlformats-officedocument.presentationml.presentation");
                break;
            }
            default: {
                pdfintent.setDataAndType(path, "application/vnd.ms-powerpoint");
                break;
            }
        }
        pdfintent.setFlags(FLAG_GRANT_READ_URI_PERMISSION | FLAG_GRANT_WRITE_URI_PERMISSION); //must for reading data from directory
        try {
            startActivity(pdfintent);
        }catch (ActivityNotFoundException e){
            Log.i("log",e.getLocalizedMessage());
        }


    }


    @Override
    public void onItemCheckedStateChanged(android.view.ActionMode mode, int position, long id, boolean checked) {
        if (checked) {
            toDelete.add((String) adapter.getItem(position));
        } else {
            toDelete.remove(adapter.getItem(position));
        }
    }

    @Override
    public boolean onCreateActionMode(android.view.ActionMode mode, Menu menu) {
        MenuInflater menuInflater =getMenuInflater();
        menuInflater.inflate(R.menu.context_actionbar_menu_files, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(android.view.ActionMode mode, Menu menu) {

        return true;
    }

    @Override
    public boolean onActionItemClicked(android.view.ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_delete: {
                for (String itemname : toDelete) {
                    offlinefiles.remove(itemname);

                    File file = new File(Environment.getExternalStorageDirectory() + "/vnrvjiet/documents/" + itemname);
                    if (file.delete()) {
                        Toast.makeText(this, itemname + " deleted", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, " deletion failed", Toast.LENGTH_SHORT).show();

                    }
                }

                mode.finish();
                return true;
            }
            case R.id.action_share: {

                    File file = new File(Environment.getExternalStorageDirectory() + "/rcc/pdf/" + toDelete.get(0));
                    Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                    Uri screenshotUri = Uri.fromFile(file);
                    sharingIntent.setType("*/*");
                    sharingIntent.putExtra(Intent.EXTRA_STREAM, screenshotUri);
                    startActivity(Intent.createChooser(sharingIntent, "Share image using"));


            }
            default:
                return false;
        }
    }


    @Override
    public void onDestroyActionMode(android.view.ActionMode mode) {

        toDelete.clear();

    }
    public String getFileExt(Uri uri){
        ContentResolver cR = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();             //return "jpeg,png,pdf epub of files"
        String type = mime.getExtensionFromMimeType(cR.getType(uri));
        Log.i("log",type);
        return type;
    }


}

