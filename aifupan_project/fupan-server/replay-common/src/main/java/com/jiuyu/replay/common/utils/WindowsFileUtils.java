package com.jiuyu.replay.common.utils;

import org.springframework.util.StringUtils;

import java.util.UUID;
import java.util.regex.Pattern;

public class WindowsFileUtils {

    /**
     * 将文件名称的违规符号去掉
     * @param folderName 文件名称
     * @return
     */
    public static String sanitizeForFolderName(String folderName) {
        if (StringUtils.isEmpty(folderName))
        {
            // 名称为空，改成随机uuid字符串
            return UUID.randomUUID().toString().replaceAll("-", "");
        }
        // 定义 Windows 非法字符（即使运行在 Linux 仍按 Windows 规则处理）
        String invalidChars = "\\/:\"*?<>|";
        Pattern illegalPattern = Pattern.compile("[" + Pattern.quote(invalidChars) + "]");
        String sanitized = illegalPattern.matcher(folderName).replaceAll("");

        // 处理保留名称（Windows 设备名称）
        Pattern reservedPattern = Pattern.compile(
                "^\\s*(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(\\..*)?\\s*$",
                Pattern.CASE_INSENSITIVE
        );
        if (reservedPattern.matcher(sanitized).matches()) {
            sanitized += "_";
        }

        // 清理首尾空格和点
        sanitized = sanitized
                .replaceAll("^[\\s.]+", "")  // 去除开头空格和点
                .replaceAll("[\\s.]+$", ""); // 去除结尾空格和点

        // 处理连续点号
        sanitized = sanitized.replaceAll("\\.{2,}", ".");

        if(StringUtils.isEmpty(sanitized) || sanitized.length() > 255)
        {
            // 去掉特殊符号后名称为空，改成随机uuid字符串
            sanitized = UUID.randomUUID().toString().replaceAll("-", "");
        }

        return sanitized;
    }
}
