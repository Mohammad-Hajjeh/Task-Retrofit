package com.example.taskretrofit.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.taskretrofit.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
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
    private static final String APK_URL = "http://download1585.mediafire.com/8mh55am8r27g/jpjaomhcj7ewaf1/app-debug.apk/";
    private Retrofit retrofit;
    private File destinationFile;
    private String checkDownloadIsSuccess = "FAILED";
    ;

    public MyService() {
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Toast.makeText(getApplicationContext(), R.string.serviceDestroyed, Toast.LENGTH_LONG).show();
        if (checkDownloadIsSuccess.equalsIgnoreCase(getString(R.string.failed))) {
            wakeUpService(10, 0);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        retrofitDownload();
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
        super.onTaskRemoved(rootIntent);
        if (checkDownloadIsSuccess.equalsIgnoreCase(getString(R.string.failed))) {
            wakeUpService(1, 1);
        }
    }

    private class DownloadApkFileTask extends AsyncTask<ResponseBody, Pair<Integer, Long>, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();


        }

        @Override
        protected String doInBackground(ResponseBody... urls) {
            return saveToDisk(urls[0], getString(R.string.apkName));
        }

        protected void onProgressUpdate(Pair<Integer, Long>... progress) {
            super.onProgressUpdate();

        }

        @Override
        protected void onPostExecute(String result) {
            checkDownloadIsSuccess = result;
            if (result.equals(getString(R.string.Success))) {
                loadBroadCastReceiver(1);
                onDestroy();
            } else if (result.equals(getString(R.string.Faild))) {
                loadBroadCastReceiver(0);
            }


        }
    }

    private String saveToDisk(ResponseBody body, String filename) {
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
                if (fileSize == -1) {
                    stopSelf();
                    return getString(R.string.Faild);
                }

                Log.d(TAG, getString(R.string.fileSize) + fileSize);
                while ((count = inputStream.read(data)) != -1) {
                    outputStream.write(data, 0, count);
                    progress += count;
                    Log.d(TAG, getString(R.string.progress) + progress + getString(R.string.slash) + fileSize + getString(R.string.print) + (float) progress / fileSize);
                }

                outputStream.flush();

                Log.d(TAG, destinationFile.getParent());
                return getString(R.string.Success);
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG, getString(R.string.saveFailed));
                return getString(R.string.Faild);
            } finally {
                if (inputStream != null) inputStream.close();
                if (outputStream != null) outputStream.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, getString(R.string.saveFailed));
            return getString(R.string.Faild);

        }
    }

    void wakeUpService(int i, int b) {
        Intent myIntent = new Intent(getApplicationContext(), MyService.class);
        PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 0, myIntent, 0);
        AlarmManager alarmManager1 = (AlarmManager) getSystemService(ALARM_SERVICE);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.add(Calendar.SECOND, i);
        alarmManager1.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        if (b == 1)
            Toast.makeText(getApplicationContext(), R.string.downloading, Toast.LENGTH_LONG).show();
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

    void retrofitDownload() {
        RetrofitInterface downloadService = createService(RetrofitInterface.class);
        Call<ResponseBody> call = downloadService.downloadFileByUrl();
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, final Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, getString(R.string.gotBody));
                    downloadApkFileTask = new DownloadApkFileTask();
                    downloadApkFileTask.execute(response.body());
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

    void loadBroadCastReceiver(int status) {
        if (status == 1)
            Toast.makeText(getApplicationContext(), R.string.downloadSuccess, Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(getApplicationContext(), R.string.downloadFailed, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(getString(R.string.serviceNotify));
        intent.putExtra(getString(R.string.status), status);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }


}