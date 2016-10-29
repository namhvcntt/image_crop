package com.namhv.sample;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.namhv.gallery.R;
import com.namhv.gallery.activities.GalleryActivity;

import java.io.File;

public class SampleActivity extends AppCompatActivity {
    public static final int PICK_IMAGE = 100;
    public static final int PICK_VIDEO = 101;
    ImageView mIvResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample);

        mIvResult = (ImageView) findViewById(R.id.iv_result);

        findViewById(R.id.btn_pick_image).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SampleActivity.this, GalleryActivity.class);
                intent.setAction(GalleryActivity.PICK_IMAGE);
                startActivityForResult(intent, PICK_IMAGE);
            }
        });

        findViewById(R.id.btn_pick_video).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SampleActivity.this, GalleryActivity.class);
                intent.setAction(GalleryActivity.PICK_VIDEO);
                startActivityForResult(intent, PICK_VIDEO);
            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (Activity.RESULT_OK == resultCode) {
            if (PICK_IMAGE == requestCode) {
                Log.e("SampleActivity", "OnActivityResult:");
                final String path = data.getData().getPath();
                final Uri uri = Uri.fromFile(new File(path));
                Glide.with(this)
                        .load(uri)
                        .asBitmap()
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .into(mIvResult);
                return;
            } else if (PICK_VIDEO == requestCode) {
                final String path = data.getData().getPath();
                final Uri uri = Uri.fromFile(new File(path));
                Toast.makeText(this, uri.toString(), Toast.LENGTH_SHORT).show();
                return;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);

    }
}
