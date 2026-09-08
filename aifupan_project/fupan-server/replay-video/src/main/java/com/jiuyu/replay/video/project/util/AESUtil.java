package com.jiuyu.replay.video.project.util;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * AES加密解密工具类
 *
 * @author AI Assistant
 * @since 2025-11-25
 */
public class AESUtil {

    /**
     * AES 加密 (CBC模式, PKCS7填充, 128位密钥)
     *
     * @param data 待加密的明文
     * @param key  密钥（文本，会自动处理为128位）
     * @return Base64编码的加密结果
     */
    public static String encrypt(String data, String key) {
        return encrypt(data, key, key);
    }

    /**
     * AES 加密 (CBC模式, PKCS7填充, 128位密钥)
     *
     * @param data 待加密的明文
     * @param key  密钥（文本，会自动处理为128位）
     * @param iv   偏移量（文本，128位）
     * @return Base64编码的加密结果
     */
    public static String encrypt(String data, String key, String iv) {
        try {
            // 生成密钥和偏移量
            SecretKeySpec secretKey = generateKey(key);
            IvParameterSpec ivSpec = generateIv(iv);

            // 创建密码器并初始化
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding"); // PKCS5Padding 对应 PKCS7
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);

            // 执行加密
            byte[] encryptedBytes = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));

            // 返回Base64编码结果
            return Base64.getEncoder().encodeToString(encryptedBytes);

        } catch (Exception e) {
            throw new RuntimeException("AES加密失败", e);
        }
    }

    /**
     * AES 解密 (CBC模式, PKCS7填充, 128位密钥)
     *
     * @param encryptedData Base64编码的加密数据
     * @param key           密钥（文本，会自动处理为128位）
     * @param iv            偏移量（文本，128位）
     * @return 解密后的原始文本
     */
    public static String decrypt(String encryptedData, String key, String iv) {
        try {
            // 生成密钥和偏移量
            SecretKeySpec secretKey = generateKey(key);
            IvParameterSpec ivSpec = generateIv(iv);

            // 创建密码器并初始化
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);

            // Base64解码并执行解密
            byte[] decodedBytes = Base64.getDecoder().decode(encryptedData);
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);

            // 返回解密后的字符串
            return new String(decryptedBytes, StandardCharsets.UTF_8);

        } catch (Exception e) {
            throw new RuntimeException("AES解密失败", e);
        }
    }

    /**
     * 生成128位AES密钥
     * 将任意长度文本处理为128位(16字节)
     */
    private static SecretKeySpec generateKey(String key) {
        try {
            byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
            byte[] aesKey = new byte[16]; // 128位 = 16字节

            // 密钥处理：如果长度不够则补0，如果超长则截断
            if (keyBytes.length < 16) {
                System.arraycopy(keyBytes, 0, aesKey, 0, keyBytes.length);
                // 剩余部分保持为0
            } else {
                System.arraycopy(keyBytes, 0, aesKey, 0, 16);
            }

            return new SecretKeySpec(aesKey, "AES");

        } catch (Exception e) {
            throw new RuntimeException("生成AES密钥失败", e);
        }
    }

    /**
     * 生成128位偏移量(IV)
     * 将任意长度文本处理为128位(16字节)
     */
    private static IvParameterSpec generateIv(String iv) {
        try {
            byte[] ivBytes = iv.getBytes(StandardCharsets.UTF_8);
            byte[] aesIv = new byte[16]; // 128位 = 16字节

            // IV处理：如果长度不够则补0，如果超长则截断
            if (ivBytes.length < 16) {
                System.arraycopy(ivBytes, 0, aesIv, 0, ivBytes.length);
                // 剩余部分保持为0
            } else {
                System.arraycopy(ivBytes, 0, aesIv, 0, 16);
            }

            return new IvParameterSpec(aesIv);

        } catch (Exception e) {
            throw new RuntimeException("生成偏移量失败", e);
        }
    }

    /**
     * 测试方法
     */
    public static void main(String[] args) {
        // 测试数据
        String originalText = "Hello, AES加密测试!";
        String key = "MySecretKey123";   // 密钥
        String iv = "MySecretKey123";  // 偏移量（推荐16字节）

        System.out.println("原文: " + originalText);
        System.out.println("密钥: " + key);
        System.out.println("偏移量: " + iv);

        // 加密
        String encrypted = encrypt(originalText, key, iv);
        System.out.println("加密后: " + encrypted);

        // 解密
        String decrypted = decrypt(encrypted, key, iv);
        System.out.println("解密后: " + decrypted);

        // 验证
        System.out.println("加解密成功: " + originalText.equals(decrypted));
    }

    /**
     * 测试不同长度的密钥和偏移量
     */
    private static void testVariousLengths() {
        System.out.println("\n=== 测试不同长度的密钥和IV ===");

        String testText = "测试文本";

        // 测试用例：不同长度的密钥和IV
        String[][] testCases = {
                {"short", "shortIv"},           // 短密钥短IV
                {"16byteKey!!!!!!", "16byteIv!!!"}, // 正好16字节
                {"this-is-a-very-long-key-that-exceeds-16-bytes", "long-iv-too"} // 超长
        };

        for (String[] testCase : testCases) {
            String key = testCase[0];
            String iv = testCase[1];

            try {
                String encrypted = encrypt(testText, key, iv);
                String decrypted = decrypt(encrypted, key, iv);

                System.out.println("密钥: '" + key + "' | IV: '" + iv + "'");
                System.out.println("加密结果: " + encrypted);
                System.out.println("解密结果: " + decrypted);
                System.out.println("成功: " + testText.equals(decrypted));
                System.out.println("---");

            } catch (Exception e) {
                System.out.println("失败 - 密钥: '" + key + "' | 错误: " + e.getMessage());
            }
        }
    }
}

