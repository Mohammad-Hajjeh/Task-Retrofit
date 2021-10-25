package com.example.taskretrofit.service;

import com.example.taskretrofit.model.AppVersion;

import java.util.List;

import io.reactivex.Observable;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;

public interface RetrofitInterface {
    @GET("APK")
    Observable<List<AppVersion>> getApk();

    @GET(".")
    Call<ResponseBody> downloadFileByUrl();

}