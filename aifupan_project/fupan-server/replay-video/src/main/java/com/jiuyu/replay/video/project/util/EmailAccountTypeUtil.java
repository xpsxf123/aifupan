package com.jiuyu.replay.video.project.util;

import cn.hutool.core.util.StrUtil;

/**
 * 邮箱账号类型工具类
 *
 * @author RayChou
 * @date 2025-11-28
 * @description 邮箱账号类型转换工具类，提供账号类型与名称的相互转换
 */
public class EmailAccountTypeUtil {

    /**
     * 解析邮箱类型名称为类型编码
     *
     * @param accountTypeStr 账号类型字符串
     * @return 账号类型编码：1-Gmail 2-Outlook 3-QQ邮箱 4-163邮箱 5-其他
     */
    public static Integer parseAccountType(String accountTypeStr) {
        if (StrUtil.isBlank(accountTypeStr)) {
            return 5;
        }

        accountTypeStr = accountTypeStr.trim();
        if ("Gmail".equalsIgnoreCase(accountTypeStr) || "1".equals(accountTypeStr)) {
            return 1;
        } else if ("Outlook".equalsIgnoreCase(accountTypeStr) || "2".equals(accountTypeStr)) {
            return 2;
        } else if ("QQ邮箱".equals(accountTypeStr) || "3".equals(accountTypeStr)) {
            return 3;
        } else if ("163邮箱".equals(accountTypeStr) || "4".equals(accountTypeStr)) {
            return 4;
        } else if ("其他".equals(accountTypeStr) || "5".equals(accountTypeStr)) {
            return 5;
        }

        return null;
    }

    /**
     * 根据账号类型编码获取类型名称
     *
     * @param accountType 账号类型编码
     * @return 账号类型名称
     */
    public static String getAccountTypeName(Integer accountType) {
        if (accountType == null) {
            return "其他";
        }

        return switch (accountType) {
            case 1 -> "Gmail";
            case 2 -> "Outlook";
            case 3 -> "QQ邮箱";
            case 4 -> "163邮箱";
            case 5 -> "其他";
            default -> "其他";
        };
    }
}

