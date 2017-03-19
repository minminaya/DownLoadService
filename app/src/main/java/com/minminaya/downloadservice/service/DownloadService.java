package com.minminaya.downloadservice.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.minminaya.downloadservice.DownLoadTask;
import com.minminaya.downloadservice.MainActivity;
import com.minminaya.downloadservice.R;
import com.minminaya.downloadservice.listener.DownLoadListener;

import java.io.File;

/**
 * Created by NIWA on 2017/3/19.
 */

public class DownloadService extends Service {


    private static final String TAG = "DownloadService";
    private DownLoadTask mDownLoadTask;
    private String downloadUrl;
    private DownloadBinder mBinder = new DownloadBinder();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class DownloadBinder extends Binder {

        /**
         *  开始下载
         * */
        public void startDownLoad(String url) {
            if (mDownLoadTask == null) {
                downloadUrl = url;
                mDownLoadTask = new DownLoadTask(listener);
                mDownLoadTask.execute(downloadUrl);//开始异步任务，传入url
                startForeground(1, getNotification("下载中...", 0));
                Log.d(TAG, "开始下载的服务");
            }
        }

        public void pauseDownLoad(){
            if (mDownLoadTask != null){
                mDownLoadTask.pauseDownload();
            }
        }

        public void cancelDownLoad(){
            if (mDownLoadTask != null){
                mDownLoadTask.cancelDownload();
            }else{
                if(downloadUrl != null){

                    //下面三句是为了获取文件名字，然后对比手机存储内的，删除
                   String filename = downloadUrl.substring(downloadUrl.lastIndexOf("/"));
                    String derectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
                    File file = new File(derectory + filename);
                    if(file.exists()){
                        file.delete();
                    }
                    getNotificationManager().cancel(1);//关闭1号通知
                    stopForeground(true);
                    Log.d(TAG, "取消了");
                }
            }
        }


    }


    private DownLoadListener listener = new DownLoadListener() {
        @Override
        public void onProgress(int progress) {
            //设置进度条
            getNotificationManager().notify(1, getNotification("下载中....", progress));
        }

        @Override
        public void onSuccess() {
            mDownLoadTask = null;
            //关闭前台服务，并且创建下载成功通知
            stopForeground(true);
            getNotificationManager().notify(1, getNotification("下好了亲", -1));
            Log.d(TAG, "下载wan");
        }

        @Override
        public void onFailed() {
            mDownLoadTask = null;
            stopForeground(true);
            getNotificationManager().notify(1, getNotification("xiazai出错", -1));
            Log.d(TAG, "下载出错");
        }

        @Override
        public void onPaused() {
            mDownLoadTask = null;
            getNotificationManager().notify(1, getNotification("xiazai暂停", -1));
            Log.d(TAG, "下载暂停");
        }

        @Override
        public void onCanceled() {
            mDownLoadTask = null;
            stopForeground(true);
            getNotificationManager().notify(1, getNotification("xiazai取消了", -1));
            Log.d(TAG, "下载取消了");
        }
    };

    /**
     * 获取系统状态栏信息服务
     */
    private NotificationManager getNotificationManager() {
        return (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    /**
     *  显示进度封装
     * */
    private Notification getNotification(String title, int progress) {
        Intent intent = new Intent(this, MainActivity.class);//上下文
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, 0);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        //设置notification信息
        builder.setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .setContentIntent(pi)
                .setContentTitle(title);
        if (progress >= 0) {
            //当Progress大于等于0时才显示进度
            builder.setContentText(progress + "%");
            builder.setProgress(100, progress, false);
        }
        return builder.build();
    }


}
