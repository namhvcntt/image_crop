package com.namhv.image_crop;

import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.namhv.gallery.R;


public class CropImageActivity extends AppCompatActivity {

    public static final String ARG_IMAGE_PATH = "img_path";
    // Lifecycle Method ////////////////////////////////////////////////////////////////////////////

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        String imgPath = getIntent().getStringExtra(ARG_IMAGE_PATH);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().add(R.id.container, CropImageFragment.getInstance(imgPath)).commit();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    public void startResultActivity(Uri uri) {
        if (isFinishing()) return;
        // Start ResultActivity
        startActivity(ResultActivity.createIntent(this, uri));
    }
}
