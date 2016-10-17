package com.namhv.gallery.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;

import com.namhv.gallery.Config;
import com.namhv.gallery.Constants;
import com.namhv.gallery.R;
import com.namhv.gallery.Utils;
import com.namhv.gallery.adapters.DirectoryAdapter;
import com.namhv.gallery.models.Directory;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends SimpleActivity
        implements AdapterView.OnItemClickListener, GridView.OnTouchListener {
    @BindView(R.id.directories_grid)
    GridView mGridView;

    private static final int STORAGE_PERMISSION = 1;
    private static final int PICK_MEDIA = 2;
    private static final int PICK_WALLPAPER = 3;

    private static List<Directory> mDirs;
    private static List<String> mToBeDeleted;
    private static Parcelable mState;

    private static boolean mIsSnackbarShown;
    private static boolean mIsPickImageIntent;
    private static boolean mIsPickVideoIntent;
    private static boolean mIsGetImageContentIntent;
    private static boolean mIsGetVideoContentIntent;
    private static boolean mIsGetAnyContentIntent;
    private static boolean mIsThirdPartyIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        final Intent intent = getIntent();
        mIsPickImageIntent = isPickImageIntent(intent);
        mIsPickVideoIntent = isPickVideoIntent(intent);
        mIsGetImageContentIntent = isGetImageContentIntent(intent);
        mIsGetVideoContentIntent = isGetVideoContentIntent(intent);
        mIsGetAnyContentIntent = isGetAnyContentIntent(intent);
        mIsThirdPartyIntent = mIsPickImageIntent || mIsPickVideoIntent || mIsGetImageContentIntent || mIsGetVideoContentIntent ||
                mIsGetAnyContentIntent;

        mToBeDeleted = new ArrayList<>();
        mDirs = new ArrayList<>();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (mIsThirdPartyIntent)
            return false;

        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.camera:
                startActivity(new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        tryloadGallery();
        if (mState != null && mGridView != null)
            mGridView.onRestoreInstanceState(mState);
    }

    @Override
    protected void onPause() {
        super.onPause();
        deleteDirs();
        if (mGridView != null)
            mState = mGridView.onSaveInstanceState();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Config.newInstance(getApplicationContext()).setIsFirstRun(false);
    }

    private void tryloadGallery() {
        if (Utils.hasStoragePermission(getApplicationContext())) {
            initializeGallery();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeGallery();
            } else {
                Utils.showToast(getApplicationContext(), R.string.no_permissions);
                finish();
            }
        }
    }

    private void initializeGallery() {
        final List<Directory> newDirs = getDirectories();
        if (newDirs.toString().equals(mDirs.toString())) {
            return;
        }
        mDirs = newDirs;

        final DirectoryAdapter adapter = new DirectoryAdapter(this, mDirs);
        mGridView.setAdapter(adapter);
        mGridView.setOnItemClickListener(this);
    }

    private List<Directory> getDirectories() {
        final Map<String, Directory> directories = new LinkedHashMap<>();
        final List<String> invalidFiles = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            if ((mIsPickVideoIntent || mIsGetVideoContentIntent) && i == 0)
                continue;

            Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            if (i == 1) {
                if (mIsPickImageIntent || mIsGetImageContentIntent)
                    continue;

                uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
            }
            final String[] columns = {MediaStore.Images.Media.DATA, MediaStore.Images.Media.DATE_TAKEN};
            final String order = getSortOrder();
            final Cursor cursor = getContentResolver().query(uri, columns, null, null, order);

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
                        directory.addSize(file.length());
                    } else if (!mToBeDeleted.contains(parentDir)) {
                        String dirName = Utils.getFilename(parentDir);
                        if (mConfig.getIsFolderHidden(parentDir)) {
                            dirName += " " + getResources().getString(R.string.hidden);
                        }

                        directories.put(parentDir, new Directory(parentDir, fullPath, dirName, 1, timestamp, file.length()));
                    }
                } while (cursor.moveToNext());
                cursor.close();
            }
        }

        final List<Directory> dirs = new ArrayList<>(directories.values());
        filterDirectories(dirs);
        Directory.mSorting = mConfig.getDirectorySorting();
        Collections.sort(dirs);

        final String[] invalids = invalidFiles.toArray(new String[invalidFiles.size()]);
        MediaScannerConnection.scanFile(getApplicationContext(), invalids, null, null);

        return dirs;
    }

    private void filterDirectories(List<Directory> dirs) {
        if (!mConfig.getShowHiddenFolders())
            removeHiddenFolders(dirs);

        removeNoMediaFolders(dirs);
    }

    private void removeHiddenFolders(List<Directory> dirs) {
        final Set<String> hiddenDirs = mConfig.getHiddenFolders();
        final List<Directory> ignoreDirs = new ArrayList<>();
        for (Directory d : dirs) {
            if (hiddenDirs.contains(d.getPath()))
                ignoreDirs.add(d);
        }

        dirs.removeAll(ignoreDirs);
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

    // sort the files at querying too, just to get the correct thumbnail
    private String getSortOrder() {
        final int sorting = mConfig.getDirectorySorting();
        String sortBy = MediaStore.Images.Media.DATE_TAKEN;
        if ((sorting & Constants.SORT_BY_NAME) != 0) {
            sortBy = MediaStore.Images.Media.DATA;
        }

        if ((sorting & Constants.SORT_DESCENDING) != 0) {
            sortBy += " DESC";
        }
        return sortBy;
    }

    private void deleteDirs() {
        if (mToBeDeleted == null || mToBeDeleted.isEmpty())
            return;
        mIsSnackbarShown = false;

        final List<String> updatedFiles = new ArrayList<>();
        for (String delPath : mToBeDeleted) {
            final File dir = new File(delPath);
            if (dir.exists()) {
                final File[] files = dir.listFiles();
                for (File f : files) {
                    updatedFiles.add(f.getAbsolutePath());
                    f.delete();
                }
                updatedFiles.add(dir.getAbsolutePath());
                dir.delete();
            }
        }

        final String[] deletedPaths = updatedFiles.toArray(new String[updatedFiles.size()]);
        MediaScannerConnection.scanFile(getApplicationContext(), deletedPaths, null, null);
        mToBeDeleted.clear();
    }

    private boolean isPickImageIntent(Intent intent) {
        return isPickIntent(intent) && (hasImageContentData(intent) || isImageType(intent));
    }

    private boolean isPickVideoIntent(Intent intent) {
        return isPickIntent(intent) && (hasVideoContentData(intent) || isVideoType(intent));
    }

    private boolean isPickIntent(Intent intent) {
        return intent != null && intent.getAction() != null && intent.getAction().equals(Intent.ACTION_PICK);
    }

    private boolean isGetContentIntent(Intent intent) {
        return intent != null && intent.getAction() != null && intent.getAction().equals(Intent.ACTION_GET_CONTENT) &&
                intent.getType() != null;
    }

    private boolean isGetImageContentIntent(Intent intent) {
        return isGetContentIntent(intent) &&
                (intent.getType().startsWith("image/") || intent.getType().equals(MediaStore.Images.Media.CONTENT_TYPE));
    }

    private boolean isGetVideoContentIntent(Intent intent) {
        return isGetContentIntent(intent) &&
                (intent.getType().startsWith("video/") || intent.getType().equals(MediaStore.Video.Media.CONTENT_TYPE));
    }

    private boolean isGetAnyContentIntent(Intent intent) {
        return isGetContentIntent(intent) && intent.getType().equals("*/*");
    }

    private boolean hasImageContentData(Intent intent) {
        final Uri data = intent.getData();
        return data != null && data.equals(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
    }

    private boolean hasVideoContentData(Intent intent) {
        final Uri data = intent.getData();
        return data != null && data.equals(MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
    }

    private boolean isImageType(Intent intent) {
        final String type = intent.getType();
        return type != null && (type.startsWith("image/") || type.equals(MediaStore.Images.Media.CONTENT_TYPE));
    }

    private boolean isVideoType(Intent intent) {
        final String type = intent.getType();
        return type != null && (type.startsWith("video/") || type.equals(MediaStore.Video.Media.CONTENT_TYPE));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == PICK_MEDIA && data != null) {
                final Intent result = new Intent();
                final String path = data.getData().getPath();
                final Uri uri = Uri.fromFile(new File(path));
                if (mIsGetImageContentIntent || mIsGetVideoContentIntent || mIsGetAnyContentIntent) {
                    final String type = Utils.getMimeType(path);
                    result.setDataAndTypeAndNormalize(uri, type);
                    result.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                } else if (mIsPickImageIntent || mIsPickVideoIntent) {
                    result.setData(uri);
                    result.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }

                setResult(RESULT_OK, result);
                finish();
            } else if (requestCode == PICK_WALLPAPER) {
                setResult(RESULT_OK);
                finish();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final Intent intent = new Intent(this, MediaActivity.class);
        intent.putExtra(Constants.DIRECTORY, mDirs.get(position).getPath());
        intent.putExtra(Constants.GET_VIDEO_INTENT, mIsPickVideoIntent || mIsGetVideoContentIntent);
        intent.putExtra(Constants.GET_ANY_INTENT, mIsGetAnyContentIntent);
        startActivityForResult(intent, PICK_MEDIA);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (mIsSnackbarShown) {
            deleteDirs();
        }

        return false;
    }
}
