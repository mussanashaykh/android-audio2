package com.nurulquran.audio.util;

public class Disk {

    private String bucketDisplayName;
    private String bucketID;
    private int count;
    private int id;
    private String location;
    private long size;

    public Disk(int id, String bucketID, String bucketDisplayName, int count, long size, String location) {
        this.id = id;
        this.bucketID = bucketID;
        this.bucketDisplayName = bucketDisplayName;
        this.count = count;
        this.size = size;
        this.location = location;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
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

    public int getCount() {
        return this.count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public long getSize() {
        return this.size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getLocation() {
        return this.location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String toString() {
        return "Disk{id=" + this.id + ",\tbucketID='" + this.bucketID + '\'' + ",\nbucketDisplayName='" + this.bucketDisplayName + '\'' + ",\tcount=" + this.count + ",\tsize=" + this.size + ",\nlocation='" + this.location + '\'' + '}';
    }
}