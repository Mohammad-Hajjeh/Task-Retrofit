package com.example.taskretrofit;
import java.util.List;

import io.reactivex.Observable;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Streaming;
import retrofit2.http.Url;

public interface RetrofitInterface {
    @GET("APK")
    Observable<List<APK>> getApk();
    @Streaming
    @GET
    Call<ResponseBody> downloadFileByUrl(@Url String fileUrl);

}