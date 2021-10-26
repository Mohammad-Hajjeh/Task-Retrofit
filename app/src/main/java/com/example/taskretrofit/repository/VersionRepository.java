package com.example.taskretrofit.repository;


import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;

import androidx.core.content.FileProvider;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.taskretrofit.BuildConfig;
import com.example.taskretrofit.R;
import com.example.taskretrofit.model.AppVersion;
import com.example.taskretrofit.model.RetrofitClient;
import com.example.taskretrofit.service.RetrofitInterface;

import java.io.File;
import java.util.List;

import io.reactivex.Observable;


public class VersionRepository {
    public VersionRepository() {
    }

    public LiveData<Observable<List<AppVersion>>> getVersion() {
        final MutableLiveData<Observable<List<AppVersion>>> version = new MutableLiveData<>();
        Observable<List<AppVersion>> versionObservable = getRetrofit(RetrofitInterface.class).getApk();
        version.setValue(versionObservable);
        return version;
    }

    public <T> T getRetrofit(Class<T> serviceClass) {
        return RetrofitClient.getInstance().getRetrofit().create(serviceClass);
    }

    public void installVersion(Context context) {
        File destinationFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), context.getString(R.string.version_apk_name));
        Uri uri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + context.getString(R.string.provider), destinationFile);
        Intent promptInstall = new Intent(Intent.ACTION_VIEW).setDataAndType(uri, context.getString(R.string.version_install_popup_type));
        promptInstall.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        promptInstall.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(promptInstall);
    }
}
