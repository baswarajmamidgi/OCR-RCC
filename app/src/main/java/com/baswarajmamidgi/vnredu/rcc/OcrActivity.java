package com.baswarajmamidgi.vnredu.rcc;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.Text;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

public class OcrActivity extends AppCompatActivity  implements GoogleApiClient.ConnectionCallbacks,GoogleApiClient.OnConnectionFailedListener,View.OnClickListener {
    private TextRecognizer textRecognizer;
    private Bitmap bitmap;
    private String path;
    private Dialog dialog;
    private GridView gridView;
    private String toolbartitle;
    private TextView text_blocks;
    private Bitmap originalbitmap;
    private ArrayList<String> wordsarray = new ArrayList<>();
    private GoogleApiClient mGoogleApiClient;
    private static final String TAG = "log";
    private static final int REQUEST_CODE_RESOLUTION = 3;
    private ArrayAdapter<String> arrayAdapter;
    private String blocks = "";
    private EditText recordname;
    private Dialog edittext_dialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ocr);
        ImageView photoView = (ImageView) findViewById(R.id.photo_view);
        FloatingActionButton retake = (FloatingActionButton) findViewById(R.id.retake);
        retake.setOnClickListener(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });


        Intent i = getIntent(); //receive data from camera activity
        path = i.getStringExtra("data");
        originalbitmap = BitmapFactory.decodeFile(path);
        bitmap = decodeSampledBitmapFromFiles(path, 200, 200);

        dialog = new Dialog(OcrActivity.this);
        dialog.setContentView(R.layout.dialog);
        dialog.setTitle("Save as...");
        recordname = (EditText) dialog.findViewById(R.id.editname);


        text_blocks= (TextView) findViewById(R.id.text_blocks);
        toolbartitle = "Image-" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime()) + ".jpg";
        recordname.setText(toolbartitle);
        getSupportActionBar().setTitle(toolbartitle);

        final TextView cancel = (TextView) dialog.findViewById(R.id.cancel);
        cancel.setOnClickListener(this);

        TextView savefile = (TextView) dialog.findViewById(R.id.updatename);
        savefile.setOnClickListener(this);



        gridView = (GridView) findViewById(R.id.gridview);
        registerForContextMenu(gridView);
        textRecognizer = new TextRecognizer.Builder(this).build();
        photoView.setImageBitmap(bitmap);
        photoView.setOnClickListener(this);

        if (!textRecognizer.isOperational()) {
            // Note: The first time that an app using a Vision API is installed on a
            // device, GMS will download a native libraries to the device in order to do detection.
            // Usually this completes before the app is run for the first time.  But if that
            // download has not yet completed, then the above call will not detect any text,
            // barcodes, or faces.
            //
            // isOperational() can be used to check if the required native libraries are currently
            // available.  The detectors will automatically become operational once the library
            // downloads complete on device.
            Log.w("log", "Detector dependencies are not yet available.");
            // Check for low storage.  If there is low storage, the native library will not be
            // downloaded, so detection will not become operational.
            IntentFilter lowstorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
            boolean hasLowStorage = registerReceiver(null, lowstorageFilter) != null;

            if (hasLowStorage) {
                Toast.makeText(this, R.string.low_storage_error, Toast.LENGTH_LONG).show();
            }
        }

        processImage(originalbitmap);  //extracts text from the image


        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {

            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.saveas:
                        dialog.show();
                        break;

                    case R.id.edit_text:
                        /*Bundle args = new Bundle();
                        args.putString("blocks", blocks);

                        //Initializing a bottom sheet
                        BottomSheetDialogFragment bottomSheetDialogFragment = new CustomBottomSheetDialogFragment();
                        bottomSheetDialogFragment.setArguments(args);
                        bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());

                        */

                        edittext_dialog  = new Dialog(OcrActivity.this,R.style.Theme_Dialog);
                        edittext_dialog.setContentView(R.layout.dialog_edittext);
                        final EditText editText = (EditText) edittext_dialog.findViewById(R.id.edit_text);

                        editText.setText(blocks);
                        //edittext_dialog.setTitle("Edi");
                        edittext_dialog.show();

                        TextView save_edittext = (TextView) edittext_dialog.findViewById(R.id.save_edittext);
                        save_edittext.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                String data=editText.getText().toString();
                                blocks=data;
                                text_blocks.setText(blocks);
                                edittext_dialog.dismiss();
                            }
                        });
                        TextView cancel_edittext = (TextView) edittext_dialog.findViewById(R.id.cancel_edittext);
                        cancel_edittext.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                edittext_dialog.dismiss();

                            }
                        });
                        break;

                    case R.id.make_pdf:
                        stringtopdf(blocks);
                        Toast.makeText(OcrActivity.this, "PDF created", Toast.LENGTH_SHORT).show();
                        break;


                    case R.id.share:
                        sharepdf();
                        break;


                    case R.id.upload:
                        saveFileToDrive();
                        break;
                }
                return true;
            }
            });
        }




    @Override
    public void onClick(View v) {
        switch (v.getId()){

            case R.id.photo_view: {
                final Dialog dialog = new Dialog(OcrActivity.this, R.style.Theme_Dialog);
                dialog.setContentView(R.layout.imageview);
                ImageView imageView = (ImageView) dialog.findViewById(R.id.imageView);
                imageView.setImageBitmap(originalbitmap);
                dialog.show();
                ImageView cancel = (ImageView) dialog.findViewById(R.id.canceldialog);
                cancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });
                break;
            }

            case R.id.retake:
            {
                startActivity(new Intent(OcrActivity.this, ImageCapture.class));
                finish();
                break;
            }


            case R.id.updatename:
            {
                try {

                    String filename=recordname.getText().toString();
                    if(TextUtils.isEmpty(filename)){
                        Toast.makeText(OcrActivity.this, "enter file name", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    savaImage(filename);
                    startActivity(new Intent(OcrActivity.this,HomeActivity.class));
                } catch (IOException e) {
                    Toast.makeText(OcrActivity.this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                }
                break;
            }
            case R.id.cancel:
            {
              dialog.dismiss();
                break;
            }

        }
        }


    public void processImage(Bitmap bitmap) {

        try {
            if (textRecognizer.isOperational() && bitmap != null) {
                Frame frame = new Frame.Builder().setBitmap(bitmap).build();
                SparseArray<TextBlock> textBlocks = textRecognizer.detect(frame);

                String lines = "";
                String words = "";
                for (int index = 0; index < textBlocks.size(); index++) {
                    //extract scanned text blocks here
                    TextBlock tBlock = textBlocks.valueAt(index);
                    blocks = blocks + tBlock.getValue() + "\n" + "\n";
                    for (Text line : tBlock.getComponents()) {
                        //extract scanned text lines here
                        lines = lines + line.getValue() + "\n";
                        for (Text element : line.getComponents()) {
                            //extract scanned text words here
                            words = words + element.getValue() + ", ";
                        }
                    }
                }
                if (textBlocks.size() == 0) {
                    Toast.makeText(this, "Scan Failed: Found nothing to scan", Toast.LENGTH_SHORT).show();
                    AlertDialog.Builder alertDialog=new AlertDialog.Builder(this);
                    alertDialog.setMessage("Try again  ?");
                    alertDialog.setPositiveButton("yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            startActivity(new Intent(OcrActivity.this,ImageCapture.class));
                            finishAffinity();
                        }
                    });

                    alertDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            startActivity(new Intent(OcrActivity.this,HomeActivity.class));
                            finishAffinity();

                        }
                    });
                    alertDialog.show();


                } else {
                        String[] array = words.split(",");
                    for (String word : array) {
                        if (word.length() > 3) {
                            wordsarray.add(word);

                        }
                    }

                }
            } else {
                Toast.makeText(this, "Could not set up the detector!", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Failed to load Image", Toast.LENGTH_SHORT)
                    .show();
            Log.i("log", e.getLocalizedMessage());
        }


        /*
        HashSet<String> hashSet = new HashSet<String>();
        hashSet.addAll(wordsarray);
        wordsarray.clear();                             //removes duplicates in array
        wordsarray.addAll(hashSet);
        arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, wordsarray);

        gridView.setAdapter(arrayAdapter);
        */
        text_blocks.setText(blocks);


    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.ocr_menu,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId()==R.id.report){
            Toast.makeText(this, "No activity found", Toast.LENGTH_SHORT).show();
        }
        if(item.getItemId()==R.id.help){
            Toast.makeText(this, "No activity found", Toast.LENGTH_SHORT).show();
        }
        return super.onOptionsItemSelected(item);
    }

    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.context_menu, menu);
    }


    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        final int listPosition = info.position;
        String selecteditem = wordsarray.get(listPosition);


        switch (item.getItemId()) {
            case R.id.copy: {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("label", selecteditem);
                clipboard.setPrimaryClip(clip);

            }

            case R.id.search: {
                Uri uri = Uri.parse("http://www.google.com/#q=" + selecteditem);
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
                break;
            }
        }
        return true;
    }

    @Override
    public void onBackPressed() {


        AlertDialog.Builder alertDialog=new AlertDialog.Builder(this);
        alertDialog.setMessage("Do you want to save ?");
        alertDialog.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    savaImage(toolbartitle);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                startActivity(new Intent(OcrActivity.this,HomeActivity.class));
                finishAffinity();
            }
        });

        alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startActivity(new Intent(OcrActivity.this,HomeActivity.class));
                finishAffinity();

            }
        });
        alertDialog.show();


    }


    public File stringtopdf(String data) {
        String extstoragedir = Environment.getExternalStorageDirectory().toString();
        File fol = new File(extstoragedir, "rcc");
        File folder = new File(fol, "pdf");
        File file = null;
        if (!folder.exists()) {
            boolean bool = folder.mkdir();
        }
        try {
            file = new File(folder, toolbartitle.substring(0,toolbartitle.length()-4)+".pdf");
            file.createNewFile();
            FileOutputStream fOut = new FileOutputStream(file);

            PdfDocument document = new PdfDocument();
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(2480, 3508, 1).create();
            PdfDocument.Page page = document.startPage(pageInfo);

            Canvas canvas = page.getCanvas();
            Paint paint = new Paint();
            canvas.drawText(data, 10, 10, paint);
            document.finishPage(page);
            document.writeTo(fOut);
            document.close();


        } catch (IOException e) {
            Log.i("log error", e.getLocalizedMessage());
        }

        return file;
    }


    private void savaImage(String filename) throws IOException {
        String extstoragedir = Environment.getExternalStorageDirectory().toString();
        File fol = new File(extstoragedir, "rcc");
        File folder = new File(fol, "images");
        if (!folder.exists()) {
            boolean bool = folder.mkdir();
        }

        Log.i("log filename", filename);
        File src = new File(path);
        File dst = new File(folder, filename);

        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);
        // Copy the bits from instream to outstream
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        out.flush();
        in.close();
        out.close();


    }

    private void sharepdf() {
        File file = stringtopdf(blocks);

        Intent sharefile = new Intent(Intent.ACTION_SEND);
        sharefile.setType("application/pdf");
        sharefile.putExtra(Intent.EXTRA_STREAM, Uri.parse(file.getAbsolutePath()));
        startActivity(Intent.createChooser(sharefile, "Share File"));
    }


    public static Bitmap decodeSampledBitmapFromFiles(String file, int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(file, options);
    }

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }


    private void saveFileToDrive() {
        if (mGoogleApiClient == null) {
            // Create the API client and bind it to an instance variable.
            // We use this instance as the callback for connection and connection
            // failures.
            // Since no account name is passed, the user is prompted to choose.
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Drive.API)
                    .addScope(Drive.SCOPE_FILE)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        }
        mGoogleApiClient.connect();

        final File file = stringtopdf(blocks);
        // Start by creating a new contents, and setting a callback.
        Log.i(TAG, "Creating new contents.");
        final Bitmap image = bitmap;
        Drive.DriveApi.newDriveContents(mGoogleApiClient)
                .setResultCallback(new ResultCallback<DriveApi.DriveContentsResult>() {

                    @Override
                    public void onResult(DriveApi.DriveContentsResult result) {
                        // If the operation was not successful, we cannot do anything
                        // and must
                        // fail.
                        if (!result.getStatus().isSuccess()) {
                            Log.i(TAG, "Failed to create new contents.");
                            return;
                        }
                        // Otherwise, we can write our data to the new contents.
                        Log.i(TAG, "New contents created.");
                        // Get an output stream for the contents.
                        OutputStream outputStream = result.getDriveContents().getOutputStream();
                        byte[] buf = new byte[8192];

                        BufferedOutputStream writer = new BufferedOutputStream(outputStream);


                        try {
                            InputStream is = new FileInputStream(file);
                            int c = 0;

                            while ((c = is.read(buf, 0, buf.length)) > 0) {
                                writer.write(buf, 0, c);
                                writer.flush();
                            }

                            writer.close();
                            System.out.println("stop");
                            is.close();
                        } catch (IOException e) {
                            Log.e(TAG, e.getMessage());
                        }

                        // Create the initial metadata - MIME type and title.
                        // Note that the user will be able to change the title later.
                        MetadataChangeSet metadataChangeSet = new MetadataChangeSet.Builder()
                                .setMimeType("application/pdf").setTitle("sample.pdf").build();

                        IntentSender intentSender = Drive.DriveApi
                                .newCreateFileActivityBuilder()
                                .setInitialMetadata(metadataChangeSet)
                                .setInitialDriveContents(result.getDriveContents())
                                .build(mGoogleApiClient);
                        try {
                            startIntentSenderForResult(intentSender, 1, null, 0, 0, 0);
                        } catch (IntentSender.SendIntentException e) {
                            // Handle the exception
                        }

                    }
                });
    }


    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onPause() {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
        super.onPause();
    }


    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        // Called whenever the API client fails to connect.
        Log.i(TAG, "GoogleApiClient connection failed: " + result.toString());
        if (!result.hasResolution()) {
            // show the localized error dialog.
            GoogleApiAvailability.getInstance().getErrorDialog(this, result.getErrorCode(), 0).show();
            return;
        }
        // The failure has a resolution. Resolve it.
        // Called typically when the app is not yet authorized, and an
        // authorization
        // dialog is displayed to the user.
        try {
            result.startResolutionForResult(this, REQUEST_CODE_RESOLUTION);
        } catch (IntentSender.SendIntentException e) {
            Log.e(TAG, "Exception while starting resolution activity", e);
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {

    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.i(TAG, "GoogleApiClient connection suspended");
    }


}




