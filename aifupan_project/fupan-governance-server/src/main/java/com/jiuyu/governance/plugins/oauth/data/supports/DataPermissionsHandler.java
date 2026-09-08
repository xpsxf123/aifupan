package com.jiuyu.governance.plugins.oauth.data.supports;

import com.jiuyu.framework.oauth.AccessUser;
import com.jiuyu.framework.oauth.client.UserHold;
import com.jiuyu.framework.oauth.exceptions.AuthenticationException;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.framework.util.FunctionUtil;
import com.jiuyu.governance.common.exceptions.BusinessException;
import com.jiuyu.governance.common.pojo.ErrorCode;
import com.jiuyu.governance.common.pojo.SystemErrorCode;
import com.jiuyu.governance.common.pojo.BizErrorCode;
import com.jiuyu.governance.business.org.service.impl.ManagerConnectorProcessor;
import com.jiuyu.governance.plugins.oauth.data.BusinessPermissions;
import com.jiuyu.governance.plugins.oauth.data.DataPermissionsRequest;
import com.jiuyu.governance.plugins.oauth.data.provider.UserPermissionProvider;
import com.jiuyu.governance.plugins.oauth.pojo.OauthConstant;
import org.springframework.beans.factory.ObjectProvider;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 数据权限处理器
 * <p>
 * 该类提供了多种方法来处理数据权限，确保用户只能访问其有权限的数据。
 * 通过使用不同的方法，可以根据不同的场景和需求来检查和应用数据权限。
 * <p>
 * <strong>使用示例：</strong>
 * <p>
 * <strong>示例1：单维度数据权限检查</strong>
 * <pre>
 * {@code
 * DataPermissionsHandler handler = new DataPermissionsHandler(userService, userDataFunction);
 * String dataPermissionTypeDimension = "department";
 * Supplier<Data> businessFunction = () -> fetchDataFromDatabase();
 * Function<Data, String> getDataId = Data::getId;
 *
 * try {
 *     Data result = handler.afterCheck(dataPermissionTypeDimension, businessFunction, getDataId);
 *     // 处理结果
 * } catch (AuthenticationException | RbacException e) {
 *     // 处理异常
 * }
 * }
 *
 * </pre>
 * <p>
 * <strong>示例2：多维度数据权限检查</strong>
 * <pre>
 * {@code
 * DataPermissionsHandler handler = new DataPermissionsHandler(userService, userDataFunction);
 * String dataPermissionTypeDimension = "department";
 * Supplier<Data> businessFunction = () -> fetchDataFromDatabase();
 * Function<Data, String> getDataId = Data::getId;
 *
 * try {
 *     Data result = handler.afterCheck(dataPermissionTypeDimension, businessFunction, getDataId);
 *     // 处理结果
 * } catch (AuthenticationException | RbacException e) {
 *     // 处理异常
 * }
 * }
 * </pre>
 * <p>
 * <strong>示例3：执行带有数据权限控制的业务逻辑</strong>
 * <pre>
 * {@code
 * DataPermissionsHandler handler = new DataPermissionsHandler(userService, userDataFunction);
 * String dataPermissionTypeDimension = "department";
 * String dataId = "12345";
 * Actuator actuator = () -> {
 *     // 执行具体的业务逻辑
 * };
 *
 * try {
 *     handler.run(dataPermissionTypeDimension, dataId, actuator);
 * } catch (AuthenticationException | RbacException e) {
 *     // 处理异常
 * }
 * }
 * </pre>
 * <p>
 * <strong>示例4：根据用户权限处理数据请求</strong>
 * <pre>
 * {@code
 * DataPermissionsHandler handler = new DataPermissionsHandler(userService, userDataFunction);
 * DataPermissionsRequest request = new DataPermissionsRequest();
 * request.setSubCompanyIds(List.of("1", "2", "3"));
 * Function<DataPermissionsRequest, Result> businessFunction = req -> processDataRequest(req);
 * Supplier<Result> notDataSupplier = () -> new DefaultResult();
 *
 * Result result = handler.apply(request, businessFunction, notDataSupplier);
 * // 处理结果
 * }
 * </pre>
 *
 * @author HeHui
 * @date 2025-03-24 14:35
 */
