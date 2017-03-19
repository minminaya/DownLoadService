package com.minminaya.downloadservice.listener;

/**
 * Created by NIWA on 2017/3/19.
 */

public interface DownLoadListener {

    void onProgress(int progress);
    void onSuccess();
    void onFailed();
    void onPaused();
    void onCanceled();
}
