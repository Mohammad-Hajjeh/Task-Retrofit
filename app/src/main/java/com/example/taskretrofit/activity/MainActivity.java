package com.example.taskretrofit.activity;

import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;

import android.os.Environment;
import android.os.Bundle;

import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProviders;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.taskretrofit.BuildConfig;
import com.example.taskretrofit.R;
import com.example.taskretrofit.model.RetrofitClient;
import com.example.taskretrofit.model.Status;
import com.example.taskretrofit.service.ApkDownloadService;
import com.example.taskretrofit.model.AppVersion;
import com.example.taskretrofit.view_model.VersionViewModel;


import java.io.File;

import java.util.List;


import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;


public class MainActivity extends AppCompatActivity {


    private static final String CURRENT_VERSION_NAME = BuildConfig.VERSION_NAME;
    private static final Integer REQUEST_CODE = 101;
    public static final int DIALOG_DOWNLOAD_PROGRESS = 0;
    private ProgressDialog progressDialog;
    private AppVersion apk;
    private VersionViewModel versionViewModel;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        versionViewModel= ViewModelProviders.of(this).get(VersionViewModel.class);
        loadJson();


    }

    void loadJson() {
        LiveData<Observable<List<AppVersion>>> versionObservable = versionViewModel.getVersionLiveData();
        if(versionObservable!=null)
        versionObservable.getValue().subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(this::handleResults, this::handleError);
    }


    private void handleResults(List<AppVersion> apkList) {
        File destinationFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), getString(R.string.version_apk_name));
        if (apkList != null && apkList.size() != 0) {
            apk = apkList.get(0);
            askForPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, REQUEST_CODE);
            if (!apk.getVersion().equals(CURRENT_VERSION_NAME)) {
                if (!destinationFile.exists()) {
                    downloadZipFile();
                } else {
                    installFile();
                }
            } else {
                Toast.makeText(this, R.string.no_update, Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, R.string.result_not_found,
                    Toast.LENGTH_LONG).show();
        }
    }

    private void handleError(Throwable t) {

        Toast.makeText(this, R.string.error_fitch_api,
                Toast.LENGTH_LONG).show();
    }

    private void downloadZipFile() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setCancelable(true);
        builder.setTitle(R.string.version_download_popup_title);
        builder.setMessage(R.string.version_download_popup_message);
        builder.setNegativeButton(R.string.version_cancel_popup, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });

        builder.setPositiveButton(R.string.version_ok_popup, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                startService(new Intent(getApplicationContext(), ApkDownloadService.class));
            }

        });
        builder.show();
        LocalBroadcastManager.getInstance(this).registerReceiver(
                messageReceiver, new IntentFilter(getString(R.string.service_notify)));

    }

    private void askForPermission(String permission, Integer requestCode) {
        if (ContextCompat.checkSelfPermission(MainActivity.this, permission) != PackageManager.PERMISSION_GRANTED) {


            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, permission)) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{permission}, requestCode);

            } else {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{permission}, requestCode);
            }
        } else if (ContextCompat.checkSelfPermission(MainActivity.this, permission) == PackageManager.PERMISSION_DENIED) {
            Toast.makeText(getApplicationContext(), R.string.permission_denied, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (ActivityCompat.checkSelfPermission(this, permissions[0]) == PackageManager.PERMISSION_GRANTED) {

            if (requestCode == REQUEST_CODE) {
                Toast.makeText(this, R.string.permission_granted, Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, R.string.permission_denied_message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case DIALOG_DOWNLOAD_PROGRESS:
                progressDialog = new ProgressDialog(this);
                progressDialog.setMessage(getString(R.string.download_file));
                progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                progressDialog.setCancelable(false);
                progressDialog.show();
                return progressDialog;
            default:
                return null;
        }
    }

    void installFile() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setCancelable(true);
        builder.setTitle(R.string.version_install_popup_title);
        builder.setMessage(R.string.version_install_popup_message);

        builder.setNegativeButton(R.string.version_cancel_popup, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();

            }
        });

        builder.setPositiveButton(R.string.version_ok_popup, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                installOk();
            }
        });
        builder.show();
    }

    void installOk() {
        File destinationFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), getString(R.string.version_apk_name));
        Uri uri = FileProvider.getUriForFile(getApplicationContext(), BuildConfig.APPLICATION_ID + getString(R.string.provider), destinationFile);
        Intent promptInstall = new Intent(Intent.ACTION_VIEW).setDataAndType(uri, getString(R.string.version_install_popup_type));
        promptInstall.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        promptInstall.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        getApplicationContext().startActivity(promptInstall);
    }

    private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Status status = (Status) intent.getSerializableExtra(getString(R.string.status));
            if (status == Status.OK) {
                installFile();
            }
        }
    };
}