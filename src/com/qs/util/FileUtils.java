package com.qs.util;

import java.io.File;

public class FileUtils {

    public static long getFileContentLength(String httpFileName) {
        File file = new File(httpFileName);
        return file.exists() && file.exists() ? file.length() : 0;
    }
}
