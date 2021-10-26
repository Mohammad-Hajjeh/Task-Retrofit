package com.example.taskretrofit.view_model;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.taskretrofit.model.AppVersion;
import com.example.taskretrofit.repository.VersionRepository;

import java.util.List;

import io.reactivex.Observable;

public class VersionViewModel extends AndroidViewModel {
    private LiveData<Observable<List<AppVersion>>> versionLiveData;
    private VersionRepository versionRepository;

    public VersionViewModel(@NonNull Application application) {
        super(application);
        versionRepository = new VersionRepository();
        this.versionLiveData = versionRepository.getVersion();
    }

    public LiveData<Observable<List<AppVersion>>> getVersionLiveData() {
        return versionLiveData;
    }

    public void installVersion(Context context) {
        versionRepository.installVersion(context);
    }
}