public class DataPermissionsHandler {

    private final Long all = 0L;

    private static DataPermissionsHandler INSTANCE;

    private final BusinessPermissionsCombination businessPermissions;

    private final List<UserPermissionProvider> permissionProviders;
    private final ManagerConnectorProcessor connectorProcessor;

    public DataPermissionsHandler(ObjectProvider<BusinessPermissions> businessPermissionsProvider,
                                  ObjectProvider<UserPermissionProvider> permissionProvidersProvider,
                                  ManagerConnectorProcessor connectorProcessor) {
        INSTANCE = this;
        this.businessPermissions = new BusinessPermissionsCombination(businessPermissionsProvider.orderedStream().collect(Collectors.toMap(BusinessPermissions::support, Function.identity(), FunctionUtil::mergeFirst)));
        this.permissionProviders = permissionProvidersProvider.orderedStream().collect(Collectors.toList());
        this.connectorProcessor = connectorProcessor;
    }

    /**
     * 获取实例
     *
     * @return {@link DataPermissionsHandler }
     */
    public static DataPermissionsHandler getInstance() {
        return INSTANCE;
    }


    /**
     * 忽略权限检查执行某段代码
     * 此方法用于在执行特定代码块时临时忽略权限检查它通过使用ThreadLocal来实现这一点，
     * 确保在执行给定的Supplier函数时，权限检查被禁用，并在执行完毕后恢复原始的权限检查状态
     *
     * @param <T>   Supplier函数返回的类型
     * @param other 一个Supplier函数，代表需要在忽略权限检查的情况下执行的代码块
     *
     * @return T 执行Supplier函数后的结果
     */
    public <T> T ignore(Supplier<T> other) {
        return PermissionContextHolder.runWithIgnore(other);
    }


    /**
     * 忽略给定的Runnable任务的执行结果
     *
     * @param other 要执行的Runnable任务
     */
    public void ignore(Runnable other) {
        PermissionContextHolder.runWithIgnore(other);
    }

    /**
     * 在检查数据权限后执行业务逻辑，并根据数据权限类型维度对数据进行后检查
     * 此方法首先验证用户是否已登录，然后执行业务功能获取信息，并确保获取的信息存在
     * 接着根据用户权限和数据权限类型维度检查用户是否有权查看该数据
     * 如果用户未登录、数据不存在或用户无权查看数据，则抛出相应的异常
     *
     * @param dataPermissionTypeDimension 数据权限类型维度，用于确定数据权限的维度
     * @param businessFunction            业务逻辑的Supplier，用于执行业务逻辑并获取数据
     * @param getDataId                   从业务数据中提取数据ID的函数，用于获取数据的唯一标识
     * @param <T>                         业务数据的类型
     *
     * @return 执行业务逻辑后获取的数据
     *
     * @throws AuthenticationException 如果用户未登录，则抛出此异常
     * @throws BusinessException       如果数据不存在或用户无权查看数据，则抛出此异常
     */
    public <T> T afterCheck(String dataPermissionTypeDimension, Supplier<T> businessFunction, Function<T, Long> getDataId) {
        return UserHold.getAccessUser().map(oauthClaim -> {
            T info = businessFunction.get();
            if (info == null) {
                return null;
            }
            return afterCheck(oauthClaim, dataPermissionTypeDimension, info, getDataId, () -> {
                throw new BusinessException(SystemErrorCode.CONFLICT, "无法查看该数据");
            }, t -> t);
        }).orElseThrow(() -> new AuthenticationException("用户未登录"));
    }

