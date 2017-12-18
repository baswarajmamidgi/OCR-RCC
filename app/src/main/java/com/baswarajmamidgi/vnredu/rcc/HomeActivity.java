package com.baswarajmamidgi.vnredu.rcc;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static android.view.GestureDetector.SimpleOnGestureListener;

public class HomeActivity extends Activity
        implements RecyclerView.OnItemTouchListener,
        View.OnClickListener,
        ActionMode.Callback,NavigationView.OnNavigationItemSelectedListener {

    RecyclerView recyclerView;
    RecyclerViewAdapter adapter;
    int itemCount;
    GestureDetectorCompat gestureDetector;
    ActionMode actionMode;
    FloatingActionButton ocr;
    FloatingActionButton barcode;

    Context mContext;
    boolean doubleBackToExitPressedOnce = false;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.layout_home);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("RCC");

        barcode= (FloatingActionButton) findViewById(R.id.barcode);
        barcode.setOnClickListener(this);

        ocr = (FloatingActionButton) findViewById(R.id.camera);
        ocr.setOnClickListener(this);

        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        // you can set the first visible item like this:
        layoutManager.scrollToPosition(0);
        recyclerView.setLayoutManager(layoutManager);

        // allows for optimizations if all items are of the same size:
        recyclerView.setHasFixedSize(true);

        List<Record> items = OCRApp.getData();
        Log.i("log", String.valueOf(items.size()));
        adapter = new RecyclerViewAdapter(items,HomeActivity.this);
        recyclerView.setAdapter(adapter);

        RecyclerView.ItemDecoration itemDecoration =
                new DividerItemDecoration(this, DividerItemDecoration.VERTICAL_LIST);
        recyclerView.addItemDecoration(itemDecoration);

        recyclerView.setItemAnimator(new DefaultItemAnimator());

        // onClickDetection is done in this Activity's onItemTouchListener
        // with the help of a GestureDetector;
        // Tip by Ian Lake on G+ in a comment to this post:
        // https://plus.google.com/+LucasRocha/posts/37U8GWtYxDE


        recyclerView.addOnItemTouchListener(this);
        gestureDetector =
                new GestureDetectorCompat(this, new RecyclerViewDemoOnGestureListener());


        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

    }





    private void removeItemFromList() {
        int position = ((LinearLayoutManager) recyclerView.getLayoutManager()).
                findFirstCompletelyVisibleItemPosition();
        OCRApp.removeItemFromList(position);
        adapter.removeData(position);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.camera) {
            Intent i=new Intent(HomeActivity.this,ImageCapture.class);
            startActivity(i);
        }

        if(view.getId()==R.id.barcode)
        {
            barcode.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(HomeActivity.this,BarcodeCaptureActivity.class));
                }
            });
        }
        else if (view.getId() == R.id.recordcard) {
            // item click
            int idx = recyclerView.getChildPosition(view);
            if (actionMode != null) {
                myToggleSelection(idx);
                return;
            }

            ProgressDialog progressDialog=new ProgressDialog(HomeActivity.this);
            progressDialog.setMessage("Loading...");
            progressDialog.show();
            Record data = adapter.getItem(idx);
            Intent startIntent = new Intent(this, OcrActivity.class);
            startIntent.putExtra("data", data.pathToImage);
            this.startActivity(startIntent);
        }
    }



    private void myToggleSelection(int idx) {
        adapter.toggleSelection(idx);
        String title = getString(R.string.selected_count, adapter.getSelectedItemCount());
        actionMode.setTitle(title);
    }

    @Override
    public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
        gestureDetector.onTouchEvent(e);
        return false;
    }

    @Override
    public void onTouchEvent(RecyclerView rv, MotionEvent e) {
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean b) {

    }

    @Override
    public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
        // Inflate a menu resource providing context menu items
        MenuInflater inflater = actionMode.getMenuInflater();
        inflater.inflate(R.menu.context_actionbar_menu, menu);
        ocr.setVisibility(View.GONE);
        barcode.setVisibility(View.GONE);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.action_remove:
                {
                List<Integer> selectedItemPositions = adapter.getSelectedItems();
                int currPos;
                for (int i = selectedItemPositions.size() - 1; i >= 0; i--) {
                    currPos = selectedItemPositions.get(i);

                    File file = new File(OCRApp.getData().get(currPos).pathToImage);

                    if (file.exists()) {
                        if (file.delete()) {
                            Toast.makeText(HomeActivity.this, " deleted", Toast.LENGTH_SHORT).show();
                            OCRApp.removeItemFromList(currPos);
                            adapter.removeData(currPos);
                        } else {
                            Toast.makeText(HomeActivity.this, file.toString() + " deletion failed", Toast.LENGTH_SHORT).show();

                        }
                    }
                    actionMode.finish();
                    return true;
                }
            }
            case R.id.action_share:
            {
                Intent i=new Intent();
                i.setAction(Intent.ACTION_SEND_MULTIPLE);
                i.setType("*/*");
                List<Integer> selectedItemPositions = adapter.getSelectedItems();
                ArrayList<Uri> files = new ArrayList<Uri>();
                int currPos;
                for (int j = selectedItemPositions.size() - 1; j >= 0; j--) {
                    currPos = selectedItemPositions.get(j);
                    File file=new File(OCRApp.getData().get(currPos).pathToImage);
                    Uri uri = Uri.fromFile(file);
                    files.add(uri);
                }
                i.putParcelableArrayListExtra(Intent.EXTRA_STREAM, files);
                i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                startActivity(Intent.createChooser(i, "share Image using"));



                actionMode.finish();
                    return true;

            }

            default:
                return false;

        }
    }

    @Override
    public void onDestroyActionMode(ActionMode actionMode) {
        this.actionMode = null;
        adapter.clearSelections();
        ocr.setVisibility(View.VISIBLE);
        barcode.setVisibility(View.VISIBLE);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        SharedPreferences settings = getSharedPreferences("settings", 0);
        boolean isChecked1 = settings.getBoolean("checkbox1", false);
        boolean isChecked2 = settings.getBoolean("checkbox2", false);
        MenuItem item = menu.findItem(R.id.sort_date);
        item.setChecked(isChecked1);
        MenuItem item2 = menu.findItem(R.id.sort_alphabetical);
        item2.setChecked(isChecked2);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        SharedPreferences settings = getSharedPreferences("settings", 0);
        SharedPreferences.Editor editor = settings.edit();


        if (id == R.id.sort_date) {
            /*editor.putString("sort", " DESC");
            editor.apply();
            Intent i = new Intent(this, MainActivity.class);
            finishAffinity();
            startActivity(i);
            return true;
            */

        }
        if (id == R.id.sort_alphabetical) {
          /*  editor.putString("sort"," ASC" );
            editor.apply();
            Intent i = new Intent(this, MainActivity.class);
            finishAffinity();
            startActivity(i);
            return true;
            */

        }
        return super.onOptionsItemSelected(item);

    }


    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.settings: {
                startActivity(new Intent(this, AppSettings.class));
                break;
            }
            case R.id.savedfiles:
            {
                startActivity(new Intent(this,SavedFiles.class));
                break;
            }
            case R.id.feedback: {

                startActivity(new Intent(this,Feedback.class));
                break;

            }
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);

        return true;

    }


    private class RecyclerViewDemoOnGestureListener extends SimpleOnGestureListener {
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            View view = recyclerView.findChildViewUnder(e.getX(), e.getY());
            onClick(view);
            return super.onSingleTapConfirmed(e);
        }

        public void onLongPress(MotionEvent e) {
            View view = recyclerView.findChildViewUnder(e.getX(), e.getY());
            if (actionMode != null) {
                return;
            }
            // Start the CAB using the ActionMode.Callback defined above
            actionMode = startActionMode(HomeActivity.this);
            int idx = recyclerView.getChildPosition(view);
            myToggleSelection(idx);
            super.onLongPress(e);
        }
    }



    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            finishAffinity();

            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Press again to exit", Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                doubleBackToExitPressedOnce = false;
            }
        }, 2000);
    }
}

