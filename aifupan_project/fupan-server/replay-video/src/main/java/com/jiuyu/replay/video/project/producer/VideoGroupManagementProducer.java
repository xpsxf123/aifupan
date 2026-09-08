package com.jiuyu.replay.video.project.producer;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.dto.power.UserDto;
import com.jiuyu.replay.generic.feign.power.UserFeign;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import com.jiuyu.replay.generic.vo.power.UserCacheVo;
import com.jiuyu.replay.video.project.bo.group.GroupAddBo;
import com.jiuyu.replay.video.project.bo.group.GroupEditBo;
import com.jiuyu.replay.video.project.bo.group.GroupQueryBo;
import com.jiuyu.replay.video.project.entity.VideoHotSearchEntity;
import com.jiuyu.replay.video.project.entity.VideoUserHotSubscriptionEntity;
import com.jiuyu.replay.video.project.entity.VideoUserInfluencerSubscriptionEntity;
import com.jiuyu.replay.video.project.entity.VideoUserSubscriptionGroupEntity;
import com.jiuyu.replay.video.project.service.*;
import com.jiuyu.replay.video.project.util.VideoCountBatchQueryUtil;
import com.jiuyu.replay.video.project.vo.group.GroupVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 分组管理业务编排
 *
 * @author RayChou
 * @date 2025/8/28 18:11
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VideoGroupManagementProducer {

    private final UserFeign userFeign;
    private final VideoUserSubscriptionGroupService videoUserSubscriptionGroupService;
    private final VideoUserInfluencerSubscriptionService videoUserInfluencerSubscriptionService;
    private final VideoUserHotSubscriptionService videoUserHotSubscriptionService;
    private final VideoHotSearchService videoHotSearchService;
    private final VideoInfoService videoInfoService;
    private final VideoCountBatchQueryUtil videoCountBatchQueryUtil;

    /**
     * 获取分组列表
     * 要求：
     * 1. 达人订阅分组返回该分组下达人个数
     * 2. 爆款订阅分组返回该分组下爆款个数
     * 3. 默认分组虚拟分组排在第一位
     * 4. 按照sort_order排序，相同则按创建时间排序
     *
     * @param queryBo 查询参数
     * @return 分组列表
     */
    public PageUtils<GroupVo> getGroupList(GroupQueryBo queryBo) {
        // 获取用户信息和租户信息
        UserCacheVo userCacheVo = userFeign.getLocalUser().getData();
        Long userId = userCacheVo.getId();
        Long tenantId = userFeign.getUserTenantId(userId).getData();


        // 构建查询条件
        LambdaQueryWrapper<VideoUserSubscriptionGroupEntity> queryWrapper = Wrappers.<VideoUserSubscriptionGroupEntity>lambdaQuery()
                .eq(VideoUserSubscriptionGroupEntity::getTenantId, tenantId)
                .eq(VideoUserSubscriptionGroupEntity::getUserId, userId);
        /*if (userCacheVo.getUserType() == 2) {
            queryWrapper.eq(VideoUserSubscriptionGroupEntity::getUserId, userId);
        }*/
        queryWrapper.eq(VideoUserSubscriptionGroupEntity::getGroupType, queryBo.getGroupType());

        // 分组名称模糊查询
        if (StrUtil.isNotBlank(queryBo.getGroupName())) {
            queryWrapper.like(VideoUserSubscriptionGroupEntity::getGroupName, queryBo.getGroupName());
        }

        // 排序：，再按创建时间降序
        queryWrapper.orderByDesc(VideoUserSubscriptionGroupEntity::getCreatedDate);

        // 查询分组数据
        Page<VideoUserSubscriptionGroupEntity> videoUserSubscriptionGroupEntityPage = videoUserSubscriptionGroupService.page(new Page<>(queryBo.getPage(), queryBo.getLimit()), queryWrapper);
        List<VideoUserSubscriptionGroupEntity> groupEntities = videoUserSubscriptionGroupEntityPage.getRecords();

        if (CollectionUtil.isEmpty(groupEntities)) {
            // 没有自定义分组时，仍需要统计默认分组的数据
            GroupMemberInfo defaultInfo = getDefaultGroupInfo(queryBo.getGroupType(), userId, tenantId);
            return new PageUtils<>(List.of(createDefaultGroup(queryBo.getGroupType(), defaultInfo.memberCount(), defaultInfo.videoCount())), 1, queryBo.getLimit(), queryBo.getPage());
        }
        // 获取用户信息
        List<Long> userIds = groupEntities.stream().map(VideoUserSubscriptionGroupEntity::getUserId).toList();
        if (CollectionUtil.isEmpty(userIds)) {
            GroupMemberInfo defaultInfo = getDefaultGroupInfo(queryBo.getGroupType(), userId, tenantId);
            return new PageUtils<>(List.of(createDefaultGroup(queryBo.getGroupType(), defaultInfo.memberCount(), defaultInfo.videoCount())), 1, queryBo.getLimit(), queryBo.getPage());
        }
        List<UserDto> userDtoList = userFeign.listByIds(userIds);
        if (CollectionUtil.isEmpty(userDtoList)) {
            GroupMemberInfo defaultInfo = getDefaultGroupInfo(queryBo.getGroupType(), userId, tenantId);
            return new PageUtils<>(List.of(createDefaultGroup(queryBo.getGroupType(), defaultInfo.memberCount(), defaultInfo.videoCount())), 1, queryBo.getLimit(), queryBo.getPage());
        }
        Map<Long, UserDto> userDtoMap = userDtoList.stream().collect(Collectors.toMap(UserDto::getId, Function.identity(), (existing, replacement) -> existing));
        // 统计各分组的成员数量和视频数量
        Map<Long, GroupMemberInfo> memberCountMap = getMemberCountMap(groupEntities, queryBo.getGroupType(), userId, tenantId);

        // 构建返回结果
        List<GroupVo> groupVoList = new ArrayList<>();

        // 1. 添加默认分组（虚拟分组，排在第一位）
        GroupMemberInfo defaultInfo = memberCountMap.getOrDefault(null, new GroupMemberInfo(0, 0));
        GroupVo defaultGroup = createDefaultGroup(queryBo.getGroupType(), defaultInfo.memberCount(), defaultInfo.videoCount());
        groupVoList.add(defaultGroup);

        // 2. 添加实际分组
        for (VideoUserSubscriptionGroupEntity entity : groupEntities) {
            GroupMemberInfo memberInfo = memberCountMap.getOrDefault(entity.getId(), new GroupMemberInfo(0, 0));
            GroupVo groupVo = GroupVo.builder()
                    .groupId(entity.getId())
                    .groupName(entity.getGroupName())
                    .groupType(entity.getGroupType())
                    .groupTypeName(getGroupTypeName(entity.getGroupType()))
                    .groupDescription(entity.getDescription())
                    .memberCount(memberInfo.memberCount())
                    .videoCount(memberInfo.videoCount())
                    .createTime(entity.getCreatedDate())
                    .creator(userDtoMap.get(entity.getUserId()) != null ? userDtoMap.get(entity.getUserId()).getNickName() : "未知")
                    .userId(entity.getUserId())
                    .build();
            groupVoList.add(groupVo);
        }

        // 返回分页结果（这里返回所有数据，如需分页可以进一步处理）
        return new PageUtils<>(videoUserSubscriptionGroupEntityPage, groupVoList);
    }

    /**
     * 统计各分组的成员数量和视频数量
     *
     * @param groupEntities 分组实体列表
     * @param groupType     分组类型
     * @param userId        用户ID
     * @param tenantId      租户ID
     * @return 分组ID -> 分组成员信息的映射
     */
    private Map<Long, GroupMemberInfo> getMemberCountMap(List<VideoUserSubscriptionGroupEntity> groupEntities, Byte groupType, Long userId, Long tenantId) {
        Map<Long, GroupMemberInfo> memberCountMap = new HashMap<>();

        if (groupType == 1) {
            // 达人订阅分组：统计达人个数，视频数量设为0
            processInfluencerGroups(groupEntities, tenantId, memberCountMap);

        } else if (groupType == 2) {
            // 爆款订阅分组：统计爆款个数和视频总数
            processHotSearchGroups(groupEntities, tenantId, memberCountMap);
        }

        return memberCountMap;
    }

    /**
     * 处理达人订阅分组的成员数量统计
     *
     * @param groupEntities  分组实体列表
     * @param tenantId       租户ID
     * @param memberCountMap 结果映射
     */
    private void processInfluencerGroups(List<VideoUserSubscriptionGroupEntity> groupEntities, Long tenantId, Map<Long, GroupMemberInfo> memberCountMap) {
        // 1. 一次性查询所有达人订阅数据（包括默认分组）
        List<VideoUserInfluencerSubscriptionEntity> allSubscriptions = videoUserInfluencerSubscriptionService.list(
                Wrappers.<VideoUserInfluencerSubscriptionEntity>lambdaQuery()
                        .eq(VideoUserInfluencerSubscriptionEntity::getTenantId, tenantId)
        );

        // 2. 按分组ID统计成员数量（处理null key问题）
        Map<Long, Long> memberCountByGroup = new HashMap<>();
        for (VideoUserInfluencerSubscriptionEntity subscription : allSubscriptions) {
            Long groupId = subscription.getGroupId();
            memberCountByGroup.merge(groupId, 1L, Long::sum);
        }

        // 3. 为所有分组设置成员数量（达人分组的视频数量固定为0）
        groupEntities.forEach(entity -> {
            int memberCount = memberCountByGroup.getOrDefault(entity.getId(), 0L).intValue();
            memberCountMap.put(entity.getId(), new GroupMemberInfo(memberCount, 0));
        });

        // 4. 设置默认分组的成员数量
        int defaultMemberCount = memberCountByGroup.getOrDefault(null, 0L).intValue();
        memberCountMap.put(null, new GroupMemberInfo(defaultMemberCount, 0));
    }

    /**
     * 处理爆款订阅分组的成员数量和视频数量统计
     *
     * @param groupEntities  分组实体列表
     * @param tenantId       租户ID
     * @param memberCountMap 结果映射
     */
    private void processHotSearchGroups(List<VideoUserSubscriptionGroupEntity> groupEntities, Long tenantId, Map<Long, GroupMemberInfo> memberCountMap) {
        // 1. 获取所有爆款订阅数据（包括默认分组）
        List<VideoUserHotSubscriptionEntity> allSubscriptions = videoUserHotSubscriptionService.list(
                Wrappers.<VideoUserHotSubscriptionEntity>lambdaQuery()
                        .eq(VideoUserHotSubscriptionEntity::getTenantId, tenantId)
        );

        if (CollectionUtil.isEmpty(allSubscriptions)) {
            // 如果没有订阅数据，所有分组的成员数量和视频数量都为0
            groupEntities.forEach(entity -> memberCountMap.put(entity.getId(), new GroupMemberInfo(0, 0)));
            memberCountMap.put(null, new GroupMemberInfo(0, 0));
            return;
        }

        // 2. 按分组ID分类订阅数据（处理null key问题）
        Map<Long, List<VideoUserHotSubscriptionEntity>> subscriptionsByGroup = new HashMap<>();
        for (VideoUserHotSubscriptionEntity subscription : allSubscriptions) {
            Long groupId = subscription.getGroupId();
            subscriptionsByGroup.computeIfAbsent(groupId, k -> new ArrayList<>()).add(subscription);
        }

        // 3. 批量查询所有爆款数据，获取searchId
        List<VideoHotSearchEntity> allQueryList = allSubscriptions.stream()
                .map(item -> VideoHotSearchEntity.builder()
                        .platformType(item.getPlatformType())
                        .searchKeyword(item.getKeyword())
                        .build())
                .distinct() // 去重，避免重复查询相同的关键词
                .toList();

        List<VideoHotSearchEntity> allHotSearchData = videoHotSearchService.listByPlatformTypeAndSearchKeywords(allQueryList);

        // 4. 构建爆款数据映射（平台类型+关键词 -> searchId）
        Map<String, Long> searchIdMap = new HashMap<>();
        if (CollectionUtil.isNotEmpty(allHotSearchData)) {
            searchIdMap = allHotSearchData.stream()
                    .collect(Collectors.toMap(
                            entity -> entity.getPlatformType() + ":" + entity.getSearchKeyword(),
                            VideoHotSearchEntity::getId,
                            (existing, replacement) -> existing // 如果有重复key，保留第一个
                    ));
        }

        // 5. 批量预加载视频数量数据（避免N+1查询）
        Map<String, Integer> videoCountCache = videoCountBatchQueryUtil.batchQueryVideoCountsBySearchIdMap(allSubscriptions, searchIdMap);

        // 6. 统计每个分组的成员数量和视频数量
        for (Map.Entry<Long, List<VideoUserHotSubscriptionEntity>> entry : subscriptionsByGroup.entrySet()) {
            Long groupId = entry.getKey();
            List<VideoUserHotSubscriptionEntity> groupSubscriptions = entry.getValue();

            int memberCount = groupSubscriptions.size();
            Map<String, Long> finalSearchIdMap = searchIdMap;
            int totalVideoCount = groupSubscriptions.stream()
                    .mapToInt(subscription -> {
                        String key = subscription.getPlatformType() + ":" + subscription.getKeyword();
                        Long searchId = finalSearchIdMap.get(key);
                        if (searchId != null && subscription.getSubscriptionLikeCountThreshold() != null) {
                            // 从缓存中获取视频数量，避免数据库查询
                            return videoCountBatchQueryUtil.getVideoCount(videoCountCache, searchId, subscription.getSubscriptionLikeCountThreshold());
                        }
                        return 0;
                    })
                    .sum();

            memberCountMap.put(groupId, new GroupMemberInfo(memberCount, totalVideoCount));
        }

        // 6. 确保所有分组都有数据（包括没有订阅的分组）
        groupEntities.forEach(entity -> {
            if (!memberCountMap.containsKey(entity.getId())) {
                memberCountMap.put(entity.getId(), new GroupMemberInfo(0, 0));
            }
        });

        // 7. 确保默认分组有数据
        if (!memberCountMap.containsKey(null)) {
            memberCountMap.put(null, new GroupMemberInfo(0, 0));
        }
    }

    /**
     * 获取默认分组的成员信息（当没有自定义分组时使用）
     *
     * @param groupType 分组类型
     * @param userId    用户ID
     * @param tenantId  租户ID
     * @return 默认分组成员信息
     */
    private GroupMemberInfo getDefaultGroupInfo(Byte groupType, Long userId, Long tenantId) {
        if (groupType == 1) {
            // 达人订阅：统计group_id为null的订阅数量
            long memberCount = videoUserInfluencerSubscriptionService.count(
                    Wrappers.<VideoUserInfluencerSubscriptionEntity>lambdaQuery()
                            //.eq(VideoUserInfluencerSubscriptionEntity::getUserId, userId)
                            .eq(VideoUserInfluencerSubscriptionEntity::getTenantId, tenantId)
                            .isNull(VideoUserInfluencerSubscriptionEntity::getGroupId)
            );
            return new GroupMemberInfo((int) memberCount, 0);

        } else if (groupType == 2) {
            // 爆款订阅：统计group_id为null的订阅数量和视频数量
            List<VideoUserHotSubscriptionEntity> defaultSubscriptions = videoUserHotSubscriptionService.list(
                    Wrappers.<VideoUserHotSubscriptionEntity>lambdaQuery()
                            //.eq(VideoUserHotSubscriptionEntity::getUserId, userId)
                            .eq(VideoUserHotSubscriptionEntity::getTenantId, tenantId)
                            .isNull(VideoUserHotSubscriptionEntity::getGroupId)
            );

            if (CollectionUtil.isEmpty(defaultSubscriptions)) {
                return new GroupMemberInfo(0, 0);
            }

            // 计算视频数量
            int totalVideoCount = calculateVideoCountForSubscriptions(defaultSubscriptions);
            return new GroupMemberInfo(defaultSubscriptions.size(), totalVideoCount);
        }

        return new GroupMemberInfo(0, 0);
    }

    /**
     * 计算订阅列表的视频数量
     *
     * @param subscriptions 订阅列表
     * @return 视频总数
     */
    private int calculateVideoCountForSubscriptions(List<VideoUserHotSubscriptionEntity> subscriptions) {
        // 构建查询条件
        List<VideoHotSearchEntity> queryList = subscriptions.stream()
                .map(item -> VideoHotSearchEntity.builder()
                        .platformType(item.getPlatformType())
                        .searchKeyword(item.getKeyword())
                        .build())
                .distinct()
                .toList();

        List<VideoHotSearchEntity> hotSearchData = videoHotSearchService.listByPlatformTypeAndSearchKeywords(queryList);

        if (CollectionUtil.isEmpty(hotSearchData)) {
            return 0;
        }

        // 构建searchId映射
        Map<String, Long> searchIdMap = hotSearchData.stream()
                .collect(Collectors.toMap(
                        entity -> entity.getPlatformType() + ":" + entity.getSearchKeyword(),
                        VideoHotSearchEntity::getId,
                        (existing, replacement) -> existing
                ));

        // 批量查询视频数量
        Map<String, Integer> videoCountCache = videoCountBatchQueryUtil.batchQueryVideoCountsBySearchIdMap(subscriptions, searchIdMap);

        // 计算总视频数量
        return subscriptions.stream()
                .mapToInt(subscription -> {
                    String key = subscription.getPlatformType() + ":" + subscription.getKeyword();
                    Long searchId = searchIdMap.get(key);
                    if (searchId != null && subscription.getSubscriptionLikeCountThreshold() != null) {
                        return videoCountBatchQueryUtil.getVideoCount(videoCountCache, searchId, subscription.getSubscriptionLikeCountThreshold());
                    }
                    return 0;
                })
                .sum();
    }


    private record GroupMemberInfo(Integer memberCount, Integer videoCount) {
    }

    /**
     * 创建默认分组（虚拟分组）
     *
     * @param groupType   分组类型
     * @param memberCount 成员数量
     * @param videoCount  视频数量
     * @return 默认分组VO
     */
    private GroupVo createDefaultGroup(Byte groupType, Integer memberCount, Integer videoCount) {
        return GroupVo.builder()
                .groupId(null) // 默认分组没有ID
                .groupName("默认分组")
                .groupType(groupType)
                .groupTypeName(getGroupTypeName(groupType))
                .groupDescription("系统默认分组")
                .memberCount(memberCount)
                .videoCount(videoCount)
                .createTime(LocalDateTime.now()) // 默认分组没有创建时间
                .creator("默认")
                .userId(0L)
                .build();
    }

    /**
     * 获取分组类型名称
     *
     * @param groupType 分组类型
     * @return 分组类型名称
     */
    private String getGroupTypeName(Byte groupType) {
        return switch (groupType) {
            case 1 -> "达人订阅分组";
            case 2 -> "爆款订阅分组";
            default -> "未知分组";
        };
    }

    /**
     * 新增分组
     *
     * @param groupAddBo 新增分组参数
     * @return 是否成功
     */
    public boolean addGroup(GroupAddBo groupAddBo) {
        // 获取用户信息
        UserCacheVo userCacheVo = userFeign.getLocalUser().getData();
        Long userId = userCacheVo.getId();
        Long tenantId = userFeign.getUserTenantId(userId).getData();

        // 检查分组名称是否重复
        boolean exists = videoUserSubscriptionGroupService.count(
                Wrappers.<VideoUserSubscriptionGroupEntity>lambdaQuery()
                        .eq(VideoUserSubscriptionGroupEntity::getTenantId, tenantId)
                        .eq(VideoUserSubscriptionGroupEntity::getUserId, userId)
                        .eq(VideoUserSubscriptionGroupEntity::getGroupType, groupAddBo.getGroupType())
                        .eq(VideoUserSubscriptionGroupEntity::getGroupName, groupAddBo.getGroupName())
        ) > 0;

        if (exists) {
            throw new BusinessException("分组名称已存在");
        }

        // 创建分组实体
        VideoUserSubscriptionGroupEntity groupEntity = new VideoUserSubscriptionGroupEntity();
        groupEntity.setId(SnowflakeManager.nextValue());
        groupEntity.setUserId(userId);
        groupEntity.setTenantId(tenantId);
        groupEntity.setGroupName(groupAddBo.getGroupName());
        groupEntity.setGroupType(groupAddBo.getGroupType());
        groupEntity.setDescription(groupAddBo.getGroupDescription());
        groupEntity.setCreatedDate(LocalDateTime.now());
        groupEntity.setUpdateDate(LocalDateTime.now());
        groupEntity.setIsDeleted((byte) 0);

        return videoUserSubscriptionGroupService.save(groupEntity);
    }

    /**
     * 编辑分组
     *
     * @param groupEditBo 编辑分组参数
     * @return 是否成功
     */
    public boolean editGroup(GroupEditBo groupEditBo) {
        // 获取用户信息
        UserCacheVo userCacheVo = userFeign.getLocalUser().getData();
        Long userId = userCacheVo.getId();
        Long tenantId = userFeign.getUserTenantId(userId).getData();

        // 查询分组是否存在
        VideoUserSubscriptionGroupEntity groupEntity = videoUserSubscriptionGroupService.getOne(
                Wrappers.<VideoUserSubscriptionGroupEntity>lambdaQuery()
                        .eq(VideoUserSubscriptionGroupEntity::getId, groupEditBo.getGroupId())
                        .eq(VideoUserSubscriptionGroupEntity::getTenantId, tenantId)
        );
        Assert.notNull(groupEntity, "分组不存在或无权限操作");
        if (userCacheVo.getUserType() == 2) {
            Assert.isTrue(Objects.equals(groupEntity.getUserId(), userId), "无权限操作");
        }

        // 检查分组名称是否重复（排除自己）
        boolean exists = videoUserSubscriptionGroupService.count(
                Wrappers.<VideoUserSubscriptionGroupEntity>lambdaQuery()
                        .eq(VideoUserSubscriptionGroupEntity::getTenantId, tenantId)
                        .eq(VideoUserSubscriptionGroupEntity::getUserId, userId)
                        .eq(VideoUserSubscriptionGroupEntity::getGroupType, groupEntity.getGroupType())
                        .eq(VideoUserSubscriptionGroupEntity::getGroupName, groupEditBo.getGroupName())
                        .ne(VideoUserSubscriptionGroupEntity::getId, groupEditBo.getGroupId())
        ) > 0;

        if (exists) {
            throw new BusinessException("分组名称已存在");
        }

        // 更新分组信息
        groupEntity.setGroupName(groupEditBo.getGroupName());
        groupEntity.setDescription(groupEditBo.getGroupDescription());
        groupEntity.setUpdateDate(LocalDateTime.now());

        return videoUserSubscriptionGroupService.updateById(groupEntity);
    }

    /**
     * 删除分组
     *
     * @param groupId 分组ID
     * @return 是否成功
     */
    public boolean deleteGroup(Long groupId) {
        // 获取用户信息
        UserCacheVo userCacheVo = userFeign.getLocalUser().getData();
        Long userId = userCacheVo.getId();
        Long tenantId = userFeign.getUserTenantId(userId).getData();

        // 查询分组是否存在
        VideoUserSubscriptionGroupEntity groupEntity = videoUserSubscriptionGroupService.getOne(
                Wrappers.<VideoUserSubscriptionGroupEntity>lambdaQuery()
                        .eq(VideoUserSubscriptionGroupEntity::getId, groupId)
                        .eq(VideoUserSubscriptionGroupEntity::getTenantId, tenantId)

        );
        Assert.notNull(groupEntity, "分组不存在或无权限操作");
        if (userCacheVo.getUserType() == 2) {
            Assert.isTrue(Objects.equals(groupEntity.getUserId(), userId), "无权限操作");
        }

        // 检查分组下是否有成员
        boolean hasMember = checkGroupHasMember(groupId, groupEntity.getGroupType(), userId, tenantId);
        if (hasMember) {
            if (groupEntity.getGroupType() == 1) {
                throw new BusinessException("分组下存在达人订阅，不能删除！");
            } else {
                throw new BusinessException("分组下存在爆款订阅，不能删除！");
            }
        }

        // 删除分组（逻辑删除）
        return videoUserSubscriptionGroupService.removeById(groupEntity);
    }

    /**
     * 检查分组下是否有成员
     *
     * @param groupId   分组ID
     * @param groupType 分组类型
     * @param userId    用户ID
     * @param tenantId  租户ID
     * @return 是否有成员
     */
    private boolean checkGroupHasMember(Long groupId, Byte groupType, Long userId, Long tenantId) {
        if (groupType == 1) {
            // 达人订阅分组
            return videoUserInfluencerSubscriptionService.count(
                    Wrappers.<VideoUserInfluencerSubscriptionEntity>lambdaQuery()
                            .eq(VideoUserInfluencerSubscriptionEntity::getGroupId, groupId)
            ) > 0;
        } else if (groupType == 2) {
            // 爆款订阅分组
            return videoUserHotSubscriptionService.count(
                    Wrappers.<VideoUserHotSubscriptionEntity>lambdaQuery()
                            .eq(VideoUserHotSubscriptionEntity::getGroupId, groupId)
            ) > 0;
        }
        return false;
    }

    /**
     * 获取分组选项列表
     * 用于下拉选择框，返回简化的分组信息
     *
     * @param groupType 分组类型
     * @return 分组选项列表
     */
    public List<GroupVo> getGroupOptions(Byte groupType) {
        // 获取用户信息
        UserCacheVo userCacheVo = userFeign.getLocalUser().getData();
        Long userId = userCacheVo.getId();
        Long tenantId = userFeign.getUserTenantId(userId).getData();

        // 查询分组数据
        LambdaQueryWrapper<VideoUserSubscriptionGroupEntity> queryWrapper = Wrappers.<VideoUserSubscriptionGroupEntity>lambdaQuery()
                .eq(VideoUserSubscriptionGroupEntity::getTenantId, tenantId)
                .eq(VideoUserSubscriptionGroupEntity::getUserId, userId);

        List<VideoUserSubscriptionGroupEntity> groupEntities = videoUserSubscriptionGroupService.list(
                queryWrapper.eq(VideoUserSubscriptionGroupEntity::getGroupType, groupType)
                        .orderByDesc(VideoUserSubscriptionGroupEntity::getCreatedDate)
        );

        // 构建返回结果
        List<GroupVo> groupOptions = new ArrayList<>();

        // 1. 添加默认分组选项（排在第一位）
        GroupVo defaultOption = GroupVo.builder()
                .groupId(null) // 默认分组ID为null
                .groupName("默认分组")
                .groupType(groupType)
                .groupTypeName(getGroupTypeName(groupType))
                .build();
        groupOptions.add(defaultOption);

        // 2. 添加用户创建的分组选项
        for (VideoUserSubscriptionGroupEntity entity : groupEntities) {
            GroupVo groupOption = GroupVo.builder()
                    .groupId(entity.getId())
                    .groupName(entity.getGroupName())
                    .groupType(entity.getGroupType())
                    .groupTypeName(getGroupTypeName(entity.getGroupType()))
                    .build();
            groupOptions.add(groupOption);
        }

        return groupOptions;
    }

    /**
     * 获取分组详情
     *
     * @param groupId 分组ID
     * @return 分组详情
     */
    public GroupVo getGroupDetail(Long groupId) {
        // 获取用户信息
        UserCacheVo userCacheVo = userFeign.getLocalUser().getData();
        Long userId = userCacheVo.getId();
        Long tenantId = userFeign.getUserTenantId(userId).getData();

        // 查询分组信息
        VideoUserSubscriptionGroupEntity groupEntity = videoUserSubscriptionGroupService.getOne(
                Wrappers.<VideoUserSubscriptionGroupEntity>lambdaQuery()
                        .eq(VideoUserSubscriptionGroupEntity::getId, groupId)
                        .eq(VideoUserSubscriptionGroupEntity::getTenantId, tenantId)
        );

        Assert.notNull(groupEntity, "分组不存在或无权限操作！");
        if (userCacheVo.getUserType() == 2) {
            Assert.isTrue(Objects.equals(groupEntity.getUserId(), userId), "无权限操作");
        }

        // 统计分组成员数量
        int memberCount = 0;
        if (groupEntity.getGroupType() == 1) {
            // 达人订阅分组
            memberCount = (int) videoUserInfluencerSubscriptionService.count(
                    Wrappers.<VideoUserInfluencerSubscriptionEntity>lambdaQuery()
                            .eq(VideoUserInfluencerSubscriptionEntity::getGroupId, groupId)
            );
        } else if (groupEntity.getGroupType() == 2) {
            // 爆款订阅分组
            memberCount = (int) videoUserHotSubscriptionService.count(
                    Wrappers.<VideoUserHotSubscriptionEntity>lambdaQuery()
                            .eq(VideoUserHotSubscriptionEntity::getGroupId, groupId)
            );
        }
        UserDto userDto = userFeign.userById(groupEntity.getUserId());
        // 构建返回结果
        GroupVo groupVo = GroupVo.builder()
                .groupId(groupEntity.getId())
                .groupName(groupEntity.getGroupName())
                .groupType(groupEntity.getGroupType())
                .groupTypeName(getGroupTypeName(groupEntity.getGroupType()))
                .groupDescription(groupEntity.getDescription())
                .memberCount(memberCount)
                .createTime(groupEntity.getCreatedDate())
                .creator(userDto != null ? userDto.getNickName() : "未知")
                .build();

        return groupVo;
    }
}
