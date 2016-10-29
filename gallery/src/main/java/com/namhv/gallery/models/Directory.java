package com.namhv.gallery.models;

public class Directory implements Comparable {
    private final String mPath;
    private final String mThumbnail;
    private final String mName;
    private final long mTimestamp;
    private int mMediaCnt;

    public Directory(String path, String thumbnail, String name, int mediaCnt, long timestamp) {
        mPath = path;
        mThumbnail = thumbnail;
        mName = name;
        mMediaCnt = mediaCnt;
        mTimestamp = timestamp;
    }

    public String getPath() {
        return mPath;
    }

    private String getThumbnail() {
        return mThumbnail;
    }

    public String getName() {
        return mName;
    }

    public int getMediaCnt() {
        return mMediaCnt;
    }

    public void setMediaCnt(int cnt) {
        mMediaCnt = cnt;
    }

    private long getTimestamp() {
        return mTimestamp;
    }

    @Override
    public int compareTo(Object object) {
        final Directory directory = (Directory) object;
        return mPath.compareTo(directory.getPath());
    }

    @Override
    public String toString() {
        return "Directory {" +
                "path=" + getPath() +
                ", thumbnail=" + getThumbnail() +
                ", name=" + getName() +
                ", timestamp=" + getTimestamp() +
                ", mediaCnt=" + getMediaCnt() + "}";
    }
}
