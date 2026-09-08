package com.jiuyu.governance.plugins.oauth.data.aop;

import com.jiuyu.framework.shandard.ApiResponse;
import com.jiuyu.framework.shandard.NextCursor;
import com.jiuyu.framework.shandard.PageData;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.governance.plugins.oauth.data.DataPermissionsRequest;
import org.springframework.aop.ClassFilter;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.support.AopUtils;
import org.springframework.aop.support.ClassFilters;
import org.springframework.aop.support.StaticMethodMatcher;
import org.springframework.aop.support.annotation.AnnotationClassFilter;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * 数据权限查询切面动态匹配器
 *
 * <p>本类用于在AOP切面中动态判断是否需要为方法调用添加数据权限过滤条件。
 * 通过校验方法签名和参数特征，决定是否触发数据权限控制逻辑。</p>
 *
 * <h3>典型应用场景：</h3>
 * <ol>
 *   <li>需要根据用户权限动态过滤查询结果的Service方法</li>
 *   <li>包含DataPermissionsRequest参数的分页/列表查询方法</li>
 *   <li>返回标准分页结构（PageData/ApiResponse等）的查询方法</li>
 * </ol>
 *
 *
 * @author HeHui
 * @date 2025-03-27
 *
 * @see MethodMatcher#matches(Method, Class, Object...)
 *
 * <h3>执行结果判断逻辑：</h3>
 * <table border="1">
 *   <tr><th>条件</th><th>结果</th></tr>
 *   <tr><td>方法无参数</td><td>❌ 不匹配</td></tr>
 *   <tr><td>返回类型不在支持列表中</td><td>❌ 不匹配</td></tr>
 *   <tr><td>类名匹配排除正则</td><td>❌ 不匹配</td></tr>
 *   <tr><td>参数包含DataPermissionsRequest实例</td><td>✅ 匹配</td></tr>
 * </table>
 */
public class DataPermissionsQueryPredicate extends StaticMethodMatcher {


    private final List<Class<?>> supportReturnTypes = new ArrayList<>();

    private final String ignoreClassRegular;

    private final List<String> basePackages;

    private final ClassFilter classFilter = ClassFilters.union(new AnnotationClassFilter(Service.class), new AnnotationClassFilter(Component.class));


    private final ExecuteStaticMethodMatcher executeStaticMethodMatcher = new ExecuteStaticMethodMatcher();
    /**
     * 数据权限查询谓词
     *
     * @param ignoreClassRegular  类名排除正则表达式（匹配的类将跳过切面处理）
     * @param basePackages        基本包路径列表（可空）
     * @param customerReturnTypes 自定义支持的返回类型扩展列表（可空）
     */
    public DataPermissionsQueryPredicate(String ignoreClassRegular, List<String> basePackages ,List<Class<?>> customerReturnTypes) {
        this.ignoreClassRegular = ignoreClassRegular;
        initSupportReturn();
        if (EmptyUtil.isNotEmpty(customerReturnTypes)) {
            supportReturnTypes.addAll(customerReturnTypes);
        }
        this.basePackages = basePackages;
    }

    /**
     * 默认支持的返回类型
     */
    private void initSupportReturn() {
        supportReturnTypes.add(Void.class);
        supportReturnTypes.add(List.class);
        supportReturnTypes.add(Set.class);
        supportReturnTypes.add(Map.class);
        supportReturnTypes.add(Optional.class);
        supportReturnTypes.add(PageData.class);
        supportReturnTypes.add(ApiResponse.class);
        supportReturnTypes.add(NextCursor.class);
    }


    /**
     * 匹配
     *
     * @param method      方法
     * @param targetClass 目标类
     *
     * @return boolean
     */
    @Override
    public boolean matches(Method method, Class<?> targetClass) {
        return classFilter.matches(targetClass) && executeStaticMethodMatcher.matches(method, targetClass);
    }


