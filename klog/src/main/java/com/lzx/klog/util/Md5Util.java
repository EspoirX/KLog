package com.lzx.klog.util;

import java.security.MessageDigest;

public class Md5Util {
    public static String getMD5(String content) {
        if (content == null || content.isEmpty()) {
            return "";
        }
        try {
            MessageDigest sha1 = MessageDigest.getInstance("MD5");
            sha1.update(content.getBytes(), 0, content.getBytes().length);
            return toHexString(sha1.digest());
        } catch (Exception e) {
            return "";
        }
    }

    private static String toHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte aB : bytes) {
            sb.append(hexChar[(aB & 0xf0) >>> 4]);
            sb.append(hexChar[aB & 0xf]);
        }
        return sb.toString();
    }

    private static final char[] hexChar = {'0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
}
