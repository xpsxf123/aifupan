package com.jiuyu.replay.common.utils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/11/26 10:14
 */
public class AESUtil {

    // 16字节密钥
    private static final String KEY = "1a42bc8d0536e97f";
    // 16字节IV
    private static final String IV = "ac3489ef1265bd70";

    /**
     * 加密
     *
     * @param plaintext 明文
     * @return 密文
     */
    public static String encrypt(String plaintext) {
        return encrypt(plaintext, KEY, IV);
    }

    /**
     * 加密
     *
     * @param plaintext 明文
     * @param key       密钥
     * @param iv        偏移量
     * @return 密文
     */
    public static String encrypt(String plaintext, String key, String iv) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(), "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(iv.getBytes());
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 解密
     *
     * @param ciphertext 密文
     * @return 明文
     */
    public static String decrypt(String ciphertext) {
        return decrypt(ciphertext, KEY, IV);
    }

    /**
     * 解密
     *
     * @param ciphertext 密文
     * @param key        密钥
     * @param iv         偏移量
     * @return 明文
     */
    public static String decrypt(String ciphertext, String key, String iv) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(), "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(iv.getBytes());
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            byte[] decoded = Base64.getDecoder().decode(ciphertext);
            return new String(cipher.doFinal(decoded), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
