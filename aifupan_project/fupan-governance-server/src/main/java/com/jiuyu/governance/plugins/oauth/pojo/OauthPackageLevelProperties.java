package com.jiuyu.governance.plugins.oauth.pojo;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 *  认证授权套餐等级配置
 * @author HeHui
 * @date 2026-07-28 17:32
 */
@ConfigurationProperties(prefix = "jiuyu.oauth.client.package-level")
@Getter
@Setter
public class OauthPackageLevelProperties {


    /**
     * 是否启用
     */
    private Boolean enable = true;

    /**
     * 排除的路径
     */
    private List<String> excludePaths;
}
