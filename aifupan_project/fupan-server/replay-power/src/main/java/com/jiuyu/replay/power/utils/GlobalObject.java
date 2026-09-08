package com.jiuyu.replay.power.utils;


import com.jiuyu.replay.power.vo.UserCacheVo;

public class GlobalObject {

    private static ThreadLocal<UserCacheVo> LOCAL_USER = new ThreadLocal<>();

    public static UserCacheVo getLocalUser() {
        return LOCAL_USER.get();
    }

    public static void setLocalUser(UserCacheVo localUser) {
        LOCAL_USER.set(localUser);
    }

    public static void removeLocalUser() {
        LOCAL_USER.remove();
    }
}