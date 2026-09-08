package com.jiuyu.replay.common.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @author ：liwj
 * @description：关于流的utils
 * @date ：2025/8/29 18:46
 */
@Slf4j
public class JYStreamUtils {

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

        log.info("开始解析zip文件，字节数组大小: {}", zipBytes.length);

        try (ByteArrayInputStream bais = new ByteArrayInputStream(zipBytes); ZipInputStream zis = new ZipInputStream(bais)) {

            ZipEntry entry;
            // 遍历zip中的每个条目
            while ((entry = zis.getNextEntry()) != null) {
                String fileName = entry.getName();
                log.info("发现zip条目: {}", fileName);

                // 只处理.txt文件
                if (fileName.toLowerCase().endsWith(".txt")) {
                    log.info("开始读取txt文件: {}", fileName);

                    // 使用UTF-8编码直接读取
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(zis, StandardCharsets.UTF_8))) {
                        StringBuilder contentBuilder = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            contentBuilder.append(line).append(System.lineSeparator());
                        }
                        String content = contentBuilder.toString();
                        log.info("成功读取文件内容，长度: {}", content.length());
                        return content;
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