    /**
     * 根据数据权限类型维度检查信息，并返回处理后的信息
     * 此方法用于在验证OAuth声明有效后，进一步检查数据权限，并根据检查结果返回相应信息
     * 如果数据不存在，将抛出RbacException异常
     *
     * @param <T>                         期望返回的信息类型
     * @param oauthClaim                  OAuth声明对象，包含用户认证信息
     * @param dataPermissionTypeDimension 数据权限类型维度，用于确定数据权限的范围
     * @param info                        要检查的信息对象
     * @param getDataId                   函数式接口，用于从info中提取数据ID
     *
     * @return <T> 返回处理后的信息对象
     *
     * @throws BusinessException 如果数据不存在或无权限，抛出此异常
     */
    public <T> T afterCheck(AccessUser oauthClaim, String dataPermissionTypeDimension, T info, Function<T, Long> getDataId) {
        return afterCheck(oauthClaim, dataPermissionTypeDimension, info, getDataId, () -> {
            throw new BusinessException(SystemErrorCode.CONFLICT, "无法查看该数据");
        }, t -> t);
    }

    /**
     * 在验证用户权限后，根据用户权限和数据权限类型维度，转换信息并返回结果
     * 如果用户没有权限或者信息为空，则调用指定的供应商来获取结果
     *
     * @param oauthClaim                  用户的OAuth声明，用于获取用户权限信息
     * @param dataPermissionTypeDimension 数据权限类型维度，用于确定用户有权访问的数据范围
     * @param info                        要转换的信息
     * @param getDataId                   函数，用于从信息中获取数据ID
     * @param notDataSupplier             供应商，用于在信息为空或用户无权限时提供返回值
     * @param mapper                      函数，用于将信息转换为结果
     * @param <R>                         结果类型
     * @param <T>                         信息类型
     *
     * @return 根据用户权限和数据权限类型维度转换后的结果，或者在信息为空或用户无权限时由供应商提供的值
     */
    public <R, T> R afterCheck(AccessUser oauthClaim, String dataPermissionTypeDimension, T info, Function<T, Long> getDataId, Supplier<R> notDataSupplier, Function<T, R> mapper) {
        if (PermissionContextHolder.isIgnore()) {
            return mapper.apply(info);
        }
        // 如果信息为空，则直接返回供应商提供的值
        if (info == null) {
            return notDataSupplier.get();
        }
        // 获取用户有权限访问的数据ID映射
        Map<String, List<Long>> userDataIds = getUserDataIds(oauthClaim, List.of(dataPermissionTypeDimension));
        // 如果用户数据ID映射为空或者不包含指定的数据权限类型维度，则返回供应商提供的值
        if (EmptyUtil.isEmpty(userDataIds) || !userDataIds.containsKey(dataPermissionTypeDimension)) {
            return notDataSupplier.get();
        }
        // 获取用户在指定维度下有权限的数据ID列表
        List<Long> dataIds = userDataIds.get(dataPermissionTypeDimension);
        // 从信息中获取数据ID
        Long dataId = getDataId.apply(info);
        // 如果用户有权限访问指定的数据ID或所有数据，则将信息转换为结果并返回
        if (dataIds.contains(dataId) || dataIds.contains(all)) {
            PermissionContextHolder.setRequestIgnore();
            return mapper.apply(info);
        }
        // 如果用户没有权限访问指定的数据ID，则返回供应商提供的值
        return notDataSupplier.get();
    }


