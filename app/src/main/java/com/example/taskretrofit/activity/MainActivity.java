package com.example.taskretrofit.activity;

import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.example.taskretrofit.BuildConfig;
import com.example.taskretrofit.R;
import com.example.taskretrofit.service.RetrofitInterface;
import com.example.taskretrofit.model.APK;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    private DownloadApkFileTask downloadApkFileTask;
    private static final String TAG = "MainActivity";
    private static final String VERSION_NAME = "1.1";
    private static final String API_URL = "http://6fc8-185-114-120-43.ngrok.io/";
    private static final String APK_URL = "http://download1518.mediafire.com/sl7fikmrd8ng/8323k64nyu9zq08/app-debug.apk/";
    private Context context;
    public static final int DIALOG_DOWNLOAD_PROGRESS = 0;
    private ProgressDialog mProgressDialog;
    private File destinationFile;
    private APK apk;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;
        loadJson();


    }

    void loadJson() {
        RetrofitInterface retrofitInterface = getRetrofit(RetrofitInterface.class);
        Observable<List<APK>> apkObservable = retrofitInterface.getApk();
        apkObservable.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(this::handleResults, this::handleError);
    }

    private void handleResults(List<APK> apkList) {
        if (apkList != null && apkList.size() != 0) {
            apk = apkList.get(0);
            askForPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, 101);

            if (apk.getVersion().equals(VERSION_NAME))
                downloadZipFile();
            else
                Toast.makeText(this, R.string.noUpdate, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, R.string.resultNotFound,
                    Toast.LENGTH_LONG).show();
        }
    }

    private void handleError(Throwable t) {

        Toast.makeText(this, R.string.errorFitchApi,
                Toast.LENGTH_LONG).show();
    }

    private void downloadZipFile() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

        builder.setCancelable(true);
        builder.setTitle(R.string.downloadTitle);
        builder.setMessage(R.string.downloadMessage);

        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                RetrofitInterface downloadService = createService(RetrofitInterface.class);
                Call<ResponseBody> call = downloadService.downloadFileByUrl();

                call.enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, final Response<ResponseBody> response) {
                        if (response.isSuccessful()) {
                            Log.d(TAG, getString(R.string.gotBody));

                            Toast.makeText(getApplicationContext(), R.string.downloading, Toast.LENGTH_SHORT).show();

                            downloadApkFileTask = new DownloadApkFileTask();
                            downloadApkFileTask.execute(response.body());

                        } else {
                            Log.d(TAG, getString(R.string.connectionFailed) + response.errorBody());
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        t.printStackTrace();
                        Log.e(TAG, t.getMessage());
                    }
                });

            }

        });
        builder.show();

    }

    public <T> T createService(Class<T> serviceClass) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(APK_URL)
                .client(new OkHttpClient.Builder().build())
                .build();
        return retrofit.create(serviceClass);
    }

    public <T> T getRetrofit(Class<T> serviceClass) {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();

        Gson gson = new GsonBuilder()
                .setLenient()
                .create();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(API_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build();
        return retrofit.create(serviceClass);
    }

    private class DownloadApkFileTask extends AsyncTask<ResponseBody, Pair<Integer, Long>, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showDialog(DIALOG_DOWNLOAD_PROGRESS);

        }

        @Override
        protected String doInBackground(ResponseBody... urls) {
            saveToDisk(urls[0], getString(R.string.apkName));
            return null;
        }

        protected void onProgressUpdate(Pair<Integer, Long>... progress) {

            Log.d(getString(R.string.api123), progress[0].second + getString(R.string.space));

            if (progress[0].first == 100) {
                Toast.makeText(getApplicationContext(), R.string.downloadSuccess, Toast.LENGTH_SHORT).show();
            }


            if (progress[0].second > 0) {
                int currentProgress = (int) ((double) progress[0].first / (double) progress[0].second * 100);
                mProgressDialog.setProgress(currentProgress);
            }

            if (progress[0].first == -1) {
                Toast.makeText(getApplicationContext(), R.string.downloadFailed, Toast.LENGTH_SHORT).show();
            }

        }

        public void doProgress(Pair<Integer, Long> progressDetails) {
            publishProgress(progressDetails);
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

            builder.setCancelable(true);
            builder.setTitle(R.string.installTitle);
            builder.setMessage(R.string.installMessage);

            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.cancel();

                }
            });

            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Uri uri = FileProvider.getUriForFile(MainActivity.this, BuildConfig.APPLICATION_ID + getString(R.string.provider), destinationFile);
                    Intent promptInstall = new Intent(Intent.ACTION_VIEW).setDataAndType(uri, getString(R.string.installType));
                    promptInstall.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    promptInstall.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    context.startActivity(promptInstall);
                }
            });
            builder.show();

        }
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
                    Pair<Integer, Long> pairs = new Pair<>(progress, fileSize);
                    downloadApkFileTask.doProgress(pairs);
                    Log.d(TAG, getString(R.string.progress) + progress + getString(R.string.slash) + fileSize + getString(R.string.print) + (float) progress / fileSize);
                }

                outputStream.flush();

                Log.d(TAG, destinationFile.getParent());
                Pair<Integer, Long> pairs = new Pair<>(100, 100L);
                downloadApkFileTask.doProgress(pairs);
                return destinationFile;
            } catch (IOException e) {
                e.printStackTrace();
                Pair<Integer, Long> pairs = new Pair<>(-1, Long.valueOf(-1));
                downloadApkFileTask.doProgress(pairs);
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

    private void askForPermission(String permission, Integer requestCode) {
        if (ContextCompat.checkSelfPermission(MainActivity.this, permission) != PackageManager.PERMISSION_GRANTED) {


            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, permission)) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{permission}, requestCode);

            } else {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{permission}, requestCode);
            }
        } else if (ContextCompat.checkSelfPermission(MainActivity.this, permission) == PackageManager.PERMISSION_DENIED) {
            Toast.makeText(getApplicationContext(), R.string.permissionDenied, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (ActivityCompat.checkSelfPermission(this, permissions[0]) == PackageManager.PERMISSION_GRANTED) {

            if (requestCode == 101)
                Toast.makeText(this, R.string.permissionGranted, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, R.string.permissionDeniedMessage, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case DIALOG_DOWNLOAD_PROGRESS:
                mProgressDialog = new ProgressDialog(this);
                mProgressDialog.setMessage(getString(R.string.downloadFile));
                mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                mProgressDialog.setCancelable(false);
                mProgressDialog.show();
                return mProgressDialog;
            default:
                return null;
        }
    }
}