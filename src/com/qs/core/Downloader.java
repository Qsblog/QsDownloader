package com.qs.core;

import com.qs.constant.Constant;
import com.qs.util.FileUtils;
import com.qs.util.HttpUtils;
import com.qs.util.LogUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.sql.Time;
import java.util.ArrayList;
import java.util.concurrent.*;

/**
 * 下载器
 */
public class Downloader {

    public ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);

    // 线程池对象
    public ThreadPoolExecutor poolExecutor =new ThreadPoolExecutor(Constant.THREAD_NUM, Constant.THREAD_NUM, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(Constant.THREAD_NUM));

    private CountDownLatch countDownLatch =  new CountDownLatch(Constant.THREAD_NUM);

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
        DownloadInfo downloadInfo = null;
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
            downloadInfo = new DownloadInfo(contentLength);

            // 把任务交给 scheduledExecutorService 执行，每隔 1s 执行一次
            scheduledExecutorService.scheduleAtFixedRate(downloadInfo, 1, 1, TimeUnit.SECONDS);

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
                downloadInfo.setDownSize(downloadInfo.getDownSize() + len);
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

    /**
     * 分块下载 01
     * 文件切分后分块下载
     * @param url 下载链接
     */
    public void splitDownload(String url) {
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

            // 切分任务
            ArrayList<Future> list = new ArrayList<>();
            split(url, list);

            countDownLatch.await();
        } catch (Exception e){
            LogUtils.error("下载失败");
        } finally {
            System.out.print("\r");
            System.out.print("下载完成");
            // 关闭 http 连接
            if (httpURLConnection != null){
                httpURLConnection.disconnect();
            }

            // 关闭 scheduledExecutorService
            scheduledExecutorService.shutdown();

            // 关闭线程池
            poolExecutor.shutdown();
            try {
                // 等待 30 s如果还没有关 就强制停止
                poolExecutor.awaitTermination(30, TimeUnit.SECONDS);
                poolExecutor.shutdownNow();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * 分块下载 02
     * 文件切分后分块下载, 下载完成后合并
     * @param url 下载链接
     */
    public void mergeDownload(String url) {
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

            // 切分任务
            ArrayList<Future> list = new ArrayList<>();
            split(url, list);

            countDownLatch.await();

            // 合并文件
            if (merge(httpFileName)){
                // 清除临时文件
                clearTemp(httpFileName);
            }
        } catch (Exception e){
            LogUtils.error("下载失败");
        } finally {
            System.out.print("\r");
            System.out.print("下载完成");
            // 关闭 http 连接
            if (httpURLConnection != null){
                httpURLConnection.disconnect();
            }

            // 关闭 scheduledExecutorService
            scheduledExecutorService.shutdown();

            // 关闭线程池
            poolExecutor.shutdown();
            try {
                // 等待 30 s如果还没有关 就强制停止
                poolExecutor.awaitTermination(30, TimeUnit.SECONDS);
                poolExecutor.shutdownNow();
            } catch (InterruptedException e) {
                LogUtils.error("关闭线程池异常{}",e);
            }
        }
    }

    /**
     * 文件切分
     * @param url
     * @param futureList
     */
    public void split(String url, ArrayList<Future> futureList){
        try {
            // 获取下载文件的大小
            long contentLength = HttpUtils.getHttpFileContentLength(url);

            // 计算切分后的文件大小
            long splitFileSize = contentLength / Constant.THREAD_NUM;

            // 计算分块个数
            for (int i = 0; i < Constant.THREAD_NUM; i++) {
                // 计算开始位置
                long startPos = i * splitFileSize;

                // 计算结束位置
                long endPos;

                if (1 == Constant.THREAD_NUM - 1){
                    // 下载最后一块，下载剩余的内容
                    endPos = 0;
                }else{
                    endPos = startPos + splitFileSize;
                }

                // 如果不是第一块，起始位置要加1
                if (startPos != 0){
                    startPos += 1;
                }

                // 创建任务对象
                DownloaderTask downloaderTask = new DownloaderTask(startPos, endPos, url,  i, countDownLatch);

                // 把任务提交到线程中
                Future<Boolean> future = poolExecutor.submit(downloaderTask);

                futureList.add(future);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 文件合并
     * @param fileName
     */
    public boolean merge(String fileName){
        LogUtils.info("开始合并文件{}", fileName);
        byte[] buffer = new byte[Constant.BUFFER_SIZE];

        int len = -1;
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(fileName, "rw")){
            for (int i = 0; i < Constant.THREAD_NUM; i++) {
                try(BufferedInputStream bis = new BufferedInputStream(new FileInputStream(fileName + Constant.SUFFIX + i))){
                    while ((len = bis.read(buffer)) != -1){
                        randomAccessFile.write(buffer, 0, len);
                    }
                }
            }
            LogUtils.info("文件合并完毕{}" + fileName);
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * 清除临时文件
     * @param fileName
     * @return
     */
    public boolean clearTemp(String fileName){
        for (int i = 0; i < Constant.THREAD_NUM; i++) {
            File file = new File(fileName + Constant.SUFFIX + i);
            file.delete();
        }
        return true;
    }


    /**
     * 最终的下载方法：文件分块后进行下载，下载完毕后合并
     * @param url 下载链接
     */
    public void download(String url) {
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

            // 切分任务
            ArrayList<Future> list = new ArrayList<>();
            split(url, list);

            countDownLatch.await();

            // 合并文件
            if (merge(httpFileName)){
                // 清除临时文件
                clearTemp(httpFileName);
            }
        } catch (Exception e){
            LogUtils.error("下载失败");
        } finally {
            System.out.print("\r");
            System.out.print("下载完成");
            // 关闭 http 连接
            if (httpURLConnection != null){
                httpURLConnection.disconnect();
            }

            // 关闭 scheduledExecutorService
            scheduledExecutorService.shutdown();

            // 关闭线程池
            poolExecutor.shutdown();
            try {
                // 等待 30 s如果还没有关 就强制停止
                poolExecutor.awaitTermination(30, TimeUnit.SECONDS);
                poolExecutor.shutdownNow();
            } catch (InterruptedException e) {
                LogUtils.error("关闭线程池异常{}",e);
            }
        }
    }
}
