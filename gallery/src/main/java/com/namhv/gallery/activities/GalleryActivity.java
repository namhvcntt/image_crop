package com.namhv.gallery.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import com.namhv.gallery.R;
import com.namhv.gallery.SpacesItemDecoration;
import com.namhv.gallery.Utils;
import com.namhv.gallery.adapters.MediaAdapter;
import com.namhv.gallery.callbacks.OnMediaClickListener;
import com.namhv.gallery.models.Directory;
import com.namhv.gallery.models.Medium;
import com.namhv.image_crop.CropImageActivity;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class GalleryActivity extends AppCompatActivity implements OnMediaClickListener, View.OnClickListener {
    // Request permision code
    private static final int STORAGE_PERMISSION = 1;

    // Intent code
    public static final int PICK_MEDIA = 111;
    public static final String PICK_IMAGE = "PICK_IMAGE";
    public static final String PICK_VIDEO = "PICK_VIDEO";

    private static List<Medium> mMedia;
    private RecyclerView mRcDirectories;

    private static boolean mIsPickImageIntent;
    private static boolean mIsPickVideoIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        // Set-up actionbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.findViewById(R.id.iv_back).setOnClickListener(this);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (null != actionBar) {
            actionBar.setDisplayShowTitleEnabled(false);
        }

        final Intent intent = getIntent();
        mIsPickImageIntent = isPickImageIntent(intent);
        mIsPickVideoIntent = isPickVideoIntent(intent);

        mMedia = new ArrayList<>();

        mRcDirectories = (RecyclerView) findViewById(R.id.media_grid);
        mRcDirectories.setLayoutManager(new GridLayoutManager(this, 3));
        mRcDirectories.addItemDecoration(new SpacesItemDecoration(getResources().getDimensionPixelOffset(R.dimen.media_chooser_item_padding)));
        MediaAdapter adapter = new MediaAdapter(this, mMedia);
        mRcDirectories.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        tryLoadGallery();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void tryLoadGallery() {
        if (Utils.hasReadStoragePermission(getApplicationContext())) {
            initializeGallery();
            mRcDirectories.getAdapter().notifyDataSetChanged();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeGallery();
            } else {
                Utils.showToast(getApplicationContext(), R.string.no_permissions_read_storage);
                finish();
            }
        }
    }

    private void initializeGallery() {
        mMedia.clear();
        final List<Directory> newDirs = getDirectories();
        for (Directory directory : newDirs) {
            initializeGallery(directory.getPath());
        }
    }

    private void initializeGallery(String path) {
        final List<Medium> newMedia = getMedia(path);
        if (newMedia.toString().equals(mMedia.toString())) {
            return;
        }

        mMedia.addAll(newMedia);
    }


    private List<Medium> getMedia(String path) {
        final List<Medium> media = new ArrayList<>();
        final List<String> invalidFiles = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            if (mIsPickVideoIntent && i == 0)
                continue;

            Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            if (i == 1) {
                if (mIsPickImageIntent)
                    continue;

                uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
            }
            final String where = MediaStore.Images.Media.DATA + " like ? ";
            final String[] args = new String[]{path + "%"};
            final String[] columns = {MediaStore.Images.Media.DATA, MediaStore.Images.Media.DATE_TAKEN};
            final Cursor cursor = getContentResolver().query(uri, columns, where, args, null);
            final String pattern = Pattern.quote(path) + "/[^/]*";

            if (cursor != null && cursor.moveToFirst()) {
                final int pathIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
                do {
                    final String curPath = cursor.getString(pathIndex);
                    if (curPath.matches(pattern)) {
                        final File file = new File(curPath);
                        if (file.exists()) {
                            final int dateIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN);
                            final long timestamp = cursor.getLong(dateIndex);
                            media.add(new Medium(curPath, (i == 1), timestamp));
                        } else {
                            invalidFiles.add(file.getAbsolutePath());
                        }
                    }
                } while (cursor.moveToNext());
                cursor.close();
            }
        }

        Collections.sort(media);

        final String[] invalids = invalidFiles.toArray(new String[invalidFiles.size()]);
        MediaScannerConnection.scanFile(getApplicationContext(), invalids, null, null);

        return media;
    }

    private List<Directory> getDirectories() {
        final Map<String, Directory> directories = new LinkedHashMap<>();
        final List<String> invalidFiles = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            Uri uriToLoad;
            if ((mIsPickVideoIntent) && i == 0) {
                Log.e("GalleryActivity", "Continue: pickVideo");
                continue;
            }

            uriToLoad = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            if (i == 1) {
                if (mIsPickImageIntent) {
                    Log.e("GalleryActivity", "Continue: pickImage");
                    continue;
                }

                uriToLoad = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
            }


            final String[] columns = {MediaStore.Images.Media.DATA, MediaStore.Images.Media.DATE_TAKEN};
            final Cursor cursor = getContentResolver().query(uriToLoad, columns, null, null, MediaStore.Images.Media.DATE_TAKEN);

            if (cursor != null && cursor.moveToFirst()) {
                final int pathIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
                do {
                    final String fullPath = cursor.getString(pathIndex);
                    final File file = new File(fullPath);
                    final String parentDir = file.getParent();

                    if (!file.exists()) {
                        invalidFiles.add(file.getAbsolutePath());
                        continue;
                    }

                    final int dateIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN);
                    final long timestamp = cursor.getLong(dateIndex);
                    if (directories.containsKey(parentDir)) {
                        final Directory directory = directories.get(parentDir);
                        final int newImageCnt = directory.getMediaCnt() + 1;
                        directory.setMediaCnt(newImageCnt);
                    } else {
                        String dirName = Utils.getFilename(parentDir);
                        directories.put(parentDir, new Directory(parentDir, fullPath, dirName, 1, timestamp));
                    }
                } while (cursor.moveToNext());
                cursor.close();
            }
        }

        final List<Directory> dirs = new ArrayList<>(directories.values());
        filterDirectories(dirs);
        Collections.sort(dirs);

        final String[] invalids = invalidFiles.toArray(new String[invalidFiles.size()]);
        MediaScannerConnection.scanFile(getApplicationContext(), invalids, null, null);

        return dirs;
    }

    private void filterDirectories(List<Directory> dirs) {
        removeNoMediaFolders(dirs);
    }

    private void removeNoMediaFolders(List<Directory> dirs) {
        final List<Directory> ignoreDirs = new ArrayList<>();
        for (final Directory d : dirs) {
            final File dir = new File(d.getPath());
            if (dir.exists() && dir.isDirectory()) {
                final String[] res = dir.list(new FilenameFilter() {
                    @Override
                    public boolean accept(File file, String filename) {
                        return filename.equals(".nomedia");
                    }
                });

                if (res.length > 0)
                    ignoreDirs.add(d);
            }
        }

        dirs.removeAll(ignoreDirs);
    }

    private boolean isPickImageIntent(Intent intent) {
        return PICK_IMAGE.contentEquals(intent.getAction());
    }

    private boolean isPickVideoIntent(Intent intent) {
        return PICK_VIDEO.contentEquals(intent.getAction());
    }

    @Override
    public void onItemClick(Medium medium) {
        final String curItemPath = medium.getPath();
        if (mIsPickImageIntent || mIsPickVideoIntent) {
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

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.iv_back:
                onBackPressed();
                break;
        }
    }
}
