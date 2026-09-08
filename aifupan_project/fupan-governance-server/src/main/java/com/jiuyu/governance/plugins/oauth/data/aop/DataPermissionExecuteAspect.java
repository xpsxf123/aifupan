package com.jiuyu.governance.plugins.oauth.data.aop;

import com.jiuyu.framework.lock.SpelParseHandler;
import com.jiuyu.framework.oauth.AccessUser;
import com.jiuyu.framework.oauth.client.UserHold;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.governance.common.exceptions.BusinessException;
import com.jiuyu.governance.common.pojo.ErrorCode;
import com.jiuyu.governance.common.pojo.SystemErrorCode;
import com.jiuyu.governance.common.pojo.BizErrorCode;
import com.jiuyu.governance.plugins.oauth.data.BeforePermission;
import com.jiuyu.governance.plugins.oauth.data.supports.DataPermissionsHandler;
import com.jiuyu.governance.plugins.oauth.pojo.OauthConstant;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.BeforeAdvice;
import org.springframework.aop.ClassFilter;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractPointcutAdvisor;
import org.springframework.aop.support.ComposablePointcut;
import org.springframework.aop.support.StaticMethodMatcher;
import org.springframework.aop.support.annotation.AnnotationMethodMatcher;

import java.io.Serial;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据权限执行切面
 * <p>
 * 本切面通过方法注解实现细粒度数据权限控制，在方法执行前进行权限校验。
 * 支持单个或多个权限维度校验，通过SpEL表达式动态解析数据标识。
 *
 * <h3>应用场景：</h3>
 * <ul>
 *   <li>需要方法级别数据权限控制的业务操作（如删除/修改敏感数据）</li>
 *   <li>需要动态解析数据ID的场景（如根据参数计算权限标识）</li>
 *   <li>需要组合多个权限维度校验的复杂业务场景</li>
 * </ul>
 *
 * <h3>执行流程：</h3>
 * <ol>
 *   <li>通过{@link BeforePermission}注解标记需要权限校验的方法</li>
 *   <li>使用{@link SpelParseHandler}解析注解中的SpEL表达式</li>
 *   <li>构建权限维度与数据ID的映射关系</li>
 *   <li>调用{@link DataPermissionsHandler}执行权限校验规则</li>
 *   <li>拦截校验失败请求并阻断执行</li>
 * </ol>
 *
 * <h3>关联核心类：</h3>
 * <table border="1">
 *   <tr><th>关联类</th><th>职责说明</th></tr>
 *   <tr><td>{@link BeforePermission}</td><td>权限校验注解，定义权限维度和数据ID表达式</td></tr>
 *   <tr><td>{@link DataPermissionsHandler}</td><td>权限处理核心类，执行具体校验逻辑</td></tr>
 *   <tr><td>{@link SpelParseHandler}</td><td>表达式解析器，动态计算数据ID值</td></tr>
 *   <tr><td>{@link com.jiuyu.framework.oauth.AccessUser}</td><td>用户身份凭证载体，提供权限校验所需用户信息</td></tr>
 * </table>
 *
 * <h3>参数说明：</h3>
 * <ul>
 *   <li><b>handler</b> - 数据权限处理器，包含具体校验规则实现</li>
 *   <li><b>parseHandler</b> - SpEL表达式解析器，支持从方法参数上下文解析动态值</li>
 * </ul>
 *
 * <h3>执行结果：</h3>
 * <ul>
 *   <li><b>权限校验通过</b> - 正常执行业务方法并返回结果</li>
 *   <li><b>权限校验失败</b> - 抛出RbacException阻断执行流程</li>
 *   <li><b>未登录用户</b> - 抛出AuthenticationException身份异常</li>
 * </ul>
 *
 * @author HeHui
 * @date 2025-03-27 13:45
 */
public class DataPermissionExecuteAspect extends AbstractPointcutAdvisor {

    @Serial
    private static final long serialVersionUID = 7293645240938107840L;

    private final Advice advice;

    private final Pointcut pointcut;

    public DataPermissionExecuteAspect(Advice advice) {
        this.advice = advice;
        this.pointcut = new ComposablePointcut(ClassFilter.TRUE, new PermissionMethodMatcher(List.of(BeforePermission.class, BeforePermission.Multiple.class)));
    }

