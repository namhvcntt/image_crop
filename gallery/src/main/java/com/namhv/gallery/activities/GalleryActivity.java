package com.namhv.gallery.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;

import com.namhv.gallery.Config;
import com.namhv.gallery.Constants;
import com.namhv.gallery.R;
import com.namhv.gallery.Utils;
import com.namhv.gallery.adapters.DirectoryAdapter;
import com.namhv.gallery.callbacks.OnDirectoryClickListener;
import com.namhv.gallery.models.Directory;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GalleryActivity extends SimpleActivity implements OnDirectoryClickListener {
    // Request permision code
    private static final int STORAGE_PERMISSION = 1;

    // Intent code
    public static final int PICK_MEDIA = 111;
    public static final String PICK_IMAGE = "PICK_IMAGE";
    public static final String PICK_VIDEO = "PICK_VIDEO";

    private RecyclerView mRcDirectories;

    private static List<Directory> mDirs;
    private static List<String> mToBeDeleted;

    private static boolean mIsPickImageIntent;
    private static boolean mIsPickVideoIntent;
    private static boolean mIsThirdPartyIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.media_chooser_main_activity);

        final Intent intent = getIntent();
        mIsPickImageIntent = isPickImageIntent(intent);
        mIsPickVideoIntent = isPickVideoIntent(intent);
        mIsThirdPartyIntent = mIsPickImageIntent || mIsPickVideoIntent;

        mToBeDeleted = new ArrayList<>();
        mDirs = new ArrayList<>();


        mRcDirectories = (RecyclerView) findViewById(R.id.directories_grid);
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
        tryLoadGallery();
    }

    @Override
    protected void onPause() {
        super.onPause();
        deleteDirs();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Config.newInstance(getApplicationContext()).setIsFirstRun(false);
    }

    private void tryLoadGallery() {
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

        mRcDirectories.setLayoutManager(new GridLayoutManager(this, 2));
        final DirectoryAdapter adapter = new DirectoryAdapter(this, mDirs);
        mRcDirectories.setAdapter(adapter);

    }

    private List<Directory> getDirectories() {
        final Map<String, Directory> directories = new LinkedHashMap<>();
        final List<String> invalidFiles = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            if ((mIsPickVideoIntent) && i == 0)
                continue;

            Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            if (i == 1) {
                if (mIsPickImageIntent)
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
        return PICK_IMAGE.contentEquals(intent.getAction());
    }

    private boolean isPickVideoIntent(Intent intent) {
        return PICK_VIDEO.contentEquals(intent.getAction());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == PICK_MEDIA && data != null) {
                final Intent result = new Intent();
                final String path = data.getData().getPath();
                final Uri uri = Uri.fromFile(new File(path));
                if (mIsPickImageIntent || mIsPickVideoIntent) {
                    result.setData(uri);
                    result.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }

                setResult(RESULT_OK, result);
                finish();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onItemClick(Directory directory) {
        final Intent intent = new Intent(this, MediaActivity.class);
        intent.putExtra(Constants.DIRECTORY, directory.getPath());
        intent.putExtra(Constants.GET_VIDEO_INTENT, mIsPickVideoIntent);
        intent.putExtra(Constants.GET_IMAGE_INTENT, mIsPickImageIntent);
        startActivityForResult(intent, PICK_MEDIA);
    }
}
