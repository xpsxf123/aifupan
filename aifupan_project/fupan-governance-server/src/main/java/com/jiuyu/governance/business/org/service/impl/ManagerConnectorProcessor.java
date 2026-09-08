package com.jiuyu.governance.business.org.service.impl;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.toolkit.ChainWrappers;
import com.jiuyu.framework.function.BatchQuery;
import com.jiuyu.framework.json.JsonTemplate;
import com.jiuyu.framework.util.CollUtil;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.governance.business.org.mapper.DeptMapper;
import com.jiuyu.governance.business.org.mapper.ManagerConnectorMapper;
import com.jiuyu.governance.business.org.mapper.SubCompanyMapper;
import com.jiuyu.governance.business.org.mapper.TeamMapper;
import com.jiuyu.governance.business.org.pojo.constants.ManagerType;
import com.jiuyu.governance.business.org.pojo.entity.Dept;
import com.jiuyu.governance.business.org.pojo.entity.ManagerConnector;
import com.jiuyu.governance.business.org.pojo.entity.SubCompany;
import com.jiuyu.governance.business.org.pojo.entity.Team;
import com.jiuyu.governance.business.rbac.mapper.EmployeeMapper;
import com.jiuyu.governance.business.rbac.pojo.entity.Employee;
import com.jiuyu.governance.business.rbac.pojo.response.EmployeeBaseInfo;
import com.jiuyu.governance.business.room.mapper.LiveRoomMapper;
import com.jiuyu.governance.business.room.pojo.entity.LiveRoom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 管理员连接处理器
 *
 * @author HeHui
 * @date 2026-03-19 09:50
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ManagerConnectorProcessor {

    private final ManagerConnectorMapper connectorMapper;

    private final EmployeeMapper employeeMapper;

    private final StringRedisTemplate redisTemplate;

    private final DeptMapper deptMapper;

    private final TeamMapper teamMapper;

    private final LiveRoomMapper roomMapper;

    private final SubCompanyMapper companyMapper;

    private final String cachePrefix = "oauth:data-permissions:";


    /**
     * 连接
     * 不处理 employeeIds 为空的情况
     *
     * @param type        类型
     * @param targetId    目标ID
     * @param employeeIds 员工ID列表
     */
    @Transactional(rollbackFor = Throwable.class)
    public void connector(ManagerType type, long targetId, List<Long> employeeIds) {
        if (EmptyUtil.isEmpty(employeeIds)) {
            return;
        }
        List<Long> connectorIds = this.getConnectorIds(type, targetId);
        List<ManagerConnector> newConnectorList = employeeIds.stream().filter(employeeId -> !connectorIds.contains(employeeId))
            .map(employeeId -> {
                ManagerConnector connector = new ManagerConnector();
                connector.setId(IdWorker.getId());
                connector.setManagerType(type);
                connector.setTargetId(targetId);
                connector.setEmployeeId(employeeId);
                connector.setIsDeleted(false);
                connector.setCreateDate(LocalDateTime.now());
                connector.setUpdateDate(LocalDateTime.now());
                return connector;
            }).toList();
        List<Long> deleteConnectorIds = connectorIds.stream().filter(connectorId -> !employeeIds.contains(connectorId)).toList();
        if (EmptyUtil.isNotEmpty(deleteConnectorIds)) {
            ChainWrappers.lambdaUpdateChain(connectorMapper)
                .eq(ManagerConnector::getManagerType, type.getValue())
                .eq(ManagerConnector::getTargetId, targetId)
                .in(ManagerConnector::getEmployeeId, deleteConnectorIds)
                .eq(ManagerConnector::getIsDeleted, false)
                .set(ManagerConnector::getIsDeleted, true)
                .set(ManagerConnector::getUpdateDate, LocalDateTime.now())
                .update();
            redisTemplate.delete(deleteConnectorIds.stream().map(this::getUserCacheKey).toList());
        }
        if (EmptyUtil.isNotEmpty(newConnectorList)) {
            connectorMapper.insertBatch(newConnectorList);
            redisTemplate.delete(newConnectorList.stream().map(ManagerConnector::getEmployeeId).map(this::getUserCacheKey).toList());
        }
    }


    /**
     * 断开所有连接
     *
     * @param type     类型
     * @param targetId 目标ID
     */
    public void disconnectAll(ManagerType type, long targetId) {
        List<Long> employeeIds = ChainWrappers.lambdaQueryChain(connectorMapper)
            .eq(ManagerConnector::getManagerType, type.getValue())
            .eq(ManagerConnector::getTargetId, targetId)
            .eq(ManagerConnector::getIsDeleted, false)
            .select(ManagerConnector::getEmployeeId)
            .list().stream().map(ManagerConnector::getEmployeeId).toList();
        if (EmptyUtil.isEmpty(employeeIds)) {
            return;
        }
        ChainWrappers.lambdaUpdateChain(connectorMapper)
            .eq(ManagerConnector::getManagerType, type.getValue())
            .eq(ManagerConnector::getTargetId, targetId)
            .eq(ManagerConnector::getIsDeleted, false)
            .set(ManagerConnector::getIsDeleted, true)
            .set(ManagerConnector::getUpdateDate, LocalDateTime.now())
            .update();
        redisTemplate.delete(employeeIds.stream().map(this::getUserCacheKey).toList());
        log.info("[管理员] 清除当前所有管理员: {}, {}", targetId, type.getDesc());
    }


    /**
     * 查询租户下各管理类型的所有连接器 ID 列表
     * <p>
     * 采用缓存优先策略：
     * 1. 先从 Redis 缓存中批量获取已缓存的连接器 ID
     * 2. 对于未缓存的类型，从数据库查询并更新缓存
     * <p>
     * 不同管理类型的缓存过期时间：
     * - COMPANY（公司）：3 天
     * - DEPT（部门）：1 天
     * - TEAM（小组）：1 天
     * - LIVE_ROOM（直播间）：3 小时
     *
     * @param tenantId     租户 ID
     * @param managerTypes 管理类型列表，支持 COMPANY、DEPT、TEAM、LIVE_ROOM
     *
     * @return Map<ManagerType, List < Long>> 各管理类型对应的连接器 ID 列表；空 Map 表示没有请求的类型或数据为空
     */
    public Map<ManagerType, List<Long>> queryTenantAllConnectorIds(long tenantId, List<ManagerType> managerTypes) {
        // 参数校验，类型为空直接返回空 Map
        if (EmptyUtil.isEmpty(managerTypes)) {
            return Map.of();
        }
        // 构建缓存 Key 准备批量查询缓存
        List<String> managerCacheKeys = managerTypes.stream().map(type -> this.getTenantCacheKey(tenantId, type)).toList();
        // 批量从 Redis 获取缓存值
        List<String> valueList = redisTemplate.opsForValue().multiGet(managerCacheKeys);
        List<ManagerType> notCacheTypes = new ArrayList<>(managerTypes.size());
        Map<ManagerType, List<Long>> resultMap = new HashMap<>();
        // 解析缓存结果，分离命中和未命中的类型
        if (EmptyUtil.isNotEmpty(valueList)) {
            for (int i = 0; i < valueList.size(); i++) {
                String cacheValue = valueList.get(i);
                if (cacheValue == null) {
                    notCacheTypes.add(managerTypes.get(i));
                } else if (JsonTemplate.isJsonArray(cacheValue)) {
                    resultMap.put(managerTypes.get(i), JsonTemplate.toList(cacheValue, Long.class));
                }
            }
        } else {
            notCacheTypes.addAll(managerTypes);
        }

        // 处理未命中缓存的类型，从数据库查询并更新缓存
        if (EmptyUtil.isNotEmpty(notCacheTypes)) {
            notCacheTypes.forEach(type -> {
                switch (type) {
                    case COMPANY -> {
                        List<Long> companyIds = queryTenantAllIds(companyMapper, tenantId, SubCompany::getTenantId, SubCompany::getId, SubCompany::getIsDeleted);
                        resultMap.put(type, companyIds);
                        redisTemplate.opsForValue().set(this.getTenantCacheKey(tenantId, type), JsonTemplate.toJson(companyIds), Duration.ofDays(3));
                    }
                    case DEPT -> {
                        List<Long> deptIds = queryTenantAllIds(deptMapper, tenantId, Dept::getTenantId, Dept::getId, Dept::getIsDeleted);
                        resultMap.put(type, deptIds);
                        redisTemplate.opsForValue().set(this.getTenantCacheKey(tenantId, type), JsonTemplate.toJson(deptIds), Duration.ofDays(1));
                    }
                    case TEAM -> {
                        List<Long> teamIds = queryTenantAllIds(teamMapper, tenantId, Team::getTenantId, Team::getId, Team::getIsDeleted);
                        resultMap.put(type, teamIds);
                        redisTemplate.opsForValue().set(this.getTenantCacheKey(tenantId, type), JsonTemplate.toJson(teamIds), Duration.ofDays(1));
                    }
                    case LIVE_ROOM -> {
                        List<Long> roomIds = queryTenantAllIds(roomMapper, tenantId, LiveRoom::getTenantId, LiveRoom::getId, LiveRoom::getIsDeleted);
                        resultMap.put(type, roomIds);
                        redisTemplate.opsForValue().set(this.getTenantCacheKey(tenantId, type), JsonTemplate.toJson(roomIds), Duration.ofHours(3));
                    }
                    case EMPLOYEE -> {
                        List<Long> employeeIds = queryTenantAllIds(employeeMapper, tenantId, Employee::getTenantId, Employee::getId, Employee::getIsDeleted);
                        resultMap.put(type, employeeIds);
                        redisTemplate.opsForValue().set(this.getTenantCacheKey(tenantId, type), JsonTemplate.toJson(employeeIds), Duration.ofDays(7));
                    }
                    default -> {
                    }
                }
            });
        }

        return resultMap;
    }


    /**
     * 查询租户下的所有ID
     *
     * @param mapper       映射器
     * @param tenantId     租户ID
     * @param getTenantId  获取租户ID字段
     * @param getId        获取ID字段
     * @param isDeleted    是否删除字段
     *
     * @return {@link List }<{@link Long }>
     */
    private <T> List<Long> queryTenantAllIds(BaseMapper<T> mapper, long tenantId, SFunction<T, Long> getTenantId, SFunction<T, Long> getId, SFunction<T, Boolean> isDeleted) {
        return new BatchQuery<>((limit, idx) -> {
            return ChainWrappers.lambdaQueryChain(mapper)
                .gt(idx != null, getId, idx)
                .eq(getTenantId, tenantId)
                .eq(isDeleted != null, isDeleted, false)
                .select(getId)
                .last("limit " + limit)
                .list();
        }, getId).get().stream().map(getId).toList();
    }


    /**
     * 获取用户连接的各类型权限ID
     *  todo 数据权限的底层在这里看
     *
     * @param managerTypes 管理类型列表
     * @param tenantId     租户ID
     * @param employeeId   员工ID
     *
     * @return {@link Map }<{@link ManagerType }, {@link List }<{@link Long }>>
     */
    public Map<ManagerType, List<Long>> getUserConnectorIds(List<ManagerType> managerTypes, Long tenantId, long employeeId) {
        String cacheKey = this.getUserCacheKey(employeeId);
        String cacheValue = redisTemplate.opsForValue().get(cacheKey);

        // 先获取用户在，公司，部门，小组，直播间的管理权限
        List<ManagerConnector> managerConnectors = null;
        if (JsonTemplate.isJsonArray(cacheValue)) {
            managerConnectors = JsonTemplate.toList(cacheValue, ManagerConnector.class);
        } else {
            managerConnectors = ChainWrappers.lambdaQueryChain(connectorMapper)
                .select(ManagerConnector::getEmployeeId, ManagerConnector::getTargetId, ManagerConnector::getManagerType)
                .eq(ManagerConnector::getEmployeeId, employeeId)
                .eq(ManagerConnector::getIsDeleted, false)
                .list();
            // todo 数据权限 -> 单个用户的数据权限缓存 1小时
            redisTemplate.opsForValue().set(cacheKey, JsonTemplate.toJson(managerConnectors), Duration.ofHours(1));
        }
        if (EmptyUtil.isEmpty(managerConnectors)) {
            return Map.of();
        }


        List<Long> companyIds = managerConnectors.stream().filter(c -> ManagerType.COMPANY.equals(c.getManagerType())).map(ManagerConnector::getTargetId).toList();
        List<Long> deptIds = managerConnectors.stream().filter(c -> ManagerType.DEPT.equals(c.getManagerType())).map(ManagerConnector::getTargetId).toList();
        List<Long> teamIds = managerConnectors.stream().filter(c -> ManagerType.TEAM.equals(c.getManagerType())).map(ManagerConnector::getTargetId).toList();
        List<Long> roomIds = managerConnectors.stream().filter(c -> ManagerType.LIVE_ROOM.equals(c.getManagerType())).map(ManagerConnector::getTargetId).toList();

        return managerTypes.stream().collect(Collectors.toMap(Function.identity(), type -> {
            List<Long> ids = this.getOrgSubIds(type, companyIds, deptIds, teamIds, roomIds, tenantId);
            switch (type) {
                case COMPANY -> {
                    if (EmptyUtil.isEmpty(ids)) {
                        return companyIds;
                    }
                    return CollUtil.merge(companyIds, ids).stream().distinct().toList();
                }
                case DEPT -> {
                    if (EmptyUtil.isEmpty(ids)) {
                        return deptIds;
                    }
                    return CollUtil.merge(deptIds, ids).stream().distinct().toList();
                }
                case TEAM -> {
                    if (EmptyUtil.isEmpty(ids)) {
                        return teamIds;
                    }
                    return CollUtil.merge(teamIds, ids).stream().distinct().toList();
                }
                case LIVE_ROOM -> {
                    if (EmptyUtil.isEmpty(ids)) {
                        return roomIds;
                    }
                    return CollUtil.merge(roomIds, ids).stream().distinct().toList();
                }
                default -> {
                    return ids;
                }
            }
        }));
    }




    /**
     * 获取组织机构子级 ID 列表
     * 根据组织类型和各级组织 ID，查询对应的下级组织 ID 集合，使用缓存优化查询性能
     *
     * @param type       管理者类型，决定查询的组织层级
     * @param companyIds 公司 ID 列表
     * @param deptIds    部门 ID 列表
     * @param teamIds    团队 ID 列表
     * @param roomIds    直播间 ID 列表
     * @param tenantId   租户 ID
     *
     * @return List<Long> 子级组织 ID 列表
     */
    private List<Long> getOrgSubIds(ManagerType type, List<Long> companyIds, List<Long> deptIds, List<Long> teamIds, List<Long> roomIds, Long tenantId) {
        // 没有上级组织，直接返回公司 ID 列表
        if (type.getSuperior() == null && type.equals(ManagerType.COMPANY)) {
            return companyIds;
        }
        return switch (type) {
            // 查询部门 ID 列表
            case DEPT -> this.getOrgCache(ManagerType.DEPT, ManagerType.COMPANY, companyIds, ids -> {
                if (EmptyUtil.isEmpty(ids)) {
                    return Map.of();
                }
                return ChainWrappers.lambdaQueryChain(deptMapper)
                    .select(Dept::getId, Dept::getCompanyId)
                    .in(Dept::getCompanyId, ids)
                    .eq(Dept::getTenantId, tenantId)
                    .eq(Dept::getIsDeleted, false)
                    .list().stream().collect(Collectors.groupingBy(Dept::getCompanyId, Collectors.mapping(Dept::getId, Collectors.toList())));
            }).stream().distinct().toList();

            case TEAM -> {
                // 如果没有公司和部门 ID，直接返回小组 ID 列表
                if (EmptyUtil.isEmpty(companyIds) && EmptyUtil.isEmpty(deptIds)) {
                    yield teamIds;
                }
                // 查询公司下属的团队 ID 列表
                List<Long> companyTeamIds = null;
                if (EmptyUtil.isNotEmpty(companyIds)) {
                    companyTeamIds = this.getOrgCache(ManagerType.TEAM, ManagerType.COMPANY, companyIds, ids -> {
                        if (EmptyUtil.isEmpty(ids)) {
                            return Map.of();
                        }
                        return ChainWrappers.lambdaQueryChain(teamMapper)
                            .select(Team::getId, Team::getCompanyId)
                            .in(Team::getCompanyId, ids)
                            .eq(Team::getTenantId, tenantId)
                            .eq(Team::getIsDeleted, false)
                            .list().stream().collect(Collectors.groupingBy(Team::getCompanyId, Collectors.mapping(Team::getId, Collectors.toList())));
                    });
                }
                // 查询部门下属的团队 ID 列表
                List<Long> deptTeamIds = null;
                if (EmptyUtil.isNotEmpty(deptIds)) {
                    deptTeamIds = this.getOrgCache(ManagerType.TEAM, ManagerType.DEPT, deptIds, ids -> {
                        if (EmptyUtil.isEmpty(ids)) {
                            return Map.of();
                        }
                        return ChainWrappers.lambdaQueryChain(teamMapper)
                            .select(Team::getId, Team::getDeptId)
                            .in(Team::getDeptId, ids)
                            .eq(Team::getTenantId, tenantId)
                            .eq(Team::getIsDeleted, false)
                            .list().stream().collect(Collectors.groupingBy(Team::getDeptId, Collectors.mapping(Team::getId, Collectors.toList())));
                    });
                }
                // 如果没有找到任何团队，返回原始团队 ID 列表
                if (EmptyUtil.isEmpty(deptTeamIds) && EmptyUtil.isEmpty(companyTeamIds)) {
                    yield teamIds;
                }
                // 合并所有来源的团队 ID 并去重
                yield Stream.of(companyTeamIds, deptTeamIds, teamIds).filter(Objects::nonNull).flatMap(Collection::stream).distinct().toList();
            }
            case LIVE_ROOM -> {
                // 如果没有部门、公司和团队 ID，直接返回直播间 ID 列表
                if (EmptyUtil.isEmpty(deptIds) && EmptyUtil.isEmpty(companyIds) && EmptyUtil.isEmpty(teamIds)) {
                    yield roomIds;
                }
                // 查询公司下属的直播间 ID 列表
                List<Long> companyRoomIds = null;
                if (EmptyUtil.isNotEmpty(companyIds)) {
                    companyRoomIds = this.getOrgCache(ManagerType.LIVE_ROOM, ManagerType.COMPANY, companyIds, ids -> {
                        if (EmptyUtil.isEmpty(ids)) {
                            return Map.of();
                        }
                        return ChainWrappers.lambdaQueryChain(roomMapper)
                            .select(LiveRoom::getId, LiveRoom::getCompanyId)
                            .in(LiveRoom::getCompanyId, ids)
                            .eq(LiveRoom::getTenantId, tenantId)
                            .eq(LiveRoom::getIsDeleted, false)
                            .list().stream().collect(Collectors.groupingBy(LiveRoom::getCompanyId, Collectors.mapping(LiveRoom::getId, Collectors.toList())));
                    });
                }
                // 查询部门下属的直播间 ID 列表
                List<Long> deptRoomIds = null;
                if (EmptyUtil.isNotEmpty(deptIds)) {
                    deptRoomIds = this.getOrgCache(ManagerType.LIVE_ROOM, ManagerType.DEPT, deptIds, ids -> {
                        if (EmptyUtil.isEmpty(ids)) {
                            return Map.of();
                        }
                        return ChainWrappers.lambdaQueryChain(roomMapper)
                            .select(LiveRoom::getId, LiveRoom::getDeptId)
                            .in(LiveRoom::getDeptId, ids)
                            .eq(LiveRoom::getTenantId, tenantId)
                            .eq(LiveRoom::getIsDeleted, false)
                            .list().stream().collect(Collectors.groupingBy(LiveRoom::getDeptId, Collectors.mapping(LiveRoom::getId, Collectors.toList())));
                    });
                }
                // 查询团队下属的直播间 ID 列表
                List<Long> teamRoomIds = null;
                if (EmptyUtil.isNotEmpty(teamIds)) {
                    teamRoomIds = this.getOrgCache(ManagerType.LIVE_ROOM, ManagerType.TEAM, teamIds, ids -> {
                        if (EmptyUtil.isEmpty(ids)) {
                            return Map.of();
                        }
                        return ChainWrappers.lambdaQueryChain(roomMapper)
                            .select(LiveRoom::getId, LiveRoom::getTeamId)
                            .in(LiveRoom::getTeamId, ids)
                            .eq(LiveRoom::getTenantId, tenantId)
                            .eq(LiveRoom::getIsDeleted, false)
                            .list().stream().collect(Collectors.groupingBy(LiveRoom::getTeamId, Collectors.mapping(LiveRoom::getId, Collectors.toList())));
                    });
                }
                // 如果没有找到任何直播间，返回原始直播间 ID 列表
                if (EmptyUtil.isEmpty(deptRoomIds) && EmptyUtil.isEmpty(companyRoomIds) && EmptyUtil.isEmpty(teamRoomIds)) {
                    yield roomIds;
                }
                // 合并所有来源的直播间 ID 并去重
                yield Stream.of(companyRoomIds, deptRoomIds, teamRoomIds, roomIds).filter(Objects::nonNull).flatMap(Collection::stream).distinct().toList();
            }
            case EMPLOYEE -> {
                // 如果没有部门、公司和团队 ID
                if (EmptyUtil.isEmpty(deptIds) && EmptyUtil.isEmpty(companyIds) && EmptyUtil.isEmpty(teamIds)) {
                    yield List.of();
                }
                // 查询公司下属的人员 ID 列表
                List<Long> companyEmployeeIds = null;
                if (EmptyUtil.isNotEmpty(companyIds)) {
                    companyEmployeeIds = this.getOrgCache(ManagerType.EMPLOYEE, ManagerType.COMPANY, companyIds, ids -> {
                        if (EmptyUtil.isEmpty(ids)) {
                            return Map.of();
                        }
                        return ChainWrappers.lambdaQueryChain(employeeMapper)
                            .select(Employee::getId, Employee::getCompanyId)
                            .in(Employee::getCompanyId, ids)
                            .eq(Employee::getTenantId, tenantId)
                            .eq(Employee::getIsDeleted, false)
                            .list().stream().collect(Collectors.groupingBy(Employee::getCompanyId, Collectors.mapping(Employee::getId, Collectors.toList())));
                    });
                }
                // 查询部门下属的员工 ID 列表
                List<Long> deptEmployeeIds = null;
                if (EmptyUtil.isNotEmpty(deptIds)) {
                    deptEmployeeIds = this.getOrgCache(ManagerType.EMPLOYEE, ManagerType.DEPT, deptIds, ids -> {
                        if (EmptyUtil.isEmpty(ids)) {
                            return Map.of();
                        }
                        return ChainWrappers.lambdaQueryChain(employeeMapper)
                            .select(Employee::getId, Employee::getDeptId)
                            .in(Employee::getDeptId, ids)
                            .eq(Employee::getTenantId, tenantId)
                            .eq(Employee::getIsDeleted, false)
                            .list().stream().collect(Collectors.groupingBy(Employee::getDeptId, Collectors.mapping(Employee::getId, Collectors.toList())));
                    });
                }
                // 查询团队下属的员工 ID 列表
                List<Long> teamEmployeeIds = null;
                if (EmptyUtil.isNotEmpty(teamIds)) {
                    teamEmployeeIds = this.getOrgCache(ManagerType.EMPLOYEE, ManagerType.TEAM, teamIds, ids -> {
                        if (EmptyUtil.isEmpty(ids)) {
                            return Map.of();
                        }
                        return ChainWrappers.lambdaQueryChain(employeeMapper)
                            .select(Employee::getId, Employee::getTeamId)
                            .in(Employee::getTeamId, ids)
                            .eq(Employee::getTenantId, tenantId)
                            .eq(Employee::getIsDeleted, false)
                            .list().stream().collect(Collectors.groupingBy(Employee::getTeamId, Collectors.mapping(Employee::getId, Collectors.toList())));
                    });
                }
                // 如果没有找到任何人员，返回原始人员 ID 列表
                if (EmptyUtil.isEmpty(deptEmployeeIds) && EmptyUtil.isEmpty(companyEmployeeIds) && EmptyUtil.isEmpty(teamEmployeeIds)) {
                    yield List.of();
                }
                // 合并所有来源的直播间 ID 并去重
                yield Stream.of(companyEmployeeIds, deptEmployeeIds, teamEmployeeIds).filter(Objects::nonNull).flatMap(Collection::stream).distinct().toList();
            }
            // 未知类型，返回空列表
            default -> List.of();
        };
    }


    /**
     * 获取组织机构缓存数据
     * 从 Redis 缓存中获取组织 ID 对应的子级 ID 列表，如果缓存不存在则通过函数计算并缓存结果
     *
     * @param source 下级类型
     * @param target 上级/超类型
     * @param orgIds 组织 ID 列表
     * @param func   数据加载函数，当缓存未命中时用于加载数据
     *
     * @return List<Long> 所有组织的子级 ID 列表
     */
    private List<Long> getOrgCache(ManagerType source, ManagerType target, List<Long> orgIds, Function<List<Long>, Map<Long, List<Long>>> func) {
        // 生成缓存 key，根据索引对应的缓存 key
        List<String> cacheKeys = orgIds.stream().map(id -> this.getOrgCacheKey(source, target, id)).toList();
        List<String> valueList = redisTemplate.opsForValue().multiGet(cacheKeys);

        // 如果缓存中一条数据都没有，全量查询并缓存
        if (EmptyUtil.isEmpty(valueList)) {
            Map<Long, List<Long>> map = func.apply(orgIds);
            orgIds.forEach(id -> {
                List<Long> ids = map.get(id);
                // 避免缓存穿透，设置空值
                if (ids == null) {
                    ids = List.of();
                }
                // 缓存 3 天的数据
                redisTemplate.opsForValue().set(this.getOrgCacheKey(source, target, id), JsonTemplate.toJson(ids), Duration.ofDays(3));
            });
            return map.values().stream().flatMap(List::stream).toList();
        }
        List<Long> resultIds = new ArrayList<>(1024);
        List<Long> notCacheIds = new ArrayList<>(orgIds.size() + 1);
        // 解析缓存命中的数据，收集未命中的 ID
        for (int i = 0; i < orgIds.size(); i++) {
            String cacheValue = valueList.get(i);
            if (cacheValue == null) {
                notCacheIds.add(orgIds.get(i));
            } else {
                // 缓存中的值
                List<Long> ids = JsonTemplate.toList(cacheValue, Long.class);
                if (EmptyUtil.isNotEmpty(ids)) {
                    resultIds.addAll(ids);
                }
            }
        }
        // 查询并缓存未命中的数据
        if (EmptyUtil.isNotEmpty(notCacheIds)) {
            Map<Long, List<Long>> map = func.apply(notCacheIds);
            notCacheIds.forEach(id -> {
                List<Long> ids = map.get(id);
                // 避免缓存穿透，设置空值
                if (ids == null) {
                    ids = List.of();
                }
                // 缓存 3 天的数据
                redisTemplate.opsForValue().set(this.getOrgCacheKey(source, target, id), JsonTemplate.toJson(ids), Duration.ofDays(3));
            });
            if (EmptyUtil.isNotEmpty(map)) {
                resultIds.addAll(map.values().stream().flatMap(List::stream).toList());
            }
        }
        return resultIds;
    }


    /**
     * 清除组织权限缓存
     *
     * @param type     类型
     * @param targetId 目标ID
     */
    public void clearOrgCache(ManagerType type, long targetId) {
        List<ManagerType> subs = type.getSubs();
        if (EmptyUtil.isEmpty(subs)) {
            return;
        }
        List<String> cacheKeys = subs.stream().map(sub -> this.getOrgCacheKey(sub, type, targetId)).toList();
        redisTemplate.delete(cacheKeys);
    }


    /**
     * 清除租户权限缓存
     *
     * @param tenantId 租户ID
     * @param managerTypes 类型列表
     */
    public void clearTenantCache(long tenantId, List<ManagerType> managerTypes) {
        if (EmptyUtil.isEmpty(managerTypes)) {
            return;
        }
        List<String> cacheList = managerTypes.stream().map(type -> {
            log.info("[组织架构]clearTenantCache tenant: {} , type: {}", tenantId, type.getDesc());
            return this.getTenantCacheKey(tenantId, type);
        }).toList();
        redisTemplate.delete(cacheList);
    }

    /**
     * 清除组织权限缓存
     *
     * @param targetIdMap 目标ID映射
     */
    public void clearOrgCache(Map<ManagerType, List<Long>> targetIdMap) {
        if (EmptyUtil.isEmpty(targetIdMap)) {
            return;
        }
        List<String> cacheKeys = targetIdMap.entrySet().stream().flatMap(entry -> {
            ManagerType type = entry.getKey();
            List<Long> targetIds = entry.getValue();
            List<ManagerType> subs = type.getSubs();
            if (EmptyUtil.isEmpty(subs)) {
                return Stream.of();
            }
            return subs.stream().flatMap(sub -> targetIds.stream().filter(Objects::nonNull).map(targetId -> this.getOrgCacheKey(sub, type, targetId)));
        }).filter(EmptyUtil::isNotEmpty).toList();
        redisTemplate.delete(cacheKeys);
    }


    /**
     * 获取连接的管理员信息
     *
     * @param type      类型
     * @param targetIds 目标ID列表
     *
     * @return 连接的管理员信息
     */
    public Map<Long, List<EmployeeBaseInfo>> getConnectorManagerInfos(ManagerType type, List<Long> targetIds) {
        if (EmptyUtil.isEmpty(targetIds)) {
            return Map.of();
        }
        Map<Long, List<Long>> targetConnectorMap = ChainWrappers.lambdaQueryChain(connectorMapper)
            .select(ManagerConnector::getEmployeeId, ManagerConnector::getTargetId)
            .eq(ManagerConnector::getManagerType, type.getValue())
            .in(ManagerConnector::getTargetId, targetIds)
            .eq(ManagerConnector::getIsDeleted, false)
            .list().stream().collect(Collectors.groupingBy(ManagerConnector::getTargetId, Collectors.mapping(ManagerConnector::getEmployeeId, Collectors.toList())));

        if (EmptyUtil.isEmpty(targetConnectorMap)) {
            return Map.of();
        }
        Map<Long, EmployeeBaseInfo> employeeMap = ChainWrappers.lambdaQueryChain(employeeMapper)
            .select(Employee::getId, Employee::getName, Employee::getUserAvatar, Employee::getStaffNumber, Employee::getMobile)
            .in(Employee::getId, targetConnectorMap.values().stream().flatMap(List::stream).toList())
            .list().stream().collect(Collectors.toMap(Employee::getId, employee -> {
                EmployeeBaseInfo baseInfo = new EmployeeBaseInfo();
                baseInfo.setId(employee.getId());
                baseInfo.setName(employee.getName());
                baseInfo.setUserAvatar(employee.getUserAvatar());
                baseInfo.setStaffNumber(employee.getStaffNumber());
                baseInfo.setMobile(employee.getMobile());
                return baseInfo;
            }));
        return targetConnectorMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> employeeMap.entrySet().stream()
            .filter(employeeEntry -> entry.getValue().contains(employeeEntry.getKey()))
            .map(Map.Entry::getValue)
            .toList()));
    }


    /**
     * 获取连接的ID列表
     *
     * @param type     类型
     * @param targetId 目标ID
     *
     * @return 连接的ID列表
     */
    private List<Long> getConnectorIds(ManagerType type, long targetId) {
        return ChainWrappers.lambdaQueryChain(connectorMapper)
            .select(ManagerConnector::getEmployeeId)
            .eq(ManagerConnector::getManagerType, type.getValue())
            .eq(ManagerConnector::getTargetId, targetId)
            .eq(ManagerConnector::getIsDeleted, false)
            .list().stream().map(ManagerConnector::getEmployeeId).toList();
    }

    /**
     * 获取用户缓存Key
     *
     * @param employeeId 员工ID
     *
     * @return 缓存Key
     */
    private String getUserCacheKey(long employeeId) {
        return cachePrefix + "manage-org:" + employeeId;
    }


    /**
     * 获取租户缓存Key
     *
     * @param tenantId 租户ID
     * @param type     类型
     *
     * @return 缓存Key
     */
    private String getTenantCacheKey(long tenantId, ManagerType type) {
        return cachePrefix + "tenant:" + tenantId + ":" + type.getCode();
    }

    /**
     * 获取组织缓存Key
     *
     * @param source 下级类型
     * @param target 上级或超类型
     * @param orgId  组织ID
     *
     * @return 缓存Key
     */
    private String getOrgCacheKey(ManagerType source, ManagerType target, long orgId) {
        return cachePrefix + "org-" + target.getCode() + "-" + source.getCode() + ":" + orgId;
    }
}
