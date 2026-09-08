package com.jiuyu.replay.generic.constant.activity;

/**
 * 邀请奖励规则code枚举
 *
 * @author RayChou
 * @date 2025/6/3 14:32
 */
public enum InviteRewardRuleCodeEnum {

    REGISTER("REGISTER", "注册爱复盘"), DOWNLOAD("DOWNLOAD", "下载爱复盘"), USE("USE", "使用爱复盘"), AI("AI", "使用AI复盘");

    private String code;

    private String name;

    InviteRewardRuleCodeEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }


    /**
     * 通过code获取name
     *
     * @param code 编码
     * @return 名称
     */
    public static String getNameByCode(String code) {
        if (code == null) {
            return null;
        }

        for (InviteRewardRuleCodeEnum item : InviteRewardRuleCodeEnum.values()) {
            if (item.getCode().equals(code)) {
                return item.getName();
            }
        }

        return null;
    }

    /**
     * 通过code获取枚举
     *
     * @param code 编码
     * @return 枚举对象
     */
    public static InviteRewardRuleCodeEnum getByCode(String code) {
        if (code == null) {
            return null;
        }

        for (InviteRewardRuleCodeEnum item : InviteRewardRuleCodeEnum.values()) {
            if (item.getCode().equals(code)) {
                return item;
            }
        }

        return null;
    }
}