    public DataPermissionExecuteAspect(DataPermissionsHandler handler, SpelParseHandler parseHandler) {
        this(new ExecuteAdvisor(handler, parseHandler));
    }


    @Override
    public Pointcut getPointcut() {
        return pointcut;
    }


    @Override
    public Advice getAdvice() {
        return advice;
    }


    /**
     * 权限方法匹配器（内部类）
     * <p>
     * 通过扫描方法注解{@link BeforePermission}及其组合注解{@link BeforePermission.Multiple}，
     * 确定需要应用权限校验切面的目标方法
     */
    private static class PermissionMethodMatcher extends StaticMethodMatcher {

        private final List<AnnotationMethodMatcher> methodMatchers;

        private PermissionMethodMatcher(List<Class<? extends Annotation>> methodAnnotationTypes) {
            this.methodMatchers = methodAnnotationTypes.stream().map(AnnotationMethodMatcher::new).collect(Collectors.toList());
        }

        /**
         * Perform static checking whether the given method matches.
         * <p>If this returns {@code false} or if the {@link #isRuntime()}
         * method returns {@code false}, no runtime check (i.e. no
         * {@link #matches(Method, Class, Object[])} call)
         * will be made.
         *
         * @param method      the candidate method
         * @param targetClass the target class
         *
         * @return whether or not this method matches statically
         */
        @Override
        public boolean matches(Method method, Class<?> targetClass) {
            return methodMatchers.stream().anyMatch(matcher -> matcher.matches(method, targetClass));
        }


        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof PermissionMethodMatcher)) {
                return false;
            }
            PermissionMethodMatcher otherMm = (PermissionMethodMatcher) obj;
            if (methodMatchers.size() != otherMm.methodMatchers.size()) {
                return false;
            }
            return new HashSet<>(otherMm.methodMatchers).containsAll(methodMatchers);
        }
    }


    /**
     * 权限执行增强器（内部类）
     * <p>
     * 具体执行权限校验逻辑，包含：
     * <ol>
     *   <li>注解参数解析</li>
     *   <li>用户凭证获取</li>
     *   <li>权限校验执行</li>
     *   <li>异常处理机制</li>
     * </ol>
     */
    @Slf4j
    private static class ExecuteAdvisor implements MethodInterceptor, BeforeAdvice {

        private final DataPermissionsHandler handler;

        private final SpelParseHandler parseHandler;

        public ExecuteAdvisor(DataPermissionsHandler handler, SpelParseHandler parseHandler) {
            this.handler = handler;
            this.parseHandler = parseHandler;
        }


        /**
         * 执行数据权限拦截处理
         * 从方法调用中提取数据权限信息，获取访问用户，并通过 handler.runBatch 执行权限校验和业务逻辑
         *
         * @param invocation 方法调用对象，包含方法信息和参数
         *
         * @return Object 业务方法的执行结果
         *
         * @throws Throwable 当权限校验或方法执行失败时抛出异常
         */
        @Override
        public Object invoke(MethodInvocation invocation) throws Throwable {
            // 提取数据权限参数映射
            Map<String, Object> permissionMap = extractDataPermissionMap(invocation);
            if (EmptyUtil.isEmpty(permissionMap)) {
                return invocation.proceed();
            }
            // 获取方法参数
            Object[] arguments = invocation.getArguments();
            AccessUser oauthClaim = null;
            // 从方法参数中查找 AccessUser 类型的用户信息
            for (Object argument : arguments) {
                if (argument instanceof AccessUser) {
                    oauthClaim = (AccessUser) argument;
                    break;
                }
            }
            // 如果参数中未找到用户信息，则从线程上下文中获取
            if (oauthClaim == null) {
                oauthClaim = UserHold.getRequiredAccessUser();
            }

            // 将包含单 ID 或集合 ID 的 Map 传递给 handler.runBatch
            return handler.runBatch(oauthClaim, permissionMap, () -> {
                try {
                    return invocation.proceed();
                } catch (Throwable e) {
                    if (e instanceof BusinessException be) {
                        throw be;
                    }
                    throw new RuntimeException(e.getMessage(), e);
                }
            });
        }


        /**
         * 从方法参数中提取数据权限参数
         *
         * @param invocation 方法调用对象
         *
         * @return Map<String, Object> 数据权限参数映射
         */
        private Map<String, Object> extractDataPermissionMap(MethodInvocation invocation) {
            BeforePermission single = invocation.getStaticPart().getAnnotation(BeforePermission.class);
            if (single != null) {
                // 直接使用 SpEL 解析为 Object，以支持集合和单个数值
                Object dataIdObj = null;
                if (single.collect()) {
                    dataIdObj = parseHandler.parse(single.dataId(), invocation.getMethod().getDeclaringClass(), invocation.getMethod(), invocation.getArguments(), null, Collection.class);
                } else {
                    dataIdObj = parseHandler.parse(single.dataId(), invocation.getMethod().getDeclaringClass(), invocation.getMethod(), invocation.getArguments(), null, Long.class);
                    if (dataIdObj instanceof Long l && l <= 0L) {
                        dataIdObj = null;
                    }
                }
                if (dataIdObj == null && !single.ignoreEmpty()) {
                    log.warn("[数据权限] BeforePermission 注解配置错误, 开启ignoreEmpty = false时,数据权限ID解析为空, 当前配置: {}, class: {}", single.dataId(), invocation.getMethod().toGenericString());
                    throw new BusinessException(SystemErrorCode.NOT_FOUND, "数据权限ID解析为空, [" + single.type() + " = " + single.type() + "]");
                }
                if (dataIdObj != null) {
                    return Map.of(single.type(), dataIdObj);
                }
            }
            BeforePermission.Multiple multiple = invocation.getStaticPart().getAnnotation(BeforePermission.Multiple.class);
            if (multiple == null || EmptyUtil.isEmpty(multiple.value())) {
                return null;
            }
            boolean enableLevel = multiple.enableLevel();

            Map<String, Object> dataPermissionMap = new HashMap<>(multiple.value().length, 1F);
            for (BeforePermission permission : multiple.value()) {
                // 直接使用 SpEL 解析为 Object，以支持集合和单个数值
                Object dataIdObj = null;
                if (permission.collect()) {
                    dataIdObj = parseHandler.parse(permission.dataId(), invocation.getMethod().getDeclaringClass(), invocation.getMethod(), invocation.getArguments(), null, Collection.class);
                } else {
                    dataIdObj = parseHandler.parse(permission.dataId(), invocation.getMethod().getDeclaringClass(), invocation.getMethod(), invocation.getArguments(), null, Long.class);
                    if (dataIdObj instanceof Long l && l <= 0L) {
                        dataIdObj = null;
                    }
                }
                if (dataIdObj == null && !permission.ignoreEmpty()) {
                    log.warn("[数据权限] BeforePermission 注解配置错误, 开启ignoreEmpty = false时,数据权限ID解析为空, 当前配置: {}, class: {}", permission.dataId(), invocation.getMethod().toGenericString());
                    throw new BusinessException(SystemErrorCode.NOT_FOUND, "数据权限ID解析为空, [" + permission.type() + " = " + permission.type() + "]");
                }
                if (dataIdObj != null) {
                    dataPermissionMap.put(permission.type(), dataIdObj);
                }
            }
            if (enableLevel && EmptyUtil.isNotEmpty(dataPermissionMap)) {
                Optional<Map.Entry<String, Object>> maxLevelOptional = dataPermissionMap.entrySet().stream().filter(entry -> EmptyUtil.isNotEmpty(entry.getKey()) && EmptyUtil.isNotEmpty(entry.getValue()))
                    .max(Comparator.comparingInt(entry -> OauthConstant.getDataPermissionsLevel(entry.getKey())));
                if (maxLevelOptional.isEmpty()) {
                    log.warn("[数据权限] BeforePermission.Multiple 注解配置错误, 开启enableLevel = true时,拿不到最大层级且不为空的数据, 当前配置: {}, class: {}", dataPermissionMap.keySet(), invocation.getMethod().toGenericString());
                    throw new BusinessException(BizErrorCode.PARAM_INVALID, "权限配置错误");
                }
                return Map.of(maxLevelOptional.get().getKey(), maxLevelOptional.get().getValue());
            }
            return dataPermissionMap;
        }
    }
}
