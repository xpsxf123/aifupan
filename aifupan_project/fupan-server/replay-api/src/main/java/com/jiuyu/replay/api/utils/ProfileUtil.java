package com.jiuyu.replay.api.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * 获取当前环境
 *
 * @author RayChou
 * @date 2025/6/6 15:27
 */
@Component
public class ProfileUtil {

    private final Environment environment;

    @Autowired
    public ProfileUtil(Environment environment) {
        this.environment = environment;
    }

    public String[] getActiveProfiles() {
        return environment.getActiveProfiles();
    }

    public String getFirstActiveProfile() {
        String[] profiles = environment.getActiveProfiles();
        return profiles.length > 0 ? profiles[0] : "default";
    }

    public boolean isDev() {
        return environment.matchesProfiles("dev");
    }

    public boolean isTest() {
        return environment.matchesProfiles("test");
    }

    public boolean isProd() {
        return environment.matchesProfiles("prod");
    }
}