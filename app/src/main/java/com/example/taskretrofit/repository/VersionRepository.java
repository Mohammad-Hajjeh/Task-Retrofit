package com.example.taskretrofit.repository;


import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.taskretrofit.model.AppVersion;
import com.example.taskretrofit.model.RetrofitClient;
import com.example.taskretrofit.service.RetrofitInterface;

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

}
