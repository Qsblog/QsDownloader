package com.qs.core;

import com.qs.constant.Constant;

/**
 * 下载信息
 */
public class DownloadInfo implements Runnable{
    
    // 下载文件总大小
    private long httpFileContentLength;
    
    // 本地已下载文件的大小
    private double finishedSize;
    
    // 本次累计下载的大小
    private double downSize;
    
    // 前一次下载的大小
    private double preSize;

    public DownloadInfo(long httpFileContentLength) {
        this.httpFileContentLength = httpFileContentLength;
    }

    public long getHttpFileContentLength() {
        return httpFileContentLength;
    }

    public void setHttpFileContentLength(long httpFileContentLength) {
        this.httpFileContentLength = httpFileContentLength;
    }

    public double getFinishedSize() {
        return finishedSize;
    }

    public void setFinishedSize(double finishedSize) {
        this.finishedSize = finishedSize;
    }

    public double getDownSize() {
        return downSize;
    }

    public void setDownSize(double downSize) {
        this.downSize = downSize;
    }

    public double getPreSize() {
        return preSize;
    }

    public void setPreSize(double preSize) {
        this.preSize = preSize;
    }

    @Override
    public void run() {
        // 计算文件总大小 单位：mb
        String httpFileSize = String.format("%.2f", httpFileContentLength / Constant.MB);

        // 计算每秒下载速度 kb
        double speedDouble = (downSize - preSize) / 1024d;

        // 判断每秒下载速度是否大鱼 1000 kb / s
        int speed =  (int) speedDouble;
        boolean speedGt1000 = speedDouble > 1000d;
        if (speedGt1000){
            // 大于1000 以MB / s 计时
            speed = (int) (speedDouble / 1024d);
        }
        
        preSize = downSize;
        
        // 剩余文件的大小
        double remainSize = httpFileContentLength - finishedSize - downSize;
        
        // 剩余下载时间
        String remainTime = String.format("%.2f", remainSize / (speedGt1000 ? Constant.MB : Constant.KB) / speed);

        // 已下载大小
        String currentFileSize = String.format("%.2f",(downSize - finishedSize) / Constant.MB);

        String downInfo = String.format("已下载 %smb/%smb，速度 %s%s/s,剩余时间 %ss",
                currentFileSize,httpFileSize,speed,speedGt1000 ? "MB" : "KB",remainTime);

        System.out.print("\r");
        System.out.print(downInfo);
    }
}
