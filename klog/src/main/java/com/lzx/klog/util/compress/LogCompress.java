package com.lzx.klog.util.compress;

import java.io.File;


public interface LogCompress {
    void compress(File file) throws Exception;

    void decompress(File file) throws Exception;
}
