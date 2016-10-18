package com.namhv.gallery.activities;

import android.content.Intent;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.namhv.gallery.Constants;
import com.namhv.gallery.R;
import com.namhv.gallery.Utils;
import com.namhv.gallery.adapters.MediaAdapter;
import com.namhv.gallery.callbacks.OnMediaClickListener;
import com.namhv.gallery.models.Medium;
import com.namhv.image_crop.CropImageActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import static com.namhv.gallery.activities.GalleryActivity.PICK_MEDIA;

public class MediaActivity extends SimpleActivity implements OnMediaClickListener {

    RecyclerView mGridView;

    private static List<Medium> mMedia;
    private static String mPath;

    private static boolean mIsGetImageIntent;
    private static boolean mIsGetVideoIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media);
        mIsGetImageIntent = getIntent().getBooleanExtra(Constants.GET_IMAGE_INTENT, false);
        mIsGetVideoIntent = getIntent().getBooleanExtra(Constants.GET_VIDEO_INTENT, false);
        mPath = getIntent().getStringExtra(Constants.DIRECTORY);
        mMedia = new ArrayList<>();


        mGridView = (RecyclerView) findViewById(R.id.media_grid);
    }

    @Override
    protected void onResume() {
        super.onResume();
        tryLoadGallery();
    }

    private void tryLoadGallery() {
        if (Utils.hasStoragePermission(getApplicationContext())) {
            initializeGallery();
        } else {
            finish();
        }
    }

    private void initializeGallery() {
        final List<Medium> newMedia = getMedia();
        if (newMedia.toString().equals(mMedia.toString())) {
            return;
        }

        mMedia = newMedia;
        if (isDirEmpty())
            return;


        mGridView.setLayoutManager(new GridLayoutManager(this, 3));
        final MediaAdapter adapter = new MediaAdapter(this, mMedia);
        mGridView.setAdapter(adapter);

        final String dirName = Utils.getFilename(mPath);
        setTitle(dirName);
    }

    private void deleteDirectoryIfEmpty() {
        final File file = new File(mPath);
        if (file.isDirectory() && file.listFiles().length == 0) {
            file.delete();
        }
    }

    private List<Medium> getMedia() {
        final List<Medium> media = new ArrayList<>();
        final List<String> invalidFiles = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            if (mIsGetVideoIntent && i == 0)
                continue;

            Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            if (i == 1) {
                if (mIsGetImageIntent)
                    continue;

                uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
            }
            final String where = MediaStore.Images.Media.DATA + " like ? ";
            final String[] args = new String[]{mPath + "%"};
            final String[] columns = {MediaStore.Images.Media.DATA, MediaStore.Images.Media.DATE_TAKEN};
            final Cursor cursor = getContentResolver().query(uri, columns, where, args, null);
            final String pattern = Pattern.quote(mPath) + "/[^/]*";

            if (cursor != null && cursor.moveToFirst()) {
                final int pathIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
                do {
                    final String curPath = cursor.getString(pathIndex);
                    if (curPath.matches(pattern)) {
                        final File file = new File(curPath);
                        if (file.exists()) {
                            final int dateIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN);
                            final long timestamp = cursor.getLong(dateIndex);
                            media.add(new Medium(curPath, (i == 1), timestamp, file.length()));
                        } else {
                            invalidFiles.add(file.getAbsolutePath());
                        }
                    }
                } while (cursor.moveToNext());
                cursor.close();
            }
        }

        Medium.mSorting = mConfig.getSorting();
        Collections.sort(media);

        final String[] invalids = invalidFiles.toArray(new String[invalidFiles.size()]);
        MediaScannerConnection.scanFile(getApplicationContext(), invalids, null, null);

        return media;
    }

    private boolean isDirEmpty() {
        if (mMedia.size() <= 0) {
            deleteDirectoryIfEmpty();
            finish();
            return true;
        }
        return false;
    }

    @Override
    public void onItemClick(Medium medium) {
        final String curItemPath = medium.getPath();
        if (mIsGetImageIntent || mIsGetVideoIntent) {
            if (medium.getIsVideo()) {
                final Intent result = new Intent();
                result.setData(Uri.parse(curItemPath));
                setResult(RESULT_OK, result);
                finish();
            } else {
                Intent intent = new Intent(this, CropImageActivity.class);
                intent.putExtra(CropImageActivity.ARG_IMAGE_PATH, curItemPath);
                startActivityForResult(intent, PICK_MEDIA);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == PICK_MEDIA && data != null) {
                final Intent result = new Intent();
                final String path = data.getData().getPath();
                final Uri uri = Uri.fromFile(new File(path));
                result.setData(uri);
                result.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                setResult(RESULT_OK, result);
                finish();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
