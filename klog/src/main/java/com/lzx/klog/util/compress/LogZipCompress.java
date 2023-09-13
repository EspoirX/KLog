package com.lzx.klog.util.compress;



import com.lzx.klog.util.SafeIoUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;
import java.util.zip.CheckedOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;


public class LogZipCompress implements LogCompress {
    private static LogZipCompress mInstance;

    private LogZipCompress() {
    }

    public synchronized static LogZipCompress getInstance() {
        if (mInstance == null) {
            mInstance = new LogZipCompress();
        }
        return mInstance;
    }

    public static final String EXT = ".zip";
    private static final String BASE_DIR = "";

    // 符号"/"用来作为目录标识判断符
    private static final String PATH = "/";
    private static final int BUFFER = 1024;

    /**
     * 压缩
     *
     * @param srcFile
     * @throws Exception
     */
    @Override
    public void compress(File srcFile) throws Exception {
        String name = srcFile.getName();
        String basePath = srcFile.getParent();
        String destPath = basePath + File.separator + name.substring(0, name.lastIndexOf(".")) + EXT;
        compress(srcFile, destPath);
    }

    /**
     * 压缩
     *
     * @param srcFile  源文件
     * @param destFile 目标文件
     * @throws Exception
     */
    public void compress(File srcFile, File destFile) throws Exception {

        // 对输出文件做CRC32校验
        CheckedOutputStream cos = new CheckedOutputStream(new FileOutputStream(destFile), new CRC32());

        ZipOutputStream zos = new ZipOutputStream(cos);

        compress(srcFile, zos, BASE_DIR);

        zos.flush();
        zos.close();
    }

    /**
     * 压缩文件
     *
     * @param srcFile
     * @param destPath
     * @throws Exception
     */
    public void compress(File srcFile, String destPath) throws Exception {
        compress(srcFile, new File(destPath));
    }

    /**
     * 压缩
     *
     * @param srcFile  源路径
     * @param zos      ZipOutputStream
     * @param basePath 压缩包内相对路径
     * @throws Exception
     */
    private void compress(File srcFile, ZipOutputStream zos, String basePath) throws Exception {
        if (srcFile.isDirectory()) {
            compressDir(srcFile, zos, basePath);
        } else {
            compressFile(srcFile, zos, basePath);
        }
    }

    /**
     * 压缩
     *
     * @param srcPath
     * @throws Exception
     */
    public void compress(String srcPath) throws Exception {
        File srcFile = new File(srcPath);

        compress(srcFile);
    }

    /**
     * 文件压缩
     *
     * @param srcPath  源文件路径
     * @param destPath 目标文件路径
     */
    public void compress(String srcPath, String destPath) throws Exception {
        File srcFile = new File(srcPath);

        compress(srcFile, destPath);
    }

    /**
     * 压缩目录
     *
     * @param dir
     * @param zos
     * @param basePath
     * @throws Exception
     */
    private void compressDir(File dir, ZipOutputStream zos, String basePath) throws Exception {

        File[] files = dir.listFiles();

        // 构建空目录
        if (files.length < 1) {
            ZipEntry entry = new ZipEntry(basePath + dir.getName() + PATH);

            zos.putNextEntry(entry);
            zos.closeEntry();

            for (File file : files) {

                // 递归压缩
                compress(file, zos, basePath + dir.getName() + PATH);

            }
        }
    }

