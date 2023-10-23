package com.qs;

import com.qs.core.Downloader;
import com.qs.util.LogUtils;

import java.util.Scanner;

/*
    主类
 */
public class Main {
    public static void main(String[] args) {
        //下载地址
        String url = null;
        if (args == null || args.length == 0) {
            do {
                LogUtils.info("请输入下载地址:");
                Scanner scanner = new Scanner(System.in);
                url = scanner.next();
            } while (url == null);
        }else {
            url = args[0];
        }
        // 测试下载地址 ：
        // https://dlcdn.apache.org/tomcat/tomcat-10/v10.1.15/bin/apache-tomcat-10.1.15-windows-x64.zip
        Downloader downloader = new Downloader();
//        downloader.normalDownload(url);
        downloader.scheduledExecutorDownload(url);
    }
}
