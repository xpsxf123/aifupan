package com.jiuyu.replay.power.enumeration;

public enum LoginSourceEnum {

    CLIENT("client", "客户端"),
    WEB("web", "web端"),
    BACK("back", "后台");

    private final String loginSourceValue;
    private final String loginSourceLabel;

    LoginSourceEnum(String loginSourceValue, String loginSourceLabel) {
        this.loginSourceValue = loginSourceValue;
        this.loginSourceLabel = loginSourceLabel;
    }

    public String getLoginSourceValue() {
        return loginSourceValue;
    }

    public String getLoginSourceLabel() {
        return loginSourceLabel;
    }
}