    /**
     * 文件压缩
     *
     * @param file 待压缩文件
     * @param zos  ZipOutputStream
     * @param dir  压缩文件中的当前路径
     * @throws Exception
     */
    private void compressFile(File file, ZipOutputStream zos, String dir) throws Exception {

        /**
         * 压缩包内文件名定义
         *
         * <pre>
         * 如果有多级目录，那么这里就需要给出包含目录的文件名
         * 如果用WinRAR打开压缩包，中文名将显示为乱码
         * </pre>
         */
        ZipEntry entry = new ZipEntry(dir + file.getName());

        zos.putNextEntry(entry);

        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));

        int count;
        byte[] data = new byte[BUFFER];
        while ((count = bis.read(data, 0, BUFFER)) != -1) {
            zos.write(data, 0, count);
        }
        bis.close();

        zos.closeEntry();
    }

    /**
     * 解压缩
     *
     * @param srcFile
     * @throws Exception
     */
    public void decompress(File srcFile) throws Exception {
        String basePath = srcFile.getParent() + File.separator;
        decompress(srcFile, basePath);
    }

    /**
     * 解压缩
     *
     * @param srcFile
     * @param destFile
     * @throws Exception
     */
    public void decompress(File srcFile, File destFile) throws Exception {

        CheckedInputStream cis = new CheckedInputStream(new FileInputStream(srcFile), new CRC32());

        ZipInputStream zis = new ZipInputStream(cis);
        decompress(destFile, zis);


        zis.close();

    }

    /**
     * 解压缩
     *
     * @param srcFile
     * @param destPath
     * @throws Exception
     */
    public void decompress(File srcFile, String destPath) throws Exception {
        decompress(srcFile, new File(destPath));

    }

    /**
     * 文件 解压缩
     *
     * @param destFile 目标文件
     * @param zis      ZipInputStream
     * @throws Exception
     */
    private void decompress(File destFile, ZipInputStream zis) throws Exception {

        ZipEntry entry = null;
        while ((entry = zis.getNextEntry()) != null) {

            if (entry.getName().contains("../")) {
                continue;
            }
            // 文件
            String dir = destFile.getPath() + File.separator + entry.getName();

            File dirFile = new File(dir);

            // 文件检查
            fileProber(dirFile);

            if (entry.isDirectory()) {
                dirFile.mkdirs();
            } else {
                decompressFile(dirFile, zis);
            }
            zis.closeEntry();
        }
    }

    /**
     * 文件 解压缩
     *
     * @param srcPath 源文件路径
     * @throws Exception
     */
    public void decompress(String srcPath) throws Exception {
        File srcFile = new File(srcPath);

        decompress(srcFile);
    }

    /**
     * 文件 解压缩
     *
     * @param srcPath  源文件路径
     * @param destPath 目标文件路径
     * @throws Exception
     */
    public void decompress(String srcPath, String destPath) throws Exception {

        File srcFile = new File(srcPath);
        decompress(srcFile, destPath);
    }

    /**
     * 文件解压缩
     *
     * @param destFile 目标文件
     * @param zis      ZipInputStream
     * @throws Exception
     */
    private void decompressFile(File destFile, ZipInputStream zis) throws Exception {

        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(destFile));

        int count;
        byte[] data = new byte[BUFFER];
        while ((count = zis.read(data, 0, BUFFER)) != -1) {
            bos.write(data, 0, count);
        }

        bos.write(data);

        bos.close();
    }

    /**
     * 文件探针
     * <p>
     * <pre>
     * 当父目录不存在时，创建目录！
     * </pre>
     *
     * @param dirFile
     */
    private void fileProber(File dirFile) {

        File parentFile = dirFile.getParentFile();
        if (!parentFile.exists()) {

            // 递归寻找上级目录
            fileProber(parentFile);

            parentFile.mkdir();
        }

    }

    private static final int BUFF_SIZE = 1024 * 1024;

    public static void upZipFile(File zipFile, String folderPath, OnUnZipListener listener) {
        ZipFile zf = null;
        InputStream in = null;
        OutputStream out = null;
        try {
            File desDir = new File(folderPath);
            if (!desDir.exists()) {
                desDir.mkdirs();
            }
            zf = new ZipFile(zipFile);
            for (Enumeration<?> entries = zf.entries(); entries.hasMoreElements(); ) {
                ZipEntry entry = ((ZipEntry) entries.nextElement());
                if (entry.isDirectory()) {
                    String name = entry.getName();
                    if (name.contains("../")) {
                        continue;
                    }
                    name = name.substring(0, name.length() - 1);
                    File createDirectory = new File(folderPath + File.separator + name);
                    createDirectory.mkdirs();
                }
                in = zf.getInputStream(entry);
                String str = folderPath + File.separator + entry.getName();
                str = new String(str.getBytes(), "utf-8");
                File desFile = new File(str);
                if (!desFile.exists()) {
                    File fileParentDir = desFile.getParentFile();
                    if (!fileParentDir.exists()) {
                        fileParentDir.mkdirs();
                    }
                    desFile.createNewFile();
                }
                out = new FileOutputStream(desFile);
                byte[] buffer = new byte[BUFF_SIZE];
                int realLength;
                while ((realLength = in.read(buffer)) > 0) {
                    out.write(buffer, 0, realLength);
                }
            }
            listener.onUnZipSuccess();
        } catch (Exception ex) {
            ex.printStackTrace();
            listener.onUnZipError(zipFile);
        } finally {
            SafeIoUtils.safeClose(zf, in, out);
        }
    }

    public interface OnUnZipListener {
        void onUnZipSuccess();

        void onUnZipError(File srcFile);
    }

}