    /**
     * 执行业务操作
     *
     * @param dataPermissionTypeDimension 数据权限类型维度，用于控制数据访问范围
     * @param dataId                      数据标识，用于指定具体数据项
     * @param actuator                    执行函数，一个实现了Actuator接口的对象，用于执行具体业务逻辑
     *                                    <p>
     *                                    此方法首先检查当前用户是否已登录，如果未登录，则抛出认证异常
     *                                    如果用户已登录，则调用重载的run方法，传入当前用户信息、数据权限类型维度、数据标识和执行函数
     *                                    这个方法的存在确保了只有经过身份验证的用户才能执行业务操作
     */
    public <T> T run(String dataPermissionTypeDimension, Long dataId, Actuator<T> actuator) {
        // 尝试获取当前用户信息，如果未登录，则抛出异常
        Optional<AccessUser> claimOptional = UserHold.getAccessUser();
        if (claimOptional.isEmpty()) {
            throw new AuthenticationException("未登陆");
        }
        // 调用重载的run方法，传入当前用户信息、数据权限类型维度、数据标识和执行函数
        return run(claimOptional.get(), dataPermissionTypeDimension, dataId, actuator);
    }


    /**
     * 执行带有数据权限控制的业务逻辑
     * 该方法确保执行业务逻辑的用户拥有必要的数据权限
     *
     * @param oauthClaim                  用户的OAuth声明，包含用户信息
     * @param dataPermissionTypeDimension 数据权限类型维度，用于指定数据权限的类型
     * @param dataId                      特定数据的ID，用于检查用户是否拥有对该数据的权限
     * @param actuator                    一个执行器接口，用于执行具体的业务逻辑
     *
     * @throws BusinessException 如果用户没有所需的数据权限，则抛出此异常
     */
    public <T> T run(AccessUser oauthClaim, String dataPermissionTypeDimension, Long dataId, Actuator<T> actuator) {
        if (EmptyUtil.isEmpty(dataId) || EmptyUtil.isEmpty(dataPermissionTypeDimension)) {
            throw new BusinessException(SystemErrorCode.NOT_FOUND, "数据ID或数据权限类型维度为空");
        }
        return runBatch(oauthClaim, Map.of(dataPermissionTypeDimension, dataId), actuator);
    }


    /**
     * 执行数据权限校验并运行具体业务逻辑 (支持批量集合参数)
     *
     * @param oauthClaim        用户身份认证信息载体对象
     * @param dataPermissionMap 数据权限维度与具体数据ID(可以是单个ID或ID集合)的映射集合
     * @param actuator          业务逻辑执行器
     *
     * @throws BusinessException 当用户不满足数据权限要求时抛出权限异常
     */
    @SuppressWarnings("unchecked")
    public <T> T runBatch(AccessUser oauthClaim, Map<String, Object> dataPermissionMap, Actuator<T> actuator) {
        if (PermissionContextHolder.isIgnore()) {
            return actuator.run();
        }
        // 获取用户有权限访问的数据ID列表
        Map<String, List<Long>> userDataMap = getUserDataIds(oauthClaim, new ArrayList<>(dataPermissionMap.keySet()));

        // 基础权限校验：1.存在权限数据 2.包含所有请求的权限维度
        if (EmptyUtil.isEmpty(userDataMap)) {
            throw new BusinessException(BizErrorCode.NO_POWER, "无数据权限");
        }
        String illegalType = dataPermissionMap.keySet().stream()
            .filter(dimension -> !userDataMap.containsKey(dimension) && !businessPermissions.support(dimension))
            .collect(Collectors.joining(","));
        if (EmptyUtil.isNotEmpty(illegalType)) {
            throw new BusinessException(BizErrorCode.PARAM_INVALID, "非法数据权限类型维度：" + illegalType);
        }

        // 细粒度权限校验：验证每个权限维度下是否包含请求的数据ID或全部权限
        boolean allMatch = dataPermissionMap.entrySet().stream().allMatch(entry -> {
            String dimension = entry.getKey();
            Object targetIdsObj = entry.getValue();

            List<Long> targetIds = new ArrayList<>();
            if (targetIdsObj instanceof Collection<?> collection) {
                for (Object item : collection) {
                    Long id = parseLong(item);
                    if (id != null) targetIds.add(id);
                }
            } else {
                Long id = parseLong(targetIdsObj);
                if (id != null) targetIds.add(id);
            }

            if (userDataMap.containsKey(dimension)) {
                List<Long> authorizedIds = userDataMap.get(dimension);
                if (authorizedIds == null || authorizedIds.contains(all)) {
                    return true;
                }
                // 必须包含请求的所有ID
                return new HashSet<>(authorizedIds).containsAll(targetIds);
            }
            // 使用 BusinessPermissions 校验 (暂且只循环校验，若需要优化可让其支持批量)
            return targetIds.stream().allMatch(id -> businessPermissions.verify(oauthClaim.currentTenantId(), oauthClaim.userId(), userDataMap, dimension, id));
        });

        if (!allMatch) {
            throw new BusinessException(BizErrorCode.NO_POWER, "无数据权限");
        }
        PermissionContextHolder.setRequestIgnore();
        // 通过所有权限校验后执行业务逻辑
        return actuator.run();
    }

