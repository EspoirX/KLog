package com.lzx.klog.util;

public class VersionUtil {


    public static int compare(String firstVersion, String secondVersion) {
        if (firstVersion == null || secondVersion == null) {
            return 0;
        }
        firstVersion = firstVersion.replace(" ", "");
        secondVersion = secondVersion.replace(" ", "");
        String[] fvs = firstVersion.split("\\.");
        String[] svs = secondVersion.split("\\.");
        if (fvs != null || svs != null) {
            int[] fvsI = strArr2IntArr(fvs);
            int[] svsI = strArr2IntArr(svs);
            int sizef = fvsI.length;
            int sizeS = svsI.length;
            int maxs = Math.max(sizef, sizeS);
            if (sizef > sizeS) {
                int[] news = new int[sizef];
                System.arraycopy(svsI, 0, news, 0, svsI.length);
                svsI = news;
            } else if (sizef < sizeS) {
                int[] news = new int[sizeS];
                System.arraycopy(fvsI, 0, news, 0, fvsI.length);
                fvsI = news;
            }
            for (int i = 0; i < maxs; i++) {
                int f = fvsI[i];
                int s = svsI[i];
                if (f > s) {
                    return 1;
                } else if (f < s) {
                    return -1;
                }
            }
        }
        return 0;
    }

    public static int compareTime(String firstTime, String secondTime) {
        if (firstTime == null || secondTime == null) {
            return 0;
        }
        firstTime = firstTime.replace(" ", "");
        secondTime = secondTime.replace(" ", "");
        String[] fvs = firstTime.split("\\_");
        String[] svs = secondTime.split("\\_");
        if (fvs != null || svs != null) {
            int[] fvsI = strArr2IntArr(fvs);
            int[] svsI = strArr2IntArr(svs);
            int sizef = fvsI.length;
            int sizeS = svsI.length;
            int maxs = Math.max(sizef, sizeS);
            if (sizef > sizeS) {
                int[] news = new int[sizef];
                System.arraycopy(svsI, 0, news, 0, svsI.length);
                svsI = news;
            } else if (sizef < sizeS) {
                int[] news = new int[sizeS];
                System.arraycopy(fvsI, 0, news, 0, fvsI.length);
                fvsI = news;
            }
            for (int i = 0; i < maxs; i++) {
                int f = fvsI[i];
                int s = svsI[i];
                if (f > s) {
                    return 1;
                } else if (f < s) {
                    return -1;
                }
            }
        }
        return 0;
    }

    private static int[] strArr2IntArr(String[] strs) {
        int size = strs.length;
        int[] iarr = new int[size];
        for (int i = 0; i < size; i++) {
            String s = strs[i];
            if (s != null && !s.isEmpty()) {
                int iv = Integer.parseInt(s);
                iarr[i] = iv;
            }

        }
        return iarr;
    }
}
