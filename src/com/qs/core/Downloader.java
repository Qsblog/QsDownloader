package com.qs.core;

import com.qs.constant.Constant;
import com.qs.util.FileUtils;
import com.qs.util.HttpUtils;
import com.qs.util.LogUtils;

import javax.lang.model.element.VariableElement;
import java.io.*;
import java.net.HttpURLConnection;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 下载器
 */
public class Downloader {

    public ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);

    /**
     * 只通过io实现下载 不展示下载中的信息
     * @param url 下载链接
     */
    public void normalDownload(String url) {
        // 获取文件名
        String httpFileName = HttpUtils.getFileName(url);

        //文件下载路径
        httpFileName = Constant.PATH + httpFileName;

        // 获取连接对象
        HttpURLConnection httpURLConnection = null;
        try {
            // 获取连接
            httpURLConnection = HttpUtils.getHttpURLConnection(url);
        } catch (IOException e) {
            LogUtils.error("{}获取连接失败{}", url, e);
        }

        try (InputStream inputStream = httpURLConnection.getInputStream();
             BufferedInputStream bis = new BufferedInputStream(inputStream);
             FileOutputStream fos = new FileOutputStream(httpFileName);
             BufferedOutputStream bos =  new BufferedOutputStream(fos);
        ) {
            int len = -1;
            while ((len = bis.read()) != -1){
                bos.write(len);
            }
        } catch (FileNotFoundException e) {
            LogUtils.error("要下载的文件不存在:{}",url);
        } catch (Exception e){
            LogUtils.error("下载失败");
        } finally {
            if (httpURLConnection != null){
                httpURLConnection.disconnect();
            }
            LogUtils.info("{}下载完成",url);
        }
    }

    /**
     * 通过scheduledExecutor实现每秒打印下载信息
     * @param url 下载链接
     */
    public void scheduledExecutorDownload(String url) {
        // 获取文件名
        String httpFileName = HttpUtils.getFileName(url);

        //文件下载路径
        httpFileName = Constant.PATH + httpFileName;

        // 获取本地文件的大小
        long localFIleLength = FileUtils.getFileContentLength(httpFileName);

        // 获取连接对象
        HttpURLConnection httpURLConnection = null;
        DownloadInfoThread downloadInfoThread = null;
        try {
            // 获取连接
            httpURLConnection = HttpUtils.getHttpURLConnection(url);

            // 获取下载文件的大小
            int contentLength = httpURLConnection.getContentLength();

            // 判断文件是否已经下载过
            if (localFIleLength >= contentLength){
                LogUtils.info("{}之前已经下载完毕，请勿重复下载",httpFileName);
                return;
            }

            // 创建获取下载信息的任务对象
            downloadInfoThread = new DownloadInfoThread(contentLength);

            // 把任务交给 scheduledExecutorService 执行，每隔 1s 执行一次
            scheduledExecutorService.scheduleAtFixedRate(downloadInfoThread, 1, 1, TimeUnit.SECONDS);

        } catch (IOException e) {
            LogUtils.error("{}获取连接失败{}", url, e);
        }

        if (httpURLConnection == null){
            throw new RuntimeException("获取下载链接对象失败");
        }

        try (InputStream inputStream = httpURLConnection.getInputStream();
             BufferedInputStream bis = new BufferedInputStream(inputStream);
             FileOutputStream fos = new FileOutputStream(httpFileName);
             BufferedOutputStream bos =  new BufferedOutputStream(fos);
        ) {
            int len = -1;
            byte[] buffer = new byte[Constant.BUFFER_SIZE];
            while ((len = bis.read(buffer)) != -1){
                downloadInfoThread.setDownSize(downloadInfoThread.getDownSize() + len);
                bos.write(buffer,0,len);
            }
        } catch (FileNotFoundException e) {
            LogUtils.error("要下载的文件不存在:{}",url);
        } catch (Exception e){
            LogUtils.error("下载失败");
        } finally {
            System.out.print("\r");
            System.out.print("下载完成");
            // 关闭 http 连接
            httpURLConnection.disconnect();
            // 关闭 scheduledExecutorService
            scheduledExecutorService.shutdown();
        }
    }
}
