package com.jiuyu.replay.words.rse.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jiuyu.replay.generic.dto.words.UserAnchorCountDto;
import com.jiuyu.replay.words.entity.AnchorUrlUserEntity;
import com.jiuyu.replay.words.repository.service.AnchorUrlUserService;
import com.jiuyu.replay.words.rse.AnchorUrlUserRse;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 主播用户关联RSE实现类
 *
 * @author AI Assistant
 */
@Service
public class AnchorUrlUserRseImpl implements AnchorUrlUserRse {

    @Resource
    private AnchorUrlUserService anchorUrlUserService;

    @Override
    public List<UserAnchorCountDto> getAnchorCountByUserIdsAndTenantId(List<Long> userIds, Long tenantId) {
        if (userIds == null || userIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 使用 MyBatis-Plus 的聚合查询
        List<Map<String, Object>> queryResult = anchorUrlUserService.listMaps(
                new QueryWrapper<AnchorUrlUserEntity>()
                        .select("user_id, COUNT(*) as anchor_count")
                        .in("user_id", userIds)
                        .eq("tenant_id", tenantId)
                        .eq("is_remove_record", 0)
                        .groupBy("user_id"));

        // 创建用户ID到数量的映射
        Map<Long, Integer> countMap = new HashMap<>();
        for (Map<String, Object> row : queryResult) {
            Long userId = Long.valueOf(row.get("user_id").toString());
            Integer count = Integer.valueOf(row.get("anchor_count").toString());
            countMap.put(userId, count);
        }

        // 转换为DTO列表
        List<UserAnchorCountDto> result = new ArrayList<>();
        for (Long userId : userIds) {
            UserAnchorCountDto dto = new UserAnchorCountDto();
            dto.setUserId(userId);
            dto.setAnchorCount(countMap.getOrDefault(userId, 0));
            result.add(dto);
        }

        return result;
    }

    @Override
    public long countAuthUsed(Long userId, Long tenantId, Integer authType) {
        if (authType == null || authType < 1 || authType > 3) {
            throw new IllegalArgumentException("不支持的授权类型: " + authType);
        }
        LambdaQueryWrapper<AnchorUrlUserEntity> wrapper = new LambdaQueryWrapper<AnchorUrlUserEntity>()
                .eq(AnchorUrlUserEntity::getUserId, userId)
                .eq(AnchorUrlUserEntity::getTenantId, tenantId)
                .eq(AnchorUrlUserEntity::getIsDeleted, 0)
                .eq(AnchorUrlUserEntity::getIsRemoveRecord, 0);
        switch (authType) {
            case 1 -> wrapper.eq(AnchorUrlUserEntity::getAuthJlbyStatus, 1);
            case 2 -> wrapper.eq(AnchorUrlUserEntity::getAuthQcStatus, 1);
            case 3 -> wrapper.eq(AnchorUrlUserEntity::getAuthLifeStatus, 1);
        }
        return anchorUrlUserService.count(wrapper);
    }

    @Override
    public Set<String> listYesterdayAnchorSecUids() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        String yesterdayStart = yesterday + " 00:00:00";
        String yesterdayEnd = yesterday + " 23:59:59";

        LambdaQueryWrapper<AnchorUrlUserEntity> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.between(AnchorUrlUserEntity::getCreateDate, yesterdayStart, yesterdayEnd)
                .select(AnchorUrlUserEntity::getAnchorUrlSecUid);
        List<AnchorUrlUserEntity> yesterdayAnchors = anchorUrlUserService.list(userWrapper);

        if (yesterdayAnchors == null || yesterdayAnchors.isEmpty()) {
            return new HashSet<>();
        }

        return yesterdayAnchors.stream()
                .map(AnchorUrlUserEntity::getAnchorUrlSecUid)
                .collect(Collectors.toSet());
    }

    @Override
    public List<String> listRecentAnchorSecUids(int pageNum, int pageSize) {
        LambdaQueryWrapper<AnchorUrlUserEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(AnchorUrlUserEntity::getCreateDate)
                .select(AnchorUrlUserEntity::getAnchorUrlSecUid);

        Page<AnchorUrlUserEntity> page = anchorUrlUserService.page(new Page<>(pageNum, pageSize), wrapper);
        List<AnchorUrlUserEntity> records = page.getRecords();

        if (records == null || records.isEmpty()) {
            return new ArrayList<>();
        }

        return records.stream()
                .map(AnchorUrlUserEntity::getAnchorUrlSecUid)
                .distinct()
                .collect(Collectors.toList());
    }
}
