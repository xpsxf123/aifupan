package com.jiuyu.replay.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author lj
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AgentQueryUserCheck {

    /**
     * 锁的key（支持Spring EL表达式，不需要传#{}边界符）
     * 例如：key = "#args[0]"
     * 校验checkUserId，如果登录的用户可以查看这个用户就通过，没权限就不给通过
     *
     * @return 返回要校验的userId，类型要是long，如果是null就可以过
     */
    String checkUserId() default "";

}
