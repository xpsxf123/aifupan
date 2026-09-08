package com.jiuyu.governance.common.utils;

import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 密码处理器
 * 提供 MD5 加密功能
 */
@Slf4j
public class PasswordHandler {

    private static final String MD5_ALGORITHM = "MD5";
    private static final String[] HEX_DIGITS = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f"};

    /**
     * 私有构造函数，防止实例化
     */
    private PasswordHandler() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }



    /**
     * 对原始密码进行 MD5 加密
     *
     * @param rawPassword 原始密码
     * @return MD5 加密后的 32 位小写十六进制字符串
     */
    public static String encode(String rawPassword) {
        return md5(rawPassword);
    }

    /**
     * 验证密码是否匹配
     *
     * @param rawPassword     原始密码
     * @param encodedPassword 加密后的密码
     * @return 如果匹配返回 true，否则返回 false
     */
    public static boolean matches(String rawPassword, String encodedPassword) {
        return md5(rawPassword).equals(encodedPassword);
    }



    /**
     * MD5 加密
     *
     * @param original 原始字符串
     * @return MD5 加密后的 32 位小写十六进制字符串，如果发生异常返回空字符串
     */
    private static String md5(String original) {
        if (original == null || original.isEmpty()) {
            return "";
        }
        try {
            MessageDigest md = MessageDigest.getInstance(MD5_ALGORITHM);
            byte[] digest = md.digest(original.getBytes(StandardCharsets.UTF_8));
            return byteArrayToHexString(digest);
        } catch (NoSuchAlgorithmException e) {
            // MD5 算法一定存在，这里不会抛出异常
            log.error("MD5 algorithm not found, this should never happen", e);
            return "";
        } catch (Exception e) {
            log.error("Error occurred during MD5 encryption, original={}", original, e);
            return "";
        }
    }

    /**
     * MD5 编码（支持指定字符集）
     *
     * @param origin      原始字符串
     * @param charsetName 字符集名称，为 null 或空时使用默认字符集
     * @return MD5 加密后的 32 位小写十六进制字符串，如果发生异常返回 null
     */
    private static String MD5Encode(String origin, String charsetName) {
        if (origin == null || origin.isEmpty()) {
            return null;
        }
        try {
            MessageDigest md = MessageDigest.getInstance(MD5_ALGORITHM);
            byte[] bytes;
            if (charsetName == null || charsetName.isEmpty()) {
                bytes = origin.getBytes(StandardCharsets.UTF_8);
            } else {
                bytes = origin.getBytes(charsetName);
            }
            byte[] digest = md.digest(bytes);
            return byteArrayToHexString(digest);
        } catch (Exception e) {
            log.error("Error occurred during MD5 encode, origin={}, charsetName={}", origin, charsetName, e);
            return null;
        }
    }

    /**
     * 将字节数组转换为 16 进制字符串
     *
     * @param bytes 字节数组
     * @return 16 进制字符串
     */
    private static String byteArrayToHexString(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        StringBuilder resultSb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            resultSb.append(byteToHexString(b));
        }
        return resultSb.toString();
    }

    /**
     * 将单个字节转换为 16 进制字符串
     *
     * @param b 字节
     * @return 16 进制字符串（2 个字符）
     */
    private static String byteToHexString(byte b) {
        int n = b & 0xFF;  // 使用位运算替代条件判断，性能更好
        int d1 = n >>> 4;  // 高 4 位
        int d2 = n & 0x0F; // 低 4 位
        return HEX_DIGITS[d1] + HEX_DIGITS[d2];
    }
}
