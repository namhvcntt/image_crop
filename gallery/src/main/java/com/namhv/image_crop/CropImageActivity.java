package com.namhv.image_crop;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.namhv.gallery.R;
import com.namhv.gallery.Utils;
import com.namhv.image_crop.image_crop.CropImageView;
import com.namhv.image_crop.image_crop.callback.CropCallback;
import com.namhv.image_crop.image_crop.callback.SaveCallback;

import java.io.File;


public class CropImageActivity extends AppCompatActivity implements View.OnClickListener {
    private static final int WRITE_STORAGE_PERMISSION = 10012;

    private static final String PROGRESS_DIALOG = "ProgressDialog";
    public static final String ARG_IMAGE_PATH = "img_path";

    private CropImageView mCropView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop_image);
        // Set-up actionbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.findViewById(R.id.iv_back).setOnClickListener(this);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (null != actionBar) {
            actionBar.setDisplayShowTitleEnabled(false);
        }
        // get intent data
        String imagePath = getIntent().getStringExtra(ARG_IMAGE_PATH);

        // Bind view
        mCropView = (CropImageView) findViewById(R.id.cropImageView);
        mCropView.setHandleShowMode(CropImageView.ShowMode.NOT_SHOW);
        mCropView.setGuideShowMode(CropImageView.ShowMode.NOT_SHOW);
        findViewById(R.id.rlt_rotate_right).setOnClickListener(this);

        if (mCropView.getImageBitmap() == null && !TextUtils.isEmpty(imagePath)) {
            File file = new File(imagePath);
            Uri imageUri = Uri.fromFile(file);
            Glide.with(this)
                    .load(imageUri)
                    .asBitmap()
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .into(mCropView);
            mCropView.setCropMode(CropImageView.CropMode.SQUARE);
        }
    }

    public void cropImage() {
        showProgress();
        mCropView.startCrop(createSaveUri(), mCropCallback, mSaveCallback);
    }

    public void showProgress() {
        ProgressDialogFragment f = ProgressDialogFragment.getInstance();
        getSupportFragmentManager()
                .beginTransaction()
                .add(f, PROGRESS_DIALOG)
                .commitAllowingStateLoss();
    }

    public void dismissProgress() {
        FragmentManager manager = getSupportFragmentManager();
        if (manager == null) return;
        ProgressDialogFragment f = (ProgressDialogFragment) manager.findFragmentByTag(PROGRESS_DIALOG);
        if (f != null) {
            getSupportFragmentManager().beginTransaction().remove(f).commitAllowingStateLoss();
        }
    }

    public Uri createSaveUri() {
        return Uri.fromFile(new File(getCacheDir(), "cropped"));
    }

    // Callbacks ///////////////////////////////////////////////////////////////////////////////////
    private final CropCallback mCropCallback = new CropCallback() {
        @Override
        public void onSuccess(Bitmap cropped) {
        }

        @Override
        public void onError() {
        }
    };

    private final SaveCallback mSaveCallback = new SaveCallback() {
        @Override
        public void onSuccess(Uri outputUri) {
            dismissProgress();
            // Return value
            Intent result = new Intent();
            result.setData(outputUri);
            setResult(Activity.RESULT_OK, result);
            finish();
        }

        @Override
        public void onError() {
            dismissProgress();
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.crop_done:
                if (Utils.hasWriteStoragePermission(getApplicationContext())) {
                    cropImage();
                } else {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, WRITE_STORAGE_PERMISSION);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == WRITE_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                cropImage();
            } else {
                Utils.showToast(getApplicationContext(), R.string.no_permissions_write_storage);
                finish();
            }
        }
    }

    @Override
    public void onBackPressed() {
        // Return value
        Intent result = new Intent();
        setResult(Activity.RESULT_CANCELED, result);
        super.onBackPressed();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.rlt_rotate_right:
                mCropView.rotateImage(CropImageView.RotateDegrees.ROTATE_90D, 100);
                break;
            case R.id.iv_back:
                onBackPressed();
                break;
        }
    }
}
