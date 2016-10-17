package com.namhv.gallery;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

public class Config {
    private SharedPreferences mPrefs;

    public static Config newInstance(Context context) {
        return new Config(context);
    }

    private Config(Context context) {
        mPrefs = context.getSharedPreferences(Constants.PREFS_KEY, Context.MODE_PRIVATE);
    }

    public void setIsFirstRun(boolean firstRun) {
        mPrefs.edit().putBoolean(Constants.IS_FIRST_RUN, firstRun).apply();
    }

    private boolean getIsSameSorting() {
        return mPrefs.getBoolean(Constants.IS_SAME_SORTING, true);
    }

    public int getSorting() {
        if (getIsSameSorting())
            return getDirectorySorting();

        return mPrefs.getInt(Constants.SORT_ORDER, Constants.SORT_BY_DATE | Constants.SORT_DESCENDING);
    }

    public int getDirectorySorting() {
        return mPrefs.getInt(Constants.DIRECTORY_SORT_ORDER, Constants.SORT_BY_NAME);
    }

    public boolean getShowHiddenFolders() {
        return mPrefs.getBoolean(Constants.SHOW_HIDDEN_FOLDERS, false);
    }

    public Set<String> getHiddenFolders() {
        return mPrefs.getStringSet(Constants.HIDDEN_FOLDERS, new HashSet<String>());
    }

    public boolean getIsFolderHidden(String path) {
        return getHiddenFolders().contains(path);
    }
}
