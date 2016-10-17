package com.namhv.gallery;

public class Constants {
    public static final String DIRECTORY = "directory";
    public static final String GET_IMAGE_INTENT = "get_image_intent";
    public static final String GET_VIDEO_INTENT = "get_video_intent";
    public static final String GET_ANY_INTENT = "get_any_intent";

    // shared preferences
    static final String PREFS_KEY = "Gallery";
    static final String IS_FIRST_RUN = "is_first_run";
    static final String IS_DARK_THEME = "is_dark_theme";
    static final String IS_SAME_SORTING = "is_same_sorting";
    static final String SORT_ORDER = "sort_order";
    static final String DIRECTORY_SORT_ORDER = "directory_sort_order";
    static final String HIDDEN_FOLDERS = "hidden_folders";
    static final String SHOW_HIDDEN_FOLDERS = "show_hidden_folders";

    // sorting
    public static final int SORT_BY_NAME = 1;
    public static final int SORT_BY_DATE = 2;
    public static final int SORT_BY_SIZE = 4;

    public static final int SORT_DESCENDING = 1024;
}
