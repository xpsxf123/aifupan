package com.jiuyu.governance.plugins.oauth.data;

import com.jiuyu.governance.plugins.oauth.data.aop.DataPermissionExecuteAspect;
import com.jiuyu.governance.plugins.oauth.pojo.OauthConstant;
import org.aopalliance.intercept.MethodInvocation;

import java.lang.annotation.*;

/**
 * 执行前验证数据权限
 * @author HeHui
 * @date 2025-03-27 13:35
 */
@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface BeforePermission {

    /**
     * 数据权限类型
     *
     * @return {@link String }
     */
    String type();

    /**
     * 数据ID 支持表达式
     *
     * @return {@link String }
     */
    String dataId();


    /**
     * 是否为集合验证
     *
     * @return {@link boolean }
     */
    boolean collect() default false;


    /**
     * 忽略空数据权限验证
     *
     * @return {@link boolean }
     */
    boolean ignoreEmpty() default false;


    /**
     * 多组数据权限验证
     *
     * @author HeHui
     * @date 2025/03/27
     */
    @Documented
    @Target({ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @interface Multiple {

        /**
         * 是否为层级验证
         *  true的情况下时只判断level最大且 dataId 不为空的数据
         * @see OauthConstant#getDataPermissionsLevel(String)
         * @see DataPermissionExecuteAspect
         *
         * @return {@link boolean }
         */
        boolean enableLevel() default false;

        /**
         * 每组的验证
         *
         * @return {@link BeforePermission[] }
         */
        BeforePermission[] value();
    }
}
