package com.nathan.collage;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    //icon from https://icons8.com

    private static final String TAG = "MAIN_ACTIVITY";
    private ImageButton mImageButton1, mImageButton2, mImageButton3, mImageButton4;

    private List<ImageButton> mImageButtons;
    private ArrayList<String> mImageFilePaths;
    private String mCurrentImagePath;

    private final static String BUNDLE_KEY_IMAGE_FILE_PATHS = "bundle key image file paths";
    private final static String BUNDLE_KEY_MOST_RECENT_FILE_PATH = "bundle key most recent file path";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mImageButton1 = findViewById(R.id.imageButton1);
        mImageButton2 = findViewById(R.id.imageButton2);
        mImageButton3 = findViewById(R.id.imageButton3);
        mImageButton4 = findViewById(R.id.imageButton4);

        mImageButtons = new ArrayList<>(Arrays.asList(mImageButton1, mImageButton2, mImageButton3, mImageButton4));

        for(ImageButton button: mImageButtons) {
            button.setOnClickListener(this);
        }

        if (savedInstanceState != null) {
            mCurrentImagePath = savedInstanceState.getString(BUNDLE_KEY_MOST_RECENT_FILE_PATH);
            mImageFilePaths = savedInstanceState.getStringArrayList(BUNDLE_KEY_IMAGE_FILE_PATHS);
        }

        if (mCurrentImagePath == null) {
            mCurrentImagePath = "";
        }
        if ( mImageFilePaths == null) {
            mImageFilePaths = new ArrayList<>(Arrays.asList("","","",""));
        }
    }

    @Override
    public void onClick(View view) {
        int requestCodeButtonIndex = mImageButtons.indexOf(view);
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        Log.d(TAG, "Click on image button at index " + requestCodeButtonIndex);
        if(takePictureIntent.resolveActivity(getPackageManager()) != null) {
            try {
                File imageFile = createImageFile();
                if (imageFile != null) {
                    Uri imageURI = FileProvider.getUriForFile(this, "com.nathan.collage.fileprovider", imageFile);
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageURI);
                    startActivityForResult(takePictureIntent, requestCodeButtonIndex);
                } else {
                    Log.e(TAG, "Image file is null");
                }
            } catch (IOException e) {
                Log.e(TAG, "Error creating image file " + e);
            }
        }
    }
    // trying code from https://stackoverflow.com/questions/38158953/how-to-create-button-in-action-bar-in-android

    // create an action bar button
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // R.menu.mymenu is a reference to an xml file named mymenu.xml which should be inside your res/menu directory.
        // If you don't have res/menu, just create a directory named "menu" inside res
        getMenuInflater().inflate(R.menu.mymenu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    // handle button activities
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.screenshot) {
            takeScreenshot();
            Toast.makeText(this, "Collage Saved!", Toast.LENGTH_LONG).show();
        }
        return super.onOptionsItemSelected(item);
    }

    private File createImageFile() throws IOException {
        //create unigue file nam w time stomp
        String imageFilename = "COLLAGE_" + new Date().getTime();
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File imageFile = File.createTempFile(
                imageFilename,
                ".jpg",
                storageDir
        );
        mCurrentImagePath = imageFile.getAbsolutePath();
        return imageFile;
    }

    @Override
    protected  void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            Log.d(TAG, "onActivityResult for request code " + requestCode + " and current path " + mCurrentImagePath);
            mImageFilePaths.set(requestCode, mCurrentImagePath);    // Save the path in mImageFilePaths
            requestSaveImageToMediaStore();
        } else if (resultCode == RESULT_CANCELED) {
            mCurrentImagePath = "";
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        // comments about things
        Log.d(TAG, "focus changed " + hasFocus);
        if (hasFocus) {
            for (int index = 0; index < mImageButtons.size() ; index++){
                loadImage(index);
            }
        }
    }

    private void loadImage(final int index) {
        ImageButton imageButton = mImageButtons.get(index);
        String path = mImageFilePaths.get(index);

        if (!path.isEmpty()) {
            Picasso.get()
                    .load(new File(path))
                    .error(android.R.drawable.stat_notify_error) // built-in error icon
                    .fit()
                    .centerCrop()
                    .into(imageButton, new Callback() {
                        @Override
                        public void onSuccess() {
                            Log.d(TAG, "Image " + index + " loaded");
                        }
                        @Override
                        public void onError(Exception e) {
                            Log.e(TAG, "error loading image", e);
                        }
                    });

        }
    }

    @Override
    public void onSaveInstanceState(Bundle outBundle) {
        super.onSaveInstanceState(outBundle);
        outBundle.putString(BUNDLE_KEY_MOST_RECENT_FILE_PATH, mCurrentImagePath);
        outBundle.putStringArrayList(BUNDLE_KEY_IMAGE_FILE_PATHS, mImageFilePaths);
    }

    private void requestSaveImageToMediaStore() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED) {
            saveImage();
        }
        else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grandResults) {
        if(grandResults.length > 0 && grandResults[0] == PackageManager.PERMISSION_GRANTED) {
            saveImage();
        }
        else {
            Toast.makeText(this, "Images will NOT be saved to media store", Toast.LENGTH_SHORT).show();
        }
    }
    private void saveImage() {
        try {
            MediaStore.Images.Media.insertImage(getContentResolver(), mCurrentImagePath, "Collage", "Collage");
        } catch (IOException e) {
            Log.e(TAG, "Image file not found", e);
        }
    }

    // taken from https://stackoverflow.com/questions/2661536/how-to-programmatically-take-a-screenshot-on-android
    private void takeScreenshot() {
        Date now = new Date();
        android.text.format.DateFormat.format("yyyy-MM-dd_hh:mm:ss", now);

        try {
            // image naming and path  to include sd card  appending name you choose for file
            File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);


            String mPath = storageDir.toString() + "/" + now + ".jpg";

            // create bitmap screen capture
            View v1 = findViewById(R.id.collage_container);
            //of the entire screen = this.getWindow().getDecorView().getRootView();
            v1.setDrawingCacheEnabled(true);
            Bitmap bitmap = Bitmap.createBitmap(v1.getDrawingCache());
            v1.setDrawingCacheEnabled(false);

            File imageFile = new File(mPath);

            FileOutputStream outputStream = new FileOutputStream(imageFile);
            int quality = 100;
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
            outputStream.flush();
            outputStream.close();

            Log.d(TAG, "in button: " + imageFile.toString());

            openScreenshot(imageFile);
        } catch (Throwable e) {
            // Several error may come out with file handling or DOM
            e.printStackTrace();
        }
    }

    private void openScreenshot(File imageFile) {

        Intent intent = new Intent(Intent.ACTION_VIEW);
        Log.d(TAG, "in openScreenshot: " + imageFile.toString());
        Uri data = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".fileprovider" , imageFile);
        intent.setDataAndType(data, "image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        this.startActivity(intent);
    }
}
