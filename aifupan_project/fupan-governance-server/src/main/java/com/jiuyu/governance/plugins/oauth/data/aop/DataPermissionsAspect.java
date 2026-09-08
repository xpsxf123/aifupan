package com.jiuyu.governance.plugins.oauth.data.aop;

import com.jiuyu.governance.plugins.oauth.data.supports.DataPermissionsHandler;
import com.jiuyu.governance.plugins.oauth.data.DataPermissionsRequest;
import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.AfterReturningAdvice;
import org.springframework.aop.BeforeAdvice;
import org.springframework.aop.Pointcut;
import org.springframework.aop.ThrowsAdvice;
import org.springframework.aop.support.AbstractPointcutAdvisor;
import org.springframework.aop.support.ComposablePointcut;

/**
 * 数据权限查询切面
 * <p>
 * 本切面通过AOP机制实现数据权限的动态过滤，将权限校验逻辑与业务代码解耦。
 * 主要作用于包含{@link DataPermissionsRequest}参数的查询方法，
 * 根据用户权限动态修改查询条件或过滤返回结果。
 *
 * <h3>应用场景：</h3>
 * <ul>
 *   <li>需要根据用户权限动态过滤查询结果的Service方法</li>
 *   <li>包含分页查询参数且需要权限控制的数据列表接口</li>
 *   <li>返回标准数据结构（PageData/ApiResponse等）的查询方法</li>
 * </ul>
 *
 * <h3>执行流程：</h3>
 * <ol>
 *   <li>通过{@link DataPermissionsQueryPredicate}匹配需要拦截的方法</li>
 *   <li>使用{@link DataPermissionsQueryAdvisor}执行具体权限校验逻辑</li>
 *   <li>根据校验结果决定是否修改查询参数或过滤返回数据</li>
 * </ol>
 *
 * <h3>关联核心类：</h3>
 * <table border="1">
 *   <tr><th>关联类</th><th>职责说明</th></tr>
 *   <tr><td>{@link DataPermissionsQueryAdvisor}</td><td>具体权限校验逻辑实现，处理方法拦截后的业务逻辑</td></tr>
 *   <tr><td>{@link DataPermissionsQueryPredicate}</td><td>动态方法匹配器，确定需要应用切面的方法</td></tr>
 *   <tr><td>{@link DataPermissionsHandler}</td><td>权限处理核心类，执行具体的权限校验规则</td></tr>
 *   <tr><td>{@link DataPermissionsRequest}</td><td>数据权限请求载体，包含权限维度等元数据</td></tr>
 * </table>
 *
 * @author HeHui
 * @date 2025-03-27 14:09
 */
public class DataPermissionsAspect extends AbstractPointcutAdvisor {


    private static final long serialVersionUID = 8208587131172341131L;

    private final DataPermissionsQueryAdvisor advisor;

    private final Pointcut pointcut;

    public DataPermissionsAspect(DataPermissionsQueryAdvisor advisor, DataPermissionsQueryPredicate predicate) {
        this.advisor = advisor;
        this.pointcut = new ComposablePointcut(predicate);
    }

    /**
     * Get the Pointcut that drives this advisor.
     */
    @Override
    public Pointcut getPointcut() {
        return pointcut;
    }

    /**
     * Return the advice part of this aspect. An advice may be an
     * interceptor, a before advice, a throws advice, etc.
     *
     * @return the advice that should apply if the pointcut matches
     *
     * @see MethodInterceptor
     * @see BeforeAdvice
     * @see ThrowsAdvice
     * @see AfterReturningAdvice
     */
    @Override
    public Advice getAdvice() {
        return advisor;
    }
}
