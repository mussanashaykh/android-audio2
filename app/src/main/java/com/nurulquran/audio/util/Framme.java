package com.nurulquran.audio.util;

public class Framme {
    private String bucketDisplayName;
    private String bucketID;
    private String data;
    private long dateTaken;
    private long duration;
    private int id;
    private String mimeType;
    private String resolution;
    private long size;

    public Framme(String data) {
        this.data = data;
    }

    public Framme(int id, String data, long size, String mimeType, long duration, String resolution, long dateTaken, String bucketID, String bucketDisplayName) {
        this.id = id;
        this.data = data;
        this.size = size;
        this.mimeType = mimeType;
        this.duration = duration;
        this.resolution = resolution;
        this.dateTaken = dateTaken;
        this.bucketID = bucketID;
        this.bucketDisplayName = bucketDisplayName;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getData() {
        return this.data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public long getSize() {
        return this.size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getMimeType() {
        return this.mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public long getDuration() {
        return this.duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public String getResolution() {
        return this.resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public long getDateTaken() {
        return this.dateTaken;
    }

    public void setDateTaken(long dateTaken) {
        this.dateTaken = dateTaken;
    }

    public String getBucketID() {
        return this.bucketID;
    }

    public void setBucketID(String bucketID) {
        this.bucketID = bucketID;
    }

    public String getBucketDisplayName() {
        return this.bucketDisplayName;
    }

    public void setBucketDisplayName(String bucketDisplayName) {
        this.bucketDisplayName = bucketDisplayName;
    }

    public String toString() {
        return "Framme{id=" + this.id + ",\ndata='" + this.data + '\'' + ",\nsize=" + this.size + ",\tmimeType='" + this.mimeType + '\'' + ",\nduration=" + this.duration + ",\nresolution='" + this.resolution + '\'' + ",\tdateTaken=" + this.dateTaken + ",\nbucketID='" + this.bucketID + '\'' + ",\tbucketDisplayName='" + this.bucketDisplayName + '\'' + '}';
    }
}