package com.jiuyu.governance.common.utils;

import java.nio.charset.StandardCharsets;

/**
 * 非安全性 但速度超快的xxHash 哈希算法 比MD5快10倍
 *
 * @author HeHui
 * @date 2025-11-05 15:56
 */
public class XXHash {

    // 常量初始化
    private static final int PRIME32_1 = 0x9E3779B1;  // 2654435761
    private static final int PRIME32_2 = 0x85EBCA77;  // 2246822519
    private static final int PRIME32_3 = 0xC2B2AE3D;  // 3266489917
    private static final int PRIME32_4 = 0x27D4EB2F;  // 668265263
    private static final int PRIME32_5 = 0x165667B1;

    // 64位哈希常量
    private static final long PRIME64_1 = 0x9E3779B185EBCA87L;
    private static final long PRIME64_2 = 0xC2B2AE3D27D4EB4FL;
    private static final long PRIME64_3 = 0x165667B19E3779F9L;
    private static final long PRIME64_4 = 0x85EBCA77C2B2AE63L;
    private static final long PRIME64_5 = 0x27D4EB2F165667C5L;

    /**
     * 使用默认种子(0)计算32位哈希值
     *
     * @param data 输入数据
     *
     * @return 32位哈希值
     */
    public static int hash32(byte[] data) {
        return hash32(data, 0, data.length, 0);
    }

    /**
     * 使用指定种子计算32位哈希值
     *
     * @param data 输入数据
     * @param seed 哈希种子
     *
     * @return 32位哈希值
     */
    public static int hash32(byte[] data, int seed) {
        return hash32(data, 0, data.length, seed);
    }

    /**
     * 使用指定参数计算32位哈希值
     *
     * @param data   输入数据
     * @param offset 数据偏移量
     * @param length 数据长度
     * @param seed   哈希种子
     *
     * @return 32位哈希值
     */
    public static int hash32(byte[] data, int offset, int length, int seed) {
        int h32;
        int index = offset;
        int end = offset + length;

        // 处理16字节以上的数据块
        if (length >= 16) {
            int v1 = seed + PRIME32_1 + PRIME32_2;
            int v2 = seed + PRIME32_2;
            int v3 = seed;
            int v4 = seed - PRIME32_1;

            // 一次处理16字节的数据块（4个整数）
            int limit = end - 16;
            do {
                v1 += getInt(data, index) * PRIME32_2;
                v1 = Integer.rotateLeft(v1, 13);
                v1 *= PRIME32_1;
                index += 4;

                v2 += getInt(data, index) * PRIME32_2;
                v2 = Integer.rotateLeft(v2, 13);
                v2 *= PRIME32_1;
                index += 4;

                v3 += getInt(data, index) * PRIME32_2;
                v3 = Integer.rotateLeft(v3, 13);
                v3 *= PRIME32_1;
                index += 4;

                v4 += getInt(data, index) * PRIME32_2;
                v4 = Integer.rotateLeft(v4, 13);
                v4 *= PRIME32_1;
                index += 4;
            } while (index <= limit);

            // 合并4个状态值
            h32 = Integer.rotateLeft(v1, 1) +
                Integer.rotateLeft(v2, 7) +
                Integer.rotateLeft(v3, 12) +
                Integer.rotateLeft(v4, 18);
        } else {
            // 处理小于16字节的数据
            h32 = seed + PRIME32_5;
        }

        // 添加长度信息
        h32 += length;

        // 处理剩余的4字节块
        int limit = end - 4;
        while (index <= limit) {
            h32 += getInt(data, index) * PRIME32_3;
            h32 = Integer.rotateLeft(h32, 17) * PRIME32_4;
            index += 4;
        }

        // 处理最后的1-3个字节
        while (index < end) {
            h32 += (data[index] & 0xFF) * PRIME32_5;
            h32 = Integer.rotateLeft(h32, 11) * PRIME32_1;
            index++;
        }

        // 最终混合，提高雪崩效应
        h32 ^= h32 >>> 15;
        h32 *= PRIME32_2;
        h32 ^= h32 >>> 13;
        h32 *= PRIME32_3;
        h32 ^= h32 >>> 16;

        return h32;
    }

    /**
     * 使用默认种子(0)计算64位哈希值
     *
     * @param data 输入数据
     *
     * @return 64位哈希值
     */
    public static long hash64(byte[] data) {
        return hash64(data, 0, data.length, 0);
    }