    /**
     * Indicates whether some other object is "equal to" this one.
     * <p>
     * The {@code equals} method implements an equivalence relation
     * on non-null object references:
     * <ul>
     * <li>It is <i>reflexive</i>: for any non-null reference value
     *     {@code x}, {@code x.equals(x)} should return
     *     {@code true}.
     * <li>It is <i>symmetric</i>: for any non-null reference values
     *     {@code x} and {@code y}, {@code x.equals(y)}
     *     should return {@code true} if and only if
     *     {@code y.equals(x)} returns {@code true}.
     * <li>It is <i>transitive</i>: for any non-null reference values
     *     {@code x}, {@code y}, and {@code z}, if
     *     {@code x.equals(y)} returns {@code true} and
     *     {@code y.equals(z)} returns {@code true}, then
     *     {@code x.equals(z)} should return {@code true}.
     * <li>It is <i>consistent</i>: for any non-null reference values
     *     {@code x} and {@code y}, multiple invocations of
     *     {@code x.equals(y)} consistently return {@code true}
     *     or consistently return {@code false}, provided no
     *     information used in {@code equals} comparisons on the
     *     objects is modified.
     * <li>For any non-null reference value {@code x},
     *     {@code x.equals(null)} should return {@code false}.
     * </ul>
     * <p>
     * The {@code equals} method for class {@code Object} implements
     * the most discriminating possible equivalence relation on objects;
     * that is, for any non-null reference values {@code x} and
     * {@code y}, this method returns {@code true} if and only
     * if {@code x} and {@code y} refer to the same object
     * ({@code x == y} has the value {@code true}).
     * <p>
     * Note that it is generally necessary to override the {@code hashCode}
     * method whenever this method is overridden, so as to maintain the
     * general contract for the {@code hashCode} method, which states
     * that equal objects must have equal hash codes.
     *
     * @param obj the reference object with which to compare.
     *
     * @return {@code true} if this object is the same as the obj
     *     argument; {@code false} otherwise.
     *
     * @see #hashCode()
     * @see HashMap
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof DataPermissionsQueryPredicate)) {
            return false;
        }
        DataPermissionsQueryPredicate otherMm = (DataPermissionsQueryPredicate) obj;
        if (classFilter != otherMm.classFilter || executeStaticMethodMatcher != otherMm.executeStaticMethodMatcher) {
            return false;
        }
        return true;
    }

    private class ExecuteStaticMethodMatcher extends StaticMethodMatcher {

        /**
         * 执行动态方法匹配判断
         *
         * <p>该方法在运行时确定是否对目标方法应用数据权限过滤切面逻辑。通过以下顺序的条件校验进行判断：</p>
         *
         * <h3>参数应用场景：</h3>
         * <ul>
         *   <li><b>method</b> - 需要检查的候选方法对象，用于获取方法签名信息（返回类型、参数等）</li>
         *   <li><b>targetClass</b> - 方法所属的目标类，用于类名模式匹配检查</li>
         *   <li><b>args</b> - 方法调用时的实际参数列表，用于检测数据权限请求对象</li>
         * </ul>
         *
         * <h3>执行结果判断逻辑：</h3>
         * <table border="1">
         *   <tr><th>序号</th><th>检查条件</th><th>结果</th><th>说明</th></tr>
         *   <tr><td>1</td><td>方法无参数(args.length == 0)</td><td>❌ 不匹配</td><td>没有参数的方法不进行权限过滤</td></tr>
         *   <tr><td>2</td><td>方法返回类型不在支持列表中</td><td>❌ 不匹配</td><td>仅支持：Void/Optional/PageData/ApiResponse/NextCursor及其子类</td></tr>
         *   <tr><td>3</td><td>类名匹配ignoreClassRegular正则表达式</td><td>❌ 不匹配</td><td>通过正则表达式排除特定类</td></tr>
         *   <tr><td>4</td><td>参数包含DataPermissionsRequest实例</td><td>✅ 匹配</td><td>检测到权限请求参数时启用过滤</td></tr>
         * </table>
         *
         * @param method      待检查的目标方法对象（非空）
         * @param targetClass 方法所属的类对象（非空）
         *
         * @return boolean 匹配结果：
         *                <ul>
         *                  <li>true - 需要应用数据权限过滤逻辑</li>
         *                  <li>false - 跳过权限过滤处理</li>
         *                </ul>
         *
         * @see #supportReturnTypes
         * @see #ignoreClassRegular
         */
        @Override
        public boolean matches(Method method, Class<?> targetClass) {
            if (EmptyUtil.isEmpty(basePackages)) {
                return false;
            }
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes == null) {
                return false;
            }
            if (parameterTypes.length == 0) {
                return false;
            }
            if (basePackages.stream().noneMatch(base -> targetClass.getPackageName().startsWith(base))) {
                return false;
            }
            if (Arrays.stream(parameterTypes).noneMatch(a -> {
                if (Modifier.isFinal(a.getModifiers())) {
                    return false;
                }
                if (AopUtils.isAopProxy(a)) {
                    return false;
                }
                return DataPermissionsRequest.class.isAssignableFrom(a);
            })) {
                return false;
            }
            if (EmptyUtil.isNotEmpty(ignoreClassRegular) && targetClass.getCanonicalName().matches(ignoreClassRegular)) {
                return false;
            }
            Class<?> returnType = method.getReturnType();
            if (Modifier.isFinal(returnType.getModifiers())) {
                return false;
            }
            return supportReturnTypes.stream().anyMatch(t -> t.isAssignableFrom(returnType));
        }
    }
}
