package com.qs.core;

import com.qs.constant.Constant;
import com.qs.util.HttpUtils;
import com.qs.util.LogUtils;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

/**
 * 分块下载
 */
public class DownloaderTask implements Callable<Boolean> {

    /**
     * 下载文件开始位置
     */
    private long startPos;

    /**
     * 下载文件结束位置
     */
    private long endPos;

    /**
     * 下载文件的链接地址
     */
    private String url;

    /**
     * 分块下载的第几部分
     */
    private int part;

    private CountDownLatch countDownLatch;

    public DownloaderTask(long startPos, long endPos, String url, int part, CountDownLatch countDownLatch) {
        this.startPos = startPos;
        this.endPos = endPos;
        this.url = url;
        this.part = part;
        this.countDownLatch = countDownLatch;
    }

    @Override
    public Boolean call() throws Exception {
        // 获取文件名
        String httpFileName = HttpUtils.getFileName(url);

        // 分块的文件名
        httpFileName = httpFileName + Constant.SUFFIX + part;

        // 下载路径
        httpFileName = Constant.PATH + httpFileName;

        // 获取分块下载的连接
        HttpURLConnection httpURLConnection = HttpUtils.getHttpURLConnection(url, startPos, endPos);

        try(
                InputStream inputStream = httpURLConnection.getInputStream();
                BufferedInputStream bis = new BufferedInputStream(inputStream);
                RandomAccessFile randomAccessFile = new RandomAccessFile(httpFileName,"rw");
        ){
            byte[] buffer = new byte[Constant.BUFFER_SIZE];
            int len = -1;
            // 循环读取数据
            while ((len = bis.read(buffer)) != -1){
                // 1秒内下载数据之和，通过源自类进行操作
                DownloadInfoThread.downSize.add(len);
                randomAccessFile.write(buffer,0,len);
            }
        }catch (FileNotFoundException e){
            LogUtils.error("下载的文件不存在{}",url);
            return false;
        }catch (Exception e){
            LogUtils.error("下载出现异常");
            return false;
        }finally {
            httpURLConnection.disconnect();
            countDownLatch.countDown();
        }
        return true;
    }
}
