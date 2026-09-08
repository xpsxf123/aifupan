package com.jiuyu.replay.common.utils;

/**
 * @author lujie
 * @description 版本号比较工具
 * @date 2025/5/24
 */
public class VersionUtil {

    /**
     * 比较两个版本号，去掉末尾字母和点号后逐段数值比较，段数多的版本更大
     *
     * @return 0: 相等, <0: v1 < v2, >0: v1 > v2
     */
    public static int compareVersion(String v1, String v2) {
        if (v1 == null || v1.isEmpty()) {
            return (v2 == null || v2.isEmpty()) ? 0 : -1;
        }
        if (v2 == null || v2.isEmpty()) {
            return 1;
        }
        // 去掉末尾的字母和点号，如 "2.3.6.test" → "2.3.6"、"2.5.8.8.yz" → "2.5.8.8"
        v1 = v1.replaceAll("[.a-zA-Z]+$", "");
        v2 = v2.replaceAll("[.a-zA-Z]+$", "");
        if (v1.isEmpty()) v1 = "0";
        if (v2.isEmpty()) v2 = "0";

        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");

        int minLen = Math.min(parts1.length, parts2.length);
        for (int i = 0; i < minLen; i++) {
            int n1 = Integer.parseInt(parts1[i]);
            int n2 = Integer.parseInt(parts2[i]);
            if (n1 != n2) return n1 - n2;
        }
        // 公共段相等时，段数多的版本更大，如 "2.3.1.0" > "2.3.1"
        return parts1.length - parts2.length;
    }
}