    /**
     * 使用指定种子计算64位哈希值
     *
     * @param data 输入数据
     * @param seed 哈希种子
     *
     * @return 64位哈希值
     */
    public static long hash64(byte[] data, long seed) {
        return hash64(data, 0, data.length, seed);
    }

    /**
     * 使用指定参数计算64位哈希值
     *
     * @param data   输入数据
     * @param offset 数据偏移量
     * @param length 数据长度
     * @param seed   哈希种子
     *
     * @return 64位哈希值
     */
    public static long hash64(byte[] data, int offset, int length, long seed) {
        long h64;
        int index = offset;
        int end = offset + length;

        // 处理32字节以上的数据块
        if (length >= 32) {
            long v1 = seed + PRIME64_1 + PRIME64_2;
            long v2 = seed + PRIME64_2;
            long v3 = seed;
            long v4 = seed - PRIME64_1;

            // 一次处理32字节的数据块（4个长整数）
            int limit = end - 32;
            do {
                v1 += getLong(data, index) * PRIME64_2;
                v1 = Long.rotateLeft(v1, 31);
                v1 *= PRIME64_1;
                index += 8;

                v2 += getLong(data, index) * PRIME64_2;
                v2 = Long.rotateLeft(v2, 31);
                v2 *= PRIME64_1;
                index += 8;

                v3 += getLong(data, index) * PRIME64_2;
                v3 = Long.rotateLeft(v3, 31);
                v3 *= PRIME64_1;
                index += 8;

                v4 += getLong(data, index) * PRIME64_2;
                v4 = Long.rotateLeft(v4, 31);
                v4 *= PRIME64_1;
                index += 8;
            } while (index <= limit);

            // 合并4个状态值
            h64 = Long.rotateLeft(v1, 1) +
                Long.rotateLeft(v2, 7) +
                Long.rotateLeft(v3, 12) +
                Long.rotateLeft(v4, 18);

            v1 *= PRIME64_2;
            v1 = Long.rotateLeft(v1, 31);
            v1 *= PRIME64_1;
            h64 ^= v1;

            h64 = h64 * PRIME64_1 + PRIME64_4;

            v2 *= PRIME64_2;
            v2 = Long.rotateLeft(v2, 31);
            v2 *= PRIME64_1;
            h64 ^= v2;

            h64 = h64 * PRIME64_1 + PRIME64_4;

            v3 *= PRIME64_2;
            v3 = Long.rotateLeft(v3, 31);
            v3 *= PRIME64_1;
            h64 ^= v3;

            h64 = h64 * PRIME64_1 + PRIME64_4;

            v4 *= PRIME64_2;
            v4 = Long.rotateLeft(v4, 31);
            v4 *= PRIME64_1;
            h64 ^= v4;

            h64 = h64 * PRIME64_1 + PRIME64_4;
        } else {
            // 处理小于32字节的数据
            h64 = seed + PRIME64_5;
        }

        // 添加长度信息
        h64 += length;

        // 处理剩余的8字节块
        int limit = end - 8;
        while (index <= limit) {
            long k1 = getLong(data, index);
            k1 *= PRIME64_2;
            k1 = Long.rotateLeft(k1, 31);
            k1 *= PRIME64_1;
            h64 ^= k1;
            h64 = Long.rotateLeft(h64, 27) * PRIME64_1 + PRIME64_4;
            index += 8;
        }

        // 处理剩余的4字节块
        limit = end - 4;
        if (index <= limit) {
            long k1 = getInt(data, index) & 0xFFFFFFFFL;
            h64 ^= k1 * PRIME64_1;
            h64 = Long.rotateLeft(h64, 23) * PRIME64_2 + PRIME64_3;
            index += 4;
        }

        // 处理最后的1-3个字节
        while (index < end) {
            h64 ^= (data[index] & 0xFF) * PRIME64_5;
            h64 = Long.rotateLeft(h64, 11) * PRIME64_1;
            index++;
        }

        // 最终混合，提高雪崩效应
        h64 ^= h64 >>> 33;
        h64 *= PRIME64_2;
        h64 ^= h64 >>> 29;
        h64 *= PRIME64_3;
        h64 ^= h64 >>> 32;

        return h64;
    }

