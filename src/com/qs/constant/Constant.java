package com.qs.constant;

import org.omg.CORBA.INTERNAL;

/**
 *  常量类
 */
public class Constant {
    /**
     * 文件下载路径
     */
    public static final String PATH = "H:\\mytest\\";

    /**
     * 下载单位
     */
    public static final Double MB = 1024d * 1024d;

    /**
     * 下载单位
     */
    public static final Double KB = 1024d;

    /**
     * 下载单位
     */
    public static final int BUFFER_SIZE = 1024 * 100;

    /**
     * 分块下载文件的后缀
     */
    public static final String SUFFIX = ".temp";

    /**
     * 切分文件的线程个数
     */
    public static int THREAD_NUM = 5;
}
