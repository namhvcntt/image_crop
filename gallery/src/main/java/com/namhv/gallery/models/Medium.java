package com.namhv.gallery.models;

import java.io.Serializable;

public class Medium implements Serializable, Comparable {
    private static final long serialVersionUID = -6543139465975455L;
    private final boolean mIsVideo;
    private final long mTimestamp;
    private String mPath;

    public Medium(String path, boolean isVideo, long timestamp) {
        mPath = path;
        mIsVideo = isVideo;
        mTimestamp = timestamp;
    }

    public String getPath() {
        return mPath;
    }

    public boolean getIsVideo() {
        return mIsVideo;
    }

    public long getTimestamp() {
        return mTimestamp;
    }

    @Override
    public int compareTo(Object object) {
        final Medium medium = (Medium) object;
        int res;
        res = (mTimestamp > medium.getTimestamp()) ? 1 : -1;

        return res;
    }
}
