package com.minminaya.downloadservice;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.minminaya.downloadservice.service.DownloadService;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {


    private Button startBtu;
    private Button pauseBtu;
    private Button cancelBtu;
    private static final String TAG = "MainActivity";
    private DownloadService.DownloadBinder mBinder;
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBinder = (DownloadService.DownloadBinder) service;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();

        Intent intent = new Intent(this, DownloadService.class);
        startService(intent);
        bindService(intent, mConnection, BIND_AUTO_CREATE);


        //判断权限够不够，不够就给
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, 1);
        } else {
            //权限够了这里处理逻辑
            Log.d(TAG, "权限够了");
        }
    }

    private void initView() {
        startBtu = (Button) findViewById(R.id.button);
        pauseBtu = (Button) findViewById(R.id.button2);
        cancelBtu = (Button) findViewById(R.id.button3);
        startBtu.setOnClickListener(this);
        pauseBtu.setOnClickListener(this);
        cancelBtu.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (mBinder == null) {
            //没纽带，控制个毛，直接GG
            return;
        }
        switch (v.getId()) {
            case R.id.button:
                String url = "http://down.360safe.com/se/360se8.1.1.250.exe";
                mBinder.startDownLoad(url);
                break;
            case R.id.button2:
                mBinder.pauseDownLoad();
                break;
            case R.id.button3:
                mBinder.cancelDownLoad();
                break;
            default:
                break;
        }
    }


    //获取到权限回调方法
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //权限够了处理逻辑
                    Log.d(TAG, "权限够了,逻辑");
                } else {
                    Toast.makeText(this, "权限不够，程序将退出", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mConnection);
    }
}
