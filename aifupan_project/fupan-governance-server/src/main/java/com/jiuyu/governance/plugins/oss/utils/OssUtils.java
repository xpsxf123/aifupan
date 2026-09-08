package com.jiuyu.governance.plugins.oss.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * @author ：lujie
 * @description：
 * @date ：2026/3/20 17:57
 */
@Slf4j
public class OssUtils {


    /**
     * 将文本内容压缩为zip字节数组（内部为data.txt）
     *
     * @param content 文本内容
     * @return zip文件的字节数组
     */
    public static byte[] compressToZipBytes(String content) {
        if (content == null || content.isEmpty()) {
            return new byte[0];
        }
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {
            ZipEntry entry = new ZipEntry("data.txt");
            zos.putNextEntry(entry);
            zos.write(content.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
            zos.finish();
            return baos.toByteArray();
        } catch (IOException e) {
            log.error("压缩为zip失败", e);
            return new byte[0];
        }
    }

    /**
     * 从zip字节数组中提取文本内容（仅处理UTF-8编码的txt文件）
     *
     * @param zipBytes zip文件的字节数组
     * @return 提取的文本内容
     */
    public static String extractTextFromZipBytes(byte[] zipBytes) {
        if (zipBytes == null || zipBytes.length == 0) {
            log.warn("zip字节数组为空");
            return "";
        }
        try (ByteArrayInputStream bais = new ByteArrayInputStream(zipBytes); ZipInputStream zis = new ZipInputStream(bais)) {

            ZipEntry entry;
            // 遍历zip中的每个条目
            while ((entry = zis.getNextEntry()) != null) {
                String fileName = entry.getName();

                // 只处理.txt文件
                if (fileName.toLowerCase().endsWith(".txt")) {

                    // 使用UTF-8编码直接读取
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(zis, StandardCharsets.UTF_8))) {
                        StringBuilder contentBuilder = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            contentBuilder.append(line).append(System.lineSeparator());
                        }
                        return contentBuilder.toString();
                    }
                }
                zis.closeEntry();
            }

            log.warn("zip文件中未找到txt文件");
            return "";

        } catch (IOException e) {
            log.error("从字节流解压zip文件失败", e);
            return "";
        }
    }

}
