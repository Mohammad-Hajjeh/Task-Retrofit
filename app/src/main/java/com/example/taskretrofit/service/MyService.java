package com.example.taskretrofit.service;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.util.Pair;
import android.view.ContextThemeWrapper;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.taskretrofit.BuildConfig;
import com.example.taskretrofit.R;
import com.example.taskretrofit.activity.MainActivity;
import com.example.taskretrofit.model.APK;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class MyService extends Service {
    private DownloadApkFileTask downloadApkFileTask;
    private static final String TAG = "MainActivity";
    private static final String APK_URL = "http://download1585.mediafire.com/2r6v5l0hvvag/jpjaomhcj7ewaf1/app-debug.apk/";
    private Retrofit retrofit;
    private File destinationFile;

    public MyService() {
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Toast.makeText(this, "ServiceDestroyed", Toast.LENGTH_SHORT).show();
    }



    @Override
    public void onCreate() {
        super.onCreate();
        RetrofitInterface downloadService = createService(RetrofitInterface.class);
        Call<ResponseBody> call = downloadService.downloadFileByUrl();
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, final Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, getString(R.string.gotBody));
                    downloadApkFileTask = new DownloadApkFileTask();
                    downloadApkFileTask.execute(response.body());
                    Toast.makeText(getApplicationContext(), R.string.downloadSuccess, Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent("ServiceNotify");
                    intent.putExtra("Status", 1);
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
                    onDestroy();

                } else {
                    Log.d(TAG, getString(R.string.connectionFailed) + response.errorBody());
                    Toast.makeText(getApplicationContext(), R.string.downloadFailed, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                t.printStackTrace();
                Log.e(TAG, t.getMessage());
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        onTaskRemoved(intent);
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Intent restartServiceIntent = new Intent(getApplicationContext(), this.getClass());
        restartServiceIntent.setPackage(getPackageName());
        startService(restartServiceIntent);
        super.onTaskRemoved(rootIntent);
    }

    private class DownloadApkFileTask extends AsyncTask<ResponseBody, Pair<Integer, Long>, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();


        }

        @Override
        protected String doInBackground(ResponseBody... urls) {
//            try {
                saveToDisk(urls[0], getString(R.string.apkName));

//                Thread.sleep(50000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
            return null;
        }

        protected void onProgressUpdate(Pair<Integer, Long>... progress) {
            super.onProgressUpdate();

        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);


        }
    }

    public <T> T createService(Class<T> serviceClass) {
        retrofit = new Retrofit.Builder()
                .baseUrl(APK_URL)
                .client(new OkHttpClient.Builder().connectTimeout(60, TimeUnit.SECONDS)
                        .writeTimeout(60, TimeUnit.SECONDS)
                        .readTimeout(60, TimeUnit.SECONDS).build())
                .build();
        return retrofit.create(serviceClass);
    }

    private File saveToDisk(ResponseBody body, String filename) {
        try {


            destinationFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), filename);

            InputStream inputStream = null;
            OutputStream outputStream = null;

            try {


                inputStream = body.byteStream();
                outputStream = new FileOutputStream(destinationFile);
                byte data[] = new byte[4096];
                int count;
                int progress = 0;
                long fileSize = body.contentLength();
                Log.d(TAG, getString(R.string.fileSize) + fileSize);
                while ((count = inputStream.read(data)) != -1) {
                    outputStream.write(data, 0, count);
                    progress += count;
                    Log.d(TAG, getString(R.string.progress) + progress + getString(R.string.slash) + fileSize + getString(R.string.print) + (float) progress / fileSize);
                }

                outputStream.flush();

                Log.d(TAG, destinationFile.getParent());
                return destinationFile;
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG, getString(R.string.saveFailed));
                return null;
            } finally {
                if (inputStream != null) inputStream.close();
                if (outputStream != null) outputStream.close();


            }

        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, getString(R.string.saveFailed));
            return null;
        }
    }


}
