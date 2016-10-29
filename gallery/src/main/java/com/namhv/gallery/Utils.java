package com.namhv.gallery;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.content.ContextCompat;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

public class Utils {
    public static String getFilename(final String path) {
        return path.substring(path.lastIndexOf("/") + 1);
    }

    public static void showToast(Context context, int resId) {
        Toast.makeText(context, context.getResources().getString(resId), Toast.LENGTH_SHORT).show();
    }

    public static boolean hasReadStoragePermission(Context cxt) {
        return ContextCompat.checkSelfPermission(cxt, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean hasWriteStoragePermission(Context cxt) {
        return ContextCompat.checkSelfPermission(cxt, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }
}
