package com.example.taskretrofit;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.taskretrofit.model.Status;
import com.example.taskretrofit.service.RetrofitInterface;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ApkDownloadService extends Service {
    private DownloadApkFileTask downloadApkFileTask;
    private static final String TAG = ApkDownloadService.class.getSimpleName();
    private static final String APK_URL = "http://download1585.mediafire.com/5ux42rtfs3vg/jpjaomhcj7ewaf1/app-debug.apk/.";
    private static final Integer BUFFER_SIZE = 4096;
    private static final Integer ITERATION_ERROR = 10;
    private static final Integer ITERATION_SUCCESS = 1;
    private File destinationFile;
    private String checkDownloadIsSuccess = "FAILED";


    public ApkDownloadService() {
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Toast.makeText(getApplicationContext(), R.string.service_destroyed, Toast.LENGTH_LONG).show();
        if (checkDownloadIsSuccess.equalsIgnoreCase(getString(R.string.failed))) {
            wakeUpService(ITERATION_ERROR, Status.ERROR);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService();
        } else {
            startForeground(1, new Notification());
        }
        retrofitDownload();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void startForegroundService() {
        String notificationChannelId = getString(R.string.notification_channel_id);
        String channelName = getString(R.string.channel_name);
        NotificationChannel notificationChannel = new NotificationChannel(notificationChannelId, channelName, NotificationManager.IMPORTANCE_NONE);
        notificationChannel.setLightColor(Color.BLUE);
        notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(notificationChannel);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, notificationChannelId);
        Notification notification = notificationBuilder.setOngoing(true)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentTitle(getString(R.string.app_running_in_background))
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        startForeground(2, notification);
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
            wakeUpService(ITERATION_SUCCESS, Status.OK);
        }
    }

    private class DownloadApkFileTask extends AsyncTask<ResponseBody, Pair<Integer, Long>, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(ResponseBody... urls) {
            return saveToDisk(urls[0], getString(R.string.version_apk_name));
        }

        protected void onProgressUpdate(Pair<Integer, Long>... progress) {
            super.onProgressUpdate();

        }

        @Override
        protected void onPostExecute(String result) {
            checkDownloadIsSuccess = result;
            if (result.equals(getString(R.string.Success))) {
                loadBroadCastReceiver(com.example.taskretrofit.model.Status.OK);
                stopForeground(true);
                onDestroy();
            } else if (result.equals(getString(R.string.Failed))) {
                loadBroadCastReceiver(com.example.taskretrofit.model.Status.ERROR);
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
                byte data[] = new byte[BUFFER_SIZE];
                int count;
                int progress = 0;
                long fileSize = body.contentLength();
                if (fileSize == -1) {
                    stopSelf();
                    return getString(R.string.Failed);
                }

                Log.d(TAG, getString(R.string.file_size) + fileSize);
                while ((count = inputStream.read(data)) != -1) {
                    outputStream.write(data, 0, count);
                    progress += count;
                    Log.d(TAG, getString(R.string.progress) + progress + getString(R.string.slash) + fileSize + getString(R.string.print) + (float) progress / fileSize);
                }

                outputStream.flush();

                Log.d(TAG, destinationFile.getParent());
                return getString(R.string.Success);
            } catch (IOException e) {
                Log.d(TAG, getString(R.string.save_failed));
                return getString(R.string.Failed);
            } finally {
                if (inputStream != null) inputStream.close();
                if (outputStream != null) outputStream.close();
            }

        } catch (IOException e) {
            Log.d(TAG, getString(R.string.save_failed));
            return getString(R.string.Failed);

        }
    }

    void wakeUpService(int iteration, Status status) {
        Intent myIntent = new Intent(getApplicationContext(), ApkDownloadService.class);
        PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 0, myIntent, 0);
        AlarmManager alarmManager1 = (AlarmManager) getSystemService(ALARM_SERVICE);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.add(Calendar.SECOND, iteration);
        alarmManager1.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        if (status == Status.OK) {
            Toast.makeText(getApplicationContext(), R.string.downloading, Toast.LENGTH_LONG).show();
        }
    }

    public <T> T createService(Class<T> serviceClass) {
        return RetrofitClient.getInstance().getRetrofit().create(serviceClass);

    }

    void retrofitDownload() {
        RetrofitInterface downloadService = createService(RetrofitInterface.class);
        Call<ResponseBody> call = downloadService.downloadFileByUrl(APK_URL);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, final Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, getString(R.string.got_Body_file));
                    downloadApkFileTask = new DownloadApkFileTask();
                    downloadApkFileTask.execute(response.body());
                } else {
                    Log.d(TAG, getString(R.string.connection_Failed) + response.errorBody());
                    Toast.makeText(getApplicationContext(), R.string.download_Failed, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(TAG, t.getMessage());
            }
        });
    }

    void loadBroadCastReceiver(Status status) {
        if (status == Status.OK) {
            Toast.makeText(getApplicationContext(), R.string.download_Successful, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getApplicationContext(), R.string.download_Failed, Toast.LENGTH_SHORT).show();
        }
        Intent intent = new Intent(getString(R.string.service_notify));
        intent.putExtra(getString(R.string.status), status);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }


}