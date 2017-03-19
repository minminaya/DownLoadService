package com.minminaya.downloadservice;

import android.os.AsyncTask;
import android.os.Environment;

import com.minminaya.downloadservice.listener.DownLoadListener;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by NIWA on 2017/3/19.
 */

public class DownLoadTask extends AsyncTask<String, Integer, Integer> {

    //定义四个下载状态标记
    private static final int TYPE_SUCXCSS = 0;
    private static final int TYPE_FAILED = 1;
    private static final int TYPE_PAUSED = 2;
    private static final int TYPE_CANCELED = 3;


    private DownLoadListener mListener;

    private boolean isPaused = false;//下载是否暂停标记
    private int lastProgress;//进度条上次更新时的大小
    private boolean isCancelled = false;

    public DownLoadTask(DownLoadListener listener) {
        mListener = listener;
    }


    @Override
    protected void onProgressUpdate(Integer... values) {
        int progress = values[0];
        if (progress > lastProgress) {
            mListener.onProgress(progress);
            lastProgress = progress;
        }
    }

    @Override
    protected void onPostExecute(Integer status) {
        switch (status) {
            case TYPE_SUCXCSS:
                mListener.onSuccess();
                break;
            case TYPE_FAILED:
                mListener.onFailed();
                break;
            case TYPE_PAUSED:
                mListener.onPaused();
                break;
            case TYPE_CANCELED:
                mListener.onCanceled();
                break;
        }
    }

    @Override
    protected Integer doInBackground(String... params) {

        InputStream is = null;//输入流
        RandomAccessFile savedFile = null;//用来访问那些保存数据记录的文件的，可以用seek( )方法来访问记录
        File file = null;//待会要下载的文件地址引用

        long downloadedLength = 0;//已下载的文件长度
        String downloadUrl = params[0];//传入参数的第一个就是url
        //subString（int）代表从该字符串的第int个开始截取（向右，下标第一个是0）,lastIndexof（String）是String那里的字符数量（从左到右）
        String fileName = downloadUrl.substring(downloadUrl.lastIndexOf("/"));
        //外置卡的共享目录路径，这里要得是下载目录，平时手机里的Download文件夹
        String directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
        //文件路径加文件名
        file = new File(directory + fileName);
        if (file.exists()) {
            //如果文件已经存在，则查看已下载文件长度
            downloadedLength = file.length();
        }
        long contentLength = getContentLength(downloadUrl);//获取要文件的大小长度
        if (contentLength == 0) {
            //如果文件长度是0，说明文件有问题，返回错误标记
            return TYPE_FAILED;
        } else if (contentLength == downloadedLength) {
            //如果文件长度是downloadedLength，说明文件下完了，返回完成标记
            return TYPE_SUCXCSS;
        }

        OkHttpClient client = new OkHttpClient();//下载客户端
        Request request = new Request.Builder()//构建下载请求
                .addHeader("RANGE", "bytes=" + downloadedLength + "-")
                .url(downloadUrl).build();
        try {
            Response response = client.newCall(request).execute();//发出请求并接受
            if (response != null) {
                //收到响应，则返回response读到的输入流给本地输入流is
                is = response.body().byteStream();
                savedFile = new RandomAccessFile(file, "rw");//随机读写模式为读写，写入file中
                //跳过已下载的字节
                savedFile.seek(downloadedLength);


                byte[] b = new byte[1024];//常规读写文件形式，缓冲buffer
                int total = 0;//读到的总下载长度，此处用他们来计算出进度条大小
                int len;//读到的文件长度

                while ((len = is.read(b)) != -1) {
                    //is.read(d)是len(buffer)的长度，等于-1代表到达终点
                    if (isCancelled) {
                        //如果取消了则返回取消标记
                        return TYPE_CANCELED;
                    } else if (isPaused) {
                        return TYPE_PAUSED;
                    } else {
                        total += len;
                        savedFile.write(b, 0, len);//写len长度b数组的byte到文件中
                        int progress = (int) ((total + downloadedLength) * 100 / contentLength);
                        publishProgress(progress);//发出进度条
                    }
                }
                //关闭response响应
                response.body().close();
                return TYPE_SUCXCSS;

            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null) {
                    //关输入流
                    is.close();
                }
                if (savedFile != null) {
                    //关闭随机读取
                    savedFile.close();
                }
                if (isCancelled() && file != null) {
                    file.delete();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return TYPE_FAILED;
    }


    /**
     * 如果请求不为空并且响应成功接收到（就是以前的响应码200），就把文件的长度返回
     * 如果请求为空或者响应没接收到，返回1
     */
    private long getContentLength(String url) {

        OkHttpClient client = new OkHttpClient();//实例化客户端
        Request request = new Request.Builder()
                .url(url)
                .build();//构造请求体

        try {
            Response response = client.newCall(request).execute();//发出请求
            if (request != null && response.isSuccessful()) {
                //如果请求不为空并且响应成功接收到（就是以前的响应码200），就把文件的长度返回
                long contentLength = response.body().contentLength();
                response.close();
                return contentLength;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        //如果请求为空或者响应没接收到，返回0
        return 0;
    }

    public void pauseDownload() {
        //只需修改状态标记
        isPaused = true;
    }
    public void cancelDownload() {
        //只需修改状态标记
        isCancelled = true;
    }
}
