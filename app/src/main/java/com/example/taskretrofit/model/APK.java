package com.example.taskretrofit.model;

import com.google.gson.annotations.SerializedName;

public class APK {
    @SerializedName("url")
    private String url;
    @SerializedName("version")
    private String version;

    public APK(String url, String version) {
        this.url = url;
        this.version = version;
    }

    public APK() {
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "APK{" +
                "url='" + url + '\'' +
                ", version='" + version + '\'' +
                '}';
    }
}
