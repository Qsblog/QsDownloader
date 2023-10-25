package com.qs.util;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.locks.ReentrantLock;

/*
    http相关工具类
 */
public class HttpUtils {


    public static long getHttpFileContentLength(String url) throws IOException {
        HttpURLConnection httpURLConnection = null;
        int contentLength = 0;
        try {
            httpURLConnection = getHttpURLConnection(url);
            contentLength = httpURLConnection.getContentLength();
        } finally {
            if (httpURLConnection != null){
                httpURLConnection.disconnect();
            }
        }
        return contentLength;
    }

    /**
     * 分块下载
     * @param url 下载地址
     * @param startPos 下载文件起始位置
     * @param endPos 下载文件结束位置
     * @return
     * @throws IOException
     */
    public static HttpURLConnection getHttpURLConnection(String url, long startPos, long endPos) throws IOException {
        HttpURLConnection httpURLConnection = getHttpURLConnection(url);
        LogUtils.info("下载的区间是：{}-{}",startPos,endPos);

        if (endPos != 0){
            httpURLConnection.setRequestProperty("Range", "bytes=" + startPos + "-" + endPos);
        }else{
            httpURLConnection.setRequestProperty("Range", "bytes=" + startPos + "-");
        }
        return httpURLConnection;
    }

    /**
     * 获取链接
     * @param url
     * @return
     * @throws IOException
     */
    public static HttpURLConnection getHttpURLConnection(String url) throws IOException {
        URL httpUrl = new URL(url);
        HttpURLConnection httpURLConnection = (HttpURLConnection) httpUrl.openConnection();
        httpURLConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.1 (KHTML, like Gecko) Chrome/14.0.835.163 Safari/535.1");
        return httpURLConnection;
    }


    /**
     * 获取文件名
     * @param url
     * @return
     */
    public static String getFileName(String url) {
        return url.substring(url.lastIndexOf("/") + 1);
    }
}