    /**
     * 对字符串进行哈希计算，使用默认种子(0)
     *
     * @param input 输入字符串
     *
     * @return 32位哈希值的十六进制字符串表示
     */
    public static String hasHex32(String input) {
        return hasHex32(input, 0);
    }

    /**
     * 对字符串进行哈希计算，使用指定种子
     *
     * @param input 输入字符串
     * @param seed  哈希种子
     *
     * @return 32位哈希值的十六进制字符串表示
     */
    public static String hasHex32(String input, int seed) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        byte[] data = input.getBytes(StandardCharsets.UTF_8);
        int hash = hash32(data, seed);
        return Integer.toHexString(hash);
    }

    /**
     * 对字符串进行64位哈希计算，使用默认种子(0)
     *
     * @param input 输入字符串
     *
     * @return 64位哈希值的十六进制字符串表示
     */
    public static String hashHex64(String input) {
        return hashHex64(input, 0L);
    }

    /**
     * 对字符串进行64位哈希计算，使用指定种子
     *
     * @param input 输入字符串
     * @param seed  哈希种子
     *
     * @return 64位哈希值的十六进制字符串表示
     */
    public static String hashHex64(String input, long seed) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        byte[] data = input.getBytes(StandardCharsets.UTF_8);
        long hash = hash64(data, seed);
        return Long.toHexString(hash);
    }

    /**
     * 从字节数组中获取一个32位整数（小端序）
     *
     * @param data  字节数组
     * @param index 索引位置
     *
     * @return 32位整数
     */
    private static int getInt(byte[] data, int index) {
        // 使用位移和或运算替代数组访问，提高性能
        return (data[index] & 0xFF) |
            ((data[index + 1] & 0xFF) << 8) |
            ((data[index + 2] & 0xFF) << 16) |
            ((data[index + 3] & 0xFF) << 24);
    }

    /**
     * 从字节数组中获取一个64位长整数（小端序）
     *
     * @param data  字节数组
     * @param index 索引位置
     *
     * @return 64位长整数
     */
    private static long getLong(byte[] data, int index) {
        // 使用位移和或运算替代数组访问，提高性能
        return ((long) data[index] & 0xFF) |
            (((long) data[index + 1] & 0xFF) << 8) |
            (((long) data[index + 2] & 0xFF) << 16) |
            (((long) data[index + 3] & 0xFF) << 24) |
            (((long) data[index + 4] & 0xFF) << 32) |
            (((long) data[index + 5] & 0xFF) << 40) |
            (((long) data[index + 6] & 0xFF) << 48) |
            (((long) data[index + 7] & 0xFF) << 56);
    }

//    public static void main(String[] args) {
//        int a = 100000;
//        String jwt = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI0NDgzNzU3NTEzODkzNDkwNjg4Iiwibmlja05hbWUiOiIxMjM0NTYxIiwiaXNzIjoid2VjaGF0LW9wZW4tY2hhbm5lbCIsImFjdGl2ZVRlbmFudElkIjowLCJ1c2VyVHlwZSI6MSwic2VjcmV0IjoiNjRlNTZhZDgtNTE5YS00MWE3LWJjMWItZDViNzlmNjZhMmYxIiwiZXhwIjoxNzYyMzI4MDU2LCJpcHMiOiIiLCJpYXQiOjE3NjIzMjc0NTYsInVzZXJuYW1lIjoidGRwIiwidG9rZW4iOiIyMjcyN2NhNWIzMTU0ZTk0YWUyNWI2MjhkYmE5YTA0ZiJ9.mg1gcVBJ-lOSDzhEiHsLXNwlx-BCG7GyWW-n-4bqffU";
//        BigDecimal base = BigDecimal.valueOf(1000000);
//        long init = System.nanoTime();
//        for (int i = 0; i < a; i++) {
//            long startTime = System.nanoTime();
//            String hashed = hashHex64(jwt);
//            long digestion = System.nanoTime() - startTime;
//            System.out.println("第"+i+"次 hash 耗时：" + BigDecimal.valueOf(digestion).divide(base, 4, RoundingMode.HALF_UP) + "ms, hex: " + hashed);
//        }
//        System.out.println(a + "次总耗时：" + BigDecimal.valueOf(System.nanoTime() - init).divide(base, 4, RoundingMode.HALF_UP) + "ms");
//    }
}
