package com.jiuyu.governance.plugins.oauth.data.aop;

import com.jiuyu.framework.oauth.AccessUser;
import com.jiuyu.framework.oauth.client.UserHold;
import com.jiuyu.framework.shandard.ApiResponse;
import com.jiuyu.framework.shandard.NextCursor;
import com.jiuyu.framework.shandard.PageData;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.governance.common.exceptions.BusinessException;
import com.jiuyu.governance.common.pojo.ErrorCode;
import com.jiuyu.governance.common.pojo.SystemErrorCode;
import com.jiuyu.governance.common.pojo.BizErrorCode;
import com.jiuyu.governance.plugins.oauth.data.supports.DataPermissionsHandler;
import com.jiuyu.governance.plugins.oauth.data.DataPermissionsRequest;
import jakarta.validation.constraints.NotNull;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.BeforeAdvice;

import java.io.Serializable;
import java.util.*;
import java.util.function.Supplier;

/**
 * 数据权限查询切面处理器
 *
 * <p>本类实现AOP拦截逻辑，对需要数据权限控制的查询方法进行动态过滤处理。根据用户权限信息，
 * 在方法执行前后添加数据权限校验逻辑，并控制最终返回结果。</p>
 *
 * <h3>典型应用场景：</h3>
 * <ul>
 *   <li>需要根据用户角色动态过滤查询结果的Service方法</li>
 *   <li>包含OauthClaim（用户凭证）和DataPermissionsRequest（权限请求）参数的查询方法</li>
 *   <li>返回标准数据结构（PageData/ApiResponse等）的分页/列表查询</li>
 * </ul>
 *
 * @author HeHui
 * @date 2025-03-27
 *
 *     <h3>核心执行逻辑：</h3>
 *     <pre>
 *     1. 提取方法参数中的权限相关对象
 *     2. 检查用户权限有效性
 *     3. 根据权限校验结果：
 *        ├─ 有权限 → 执行原始方法
 *        └─ 无权限 → 返回预设默认值
 *     </pre>
 * @see DataPermissionsHandler 数据权限处理核心接口
 * @see MethodInterceptor AOP方法拦截器接口
 * @see DataPermissionsQueryPredicate 数据权限查询切面匹配器
 */
public class DataPermissionsQueryAdvisor implements MethodInterceptor, BeforeAdvice, Serializable {


    private static final long serialVersionUID = 2721673887135272910L;

    private static final Map<Class<?>, Supplier<Object>> defaultValueMap = new HashMap<>();

    private final DataPermissionsHandler handler;


    /**
     * 数据权限查询顾问
     *
     * @param customerDefaultValueMap 自定义返回值映射（可扩展支持更多返回类型）
     *                                示例：
     *                                {@code
     *                                new HashMap<Class<?>, Supplier<Object>>() {{
     *                                put(CustomResult.class, CustomResult::empty);
     *                                }}
     *                                }
     */
    public DataPermissionsQueryAdvisor(Map<Class<?>, Supplier<Object>> customerDefaultValueMap, DataPermissionsHandler handler) {
        this.handler = handler;
        init();
        if (EmptyUtil.isNotEmpty(customerDefaultValueMap)) {
            defaultValueMap.putAll(customerDefaultValueMap);
        }
    }


    /**
     * 初始化默认返回值映射
     * <p>预置常见返回类型的默认值生成逻辑：</p>
     * <table border="1">
     *   <tr><th>返回类型</th><th>默认值</th></tr>
     *   <tr><td>Void</td><td>null</td></tr>
     *   <tr><td>Optional</td><td>Optional.empty()</td></tr>
     *   <tr><td>PageData</td><td>空分页对象</td></tr>
     *   <tr><td>ApiResponse</td><td>失败响应("无数据权限")</td></tr>
     *   <tr><td>NextCursor</td><td>结束游标</td></tr>
     * </table>
     */
    private void init() {
        defaultValueMap.put(Void.class, () -> {
            return null;
        });
        defaultValueMap.put(Optional.class, Optional::empty);
        defaultValueMap.put(List.class, List::of);
        defaultValueMap.put(Set.class, Set::of);
        defaultValueMap.put(Map.class, Map::of);
        defaultValueMap.put(PageData.class, PageData::empty);
        defaultValueMap.put(ApiResponse.class, () -> ApiResponse.failed(BizErrorCode.NO_POWER.getCode(), "无数据权限"));
        defaultValueMap.put(NextCursor.class, NextCursor::over);
    }


    /**
     * AOP拦截处理方法
     *
     * <h3>执行流程：</h3>
     * <ol>
     *   <li>解析方法参数：
     *     <ul>
     *       <li>提取OauthClaim（用户凭证）</li>
     *       <li>提取DataPermissionsRequest（权限请求）</li>
     *     </ul>
     *   </li>
     *   <li>获取匹配的默认值生成器：
     *     <ul>
     *       <li>根据方法返回类型获取预设默认值</li>
     *       <li>未匹配类型时抛出RbacException</li>
     *     </ul>
     *   </li>
     *   <li>权限校验处理：
     *     <ul>
     *       <li>无权限请求参数 → 直接返回默认值</li>
     *       <li>有用户凭证 → 带凭证校验权限</li>
     *       <li>无用户凭证 → 匿名方式校验权限</li>
     *     </ul>
     *   </li>
     * </ol>
     *
     * @param invocation 方法调用上下文（包含目标方法、参数等信息）
     *
     * @return Object 处理结果：
     *     <ul>
     *       <li>权限通过 → 原始方法执行结果</li>
     *       <li>权限拒绝 → 类型对应的默认值</li>
     *     </ul>
     *
     * @throws Throwable 可能抛出以下异常：
     *                   <ul>
     *                     <li>RbacException：无权限且无匹配默认值时</li>
     *                     <li>RuntimeException：方法执行过程中的封装异常</li>
     *                   </ul>
     */
    @Override
    public Object invoke(@NotNull MethodInvocation invocation) throws Throwable {
        Object[] arguments = invocation.getArguments();
        AccessUser oauthClaim = null;
        DataPermissionsRequest request = null;
        // 提取权限相关参数
        for (Object argument : arguments) {
            if (argument instanceof AccessUser) {
                oauthClaim = (AccessUser) argument;
                continue;
            }
            if (argument instanceof DataPermissionsRequest) {
                request = (DataPermissionsRequest) argument;
            }
        }
        // 没有权限请求参数时，直接返回默认值
        Supplier<Object> defaultReturn = defaultValueMap.getOrDefault(invocation.getMethod().getReturnType(), () -> {
            throw new BusinessException(BizErrorCode.NO_POWER, "无数据权限");
        });
        if (request == null) {
            return defaultReturn.get();
        }
        // 没有登录信息时，尝试获取登录信息
        if (oauthClaim == null) {
            Optional<AccessUser> claimOptional = UserHold.getAccessUser();
            // 没有登录信息时，直接返回默认值
            if (claimOptional.isEmpty()) {
                return defaultReturn.get();
            }
            oauthClaim = claimOptional.get();
        }

        // 调用权限处理器，处理权限校验逻辑
        return handler.apply(oauthClaim, request, r -> {
            try {
                return invocation.proceed();
            } catch (Throwable e) {
                if (r instanceof BusinessException) {
                    throw (BusinessException) e;
                }
                throw new BusinessException(BizErrorCode.GENERAL_FAILED, "查询失败", e);
            }
        }, defaultReturn);
    }
}