    private Long parseLong(Object obj) {
        if (obj instanceof Long) {
            return (Long) obj;
        } else if (obj instanceof Number) {
            return ((Number) obj).longValue();
        } else if (obj instanceof String) {
            try {
                return Long.parseLong((String) obj);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }


    /**
     * 根据OAuth声明和数据权限请求应用业务功能
     *
     * @param oauthClaim       OAuth声明，包含用户认证和授权信息
     * @param request          数据权限请求，具体的数据权限请求对象
     * @param businessFunction 业务功能，一个函数，定义如何处理数据权限请求并返回结果
     * @param <R>              结果类型，业务功能执行后的返回值类型
     * @param <T>              请求类型，继承自DataPermissionsRequest，表示具体的数据权限请求类型
     *
     * @return 应用业务功能后的结果，类型为R
     *     <p>
     *     此方法封装了业务功能的执行，确保在执行前检查用户的OAuth声明和数据权限请求
     *     如果用户没有相应的数据权限，将抛出RbacException异常
     */
    public <R, T extends DataPermissionsRequest> R apply(AccessUser oauthClaim, T request, Function<T, R> businessFunction) {
        return apply(oauthClaim, request, businessFunction, () -> {
            throw new BusinessException(BizErrorCode.NO_POWER, "无数据权限");
        });
    }

    /**
     * 应用给定的数据权限请求和业务函数来执行操作，并返回结果.
     * 如果执行过程中没有数据权限，则抛出异常.
     *
     * @param request          数据权限请求对象，用于指定具体的数据权限需求.
     * @param businessFunction 业务逻辑函数，用于执行具体的业务操作.
     * @param <R>              业务函数返回值的类型.
     * @param <T>              数据权限请求的类型，必须是DataPermissionsRequest的子类.
     *
     * @return 业务函数执行的结果.
     *
     * @throws BusinessException 如果没有数据权限，则抛出此异常.
     */
    public <R, T extends DataPermissionsRequest> R apply(T request, Function<T, R> businessFunction) {
        return apply(request, businessFunction, () -> {
            throw new BusinessException(BizErrorCode.NO_POWER, "无数据权限");
        });
    }

    /**
     * 根据用户权限处理数据
     * 该方法用于处理用户的数据权限根据用户所属公司和请求的下属公司ID列表，
     * 决定是否执行业务逻辑或返回默认数据
     *
     * @param request          请求对象，包含下属公司的ID列表
     * @param businessFunction 业务逻辑处理函数，用于处理请求对象并返回结果
     * @param notDataSupplier  当用户无权限或无数据时的备用数据提供者
     *
     * @return {@link R } 返回业务逻辑处理结果或备用数据
     */
    public <R, T extends DataPermissionsRequest> R apply(T request, Function<T, R> businessFunction, Supplier<R> notDataSupplier) {
        return UserHold.getAccessUser().map(user -> {
            return apply(user, request, businessFunction, notDataSupplier);
        }).orElseGet(notDataSupplier);
    }


    /**
     * 根据OAuth声明和数据权限请求处理数据
     * 此方法使用了泛型，允许它接受不同类型的请求对象和返回不同类型的响应对象
     * 它首先设置公司的ID，然后根据用户的数据权限过滤请求的数据
     * 如果用户没有数据权限，或者请求的数据不完全在用户的权限范围内，将返回备用数据
     * 否则，它将执行业务逻辑并返回结果
     *
     * @param oauthClaim       OAuth声明，包含用户信息和数据权限
     * @param request          数据权限请求对象，包含请求的数据信息
     * @param businessFunction 业务逻辑函数，用于处理请求并返回结果
     * @param notDataSupplier  备用数据供应商，当用户没有数据权限时提供备用数据
     *
     * @return {@link R } 业务逻辑处理后的结果或备用数据
     */
    public <R, T extends DataPermissionsRequest> R apply(AccessUser oauthClaim, T request, Function<T, R> businessFunction, Supplier<R> notDataSupplier) {
        // 设置请求的公司ID
        request.setTenantId(oauthClaim.currentTenantId());
        request.initCurrentUserId(oauthClaim.userId());
        request.ifSystemUser(Objects.equals(oauthClaim.userType(), OauthConstant.SYSTEM_USER));
        if (PermissionContextHolder.isIgnore()) {
            return businessFunction.apply(request);
        }
        // 获取请求的数据权限类型维度
        List<String> typeDimensions = request.dataTypes();
        // 获取数据权限
        Map<String, List<Long>> userDataIds = getUserDataIds(oauthClaim, typeDimensions);
        // 如果用户没有数据权限，则返回备用数据
        if (EmptyUtil.isEmpty(userDataIds)) {
            return notDataSupplier.get();
        }
        // 是否为单位权限
        if (userDataIds.values().stream().allMatch(this::isAll)) {
            PermissionContextHolder.setRequestIgnore();
            request.ifSystemUser(true);
            return businessFunction.apply(request);
        }

        // 根据数据权限类型维度或所有维度处理请求的数据
        Collection<String> keys = typeDimensions == null ? userDataIds.keySet() : typeDimensions;
        for (String key : keys) {
            // 获取请求的数据ID列表
            List<Long> requestDataIds = request.findDataIds(key);
            List<Long> dataIds = userDataIds.get(key);
            if (EmptyUtil.isEmpty(requestDataIds) && EmptyUtil.isEmpty(dataIds)) {
                continue;
            }
            // 如果当前维度的数据权限为空，则返回备用数据
            if (EmptyUtil.isEmpty(dataIds)) {
                return notDataSupplier.get();
            }
            // 当前维度为全部数据权限
            if (isAll(dataIds)) {
                continue;
            }

            // 如果请求的数据ID列表为空，使用当前维度的数据权限
            if (EmptyUtil.isEmpty(requestDataIds)) {
                request.toDataIds(key, dataIds);
            } else {
                // 过滤请求的数据ID列表，保留用户有权限的数据
                List<Long> thisDataIds = requestDataIds.stream().filter(dataIds::contains).collect(Collectors.toList());
                // 如果过滤后的数据ID列表与请求的数据ID列表不一致，说明用户没有权限访问所有请求的数据，返回备用数据
                if (thisDataIds.size() != requestDataIds.size()) {
                    return notDataSupplier.get();
                }
                request.toDataIds(key, thisDataIds);
            }
        }
        PermissionContextHolder.setRequestIgnore();
        // 执行业务逻辑并返回结果
        return businessFunction.apply(request);

    }


    /**
     * 获取用户数据权限id (带有请求级缓存)
     *
     * @param user 用户
     *
     * @return {@link Map }<{@link String }, {@link List }<{@link Long }>>
     */
    public Map<String, List<Long>> getUserDataIds(AccessUser user, List<String> dataTypes) {
        if (user == null || EmptyUtil.isEmpty(dataTypes)) {
            return Map.of();
        }

        // L1 缓存 尝试从请求上下文缓存中获取
        Map<String, List<Long>> userPermissionMap = new HashMap<>(8);
        List<String> notCacheType = new ArrayList<>();
        for (String dataType : dataTypes) {
            List<Long> cachedIds = PermissionContextHolder.getRequestCache(dataType);
            if (cachedIds != null) {
                userPermissionMap.put(dataType, cachedIds);
                continue;
            }
            notCacheType.add(dataType);
        }
        if (EmptyUtil.isEmpty(notCacheType)) {
            return userPermissionMap;
        }

        Map<String, List<Long>> l2CacheMap = new HashMap<>();
        for (UserPermissionProvider provider : permissionProviders) {
            if (provider.supports(user)) {
                Map<String, List<Long>> permissionMap = provider.getPermissionIds(user, notCacheType, connectorProcessor);
                if (EmptyUtil.isNotEmpty(permissionMap)) {
                    l2CacheMap.putAll(permissionMap);
                }
                break; // 找到支持的 provider 后跳出内层循环
            }
        }
        // 存入L1 请求级缓存
        if (EmptyUtil.isNotEmpty(l2CacheMap)) {
            l2CacheMap.forEach(PermissionContextHolder::setRequestCache);
            userPermissionMap.putAll(l2CacheMap);
        }
        return userPermissionMap;
    }


    /**
     * 是否拥有全部数据权限
     *
     * @param ids ids
     *
     * @return boolean
     */
    public boolean isAll(List<Long> ids) {
        if (ids == null) {
            return false; // null 代表全部权限
        }
        if (EmptyUtil.isEmpty(ids)) {
            return false;
        }
        return ids.contains(all);
    }


    /**
     * 清除请求
     * <p>
     * 此方法用于清理或重置请求状态
     */
    public void cleanRequest() {
        PermissionContextHolder.clearRequestIgnore();
    }


    /**
     * 执行器
     *
     * @author HeHui
     * @date 2024/11/14
     */
    @FunctionalInterface
    public interface Actuator<T> {

        /**
         * 运行
         */
        T run();
    }

    /**
     * BusinessPermissionsCombination类用于处理业务权限的组合逻辑
     * 它作为一个私有静态内部类，旨在为权限组合提供一种封装机制，以增强代码的可维护性和安全性
     * 该类主要负责根据不同的业务场景，组合不同的权限配置，以满足复杂多变的业务需求
     * <p>
     * 注意：该类的设计避免了外部直接访问，确保了权限组合逻辑的专一性和安全性
     */
    private static class BusinessPermissionsCombination {

        private final Map<String, BusinessPermissions> businessPermissionMap;

        private BusinessPermissionsCombination(List<BusinessPermissions> businessPermissions) {
            if (EmptyUtil.isEmpty(businessPermissions)) {
                businessPermissionMap = Map.of();
            } else {
                this.businessPermissionMap = businessPermissions.stream().collect(Collectors.toMap(BusinessPermissions::support, Function.identity()));
            }
        }

        private BusinessPermissionsCombination(Map<String, BusinessPermissions> businessPermissionMap) {
            this.businessPermissionMap = Objects.requireNonNullElseGet(businessPermissionMap, Map::of);
        }


        /**
         * 支持的业务
         *
         * @return boolean
         */
        public boolean support(String businessType) {
            return businessPermissionMap.containsKey(businessType);
        }

        /**
         * 验证
         *
         * @param tenantId          公司id
         * @param userId            用户id
         * @param userPermissionMap 用户数据权限
         * @param businessType      业务类型
         * @param businessId        业务ID
         *
         * @return boolean
         */
        public boolean verify(Long tenantId, Long userId, Map<String, List<Long>> userPermissionMap, String businessType, Long businessId) {
            return businessPermissionMap.get(businessType).verify(tenantId, userId, userPermissionMap, businessType, businessId);
        }
    }
}
