package com.example.taskretrofit.service;

import com.example.taskretrofit.model.APK;

import java.util.List;

import io.reactivex.Observable;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Url;

public interface RetrofitInterface {
    @GET("APK")
    Observable<List<APK>> getApk();

    @GET(".")
    Call<ResponseBody> downloadFileByUrl();

}