package com.jiuyu.governance.plugins.oauth.data.supports;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 数据权限参数配置
 *
 * @author HeHui
 * @date 2026-03-19 12:20
 */
@Getter
@Setter
@ConfigurationProperties(prefix = DataPermissionsParameter.PREFIX)
public class DataPermissionsParameter {

    public static final String PREFIX = "jiuyu.oauth.data-permissions";

    /** 查询时处理数据权限 */
    private Boolean enableQuery = true;

    /** 自定义支持返回类型 */
    private List<Class<?>> returnTypes;

    /** 忽略类名的规则 */
    private String ignoreClassRegular;

    /** 基本包路径 */
    private List<String> basePackages = List.of("com.jiuyu.governance");
}
