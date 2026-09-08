package com.jiuyu.replay.words.repository.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jiuyu.replay.generic.vo.words.AnchorAuthStatusVo;
import com.jiuyu.replay.words.entity.AnchorUrlUserEntity;
import jakarta.validation.constraints.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public interface AnchorUrlUserService extends IService<AnchorUrlUserEntity> {
    /**
     * 根据用户usrid统计绑定主播数量
     * @param userId
     * @return
     */
    Integer sum(Long userId);

    List<AnchorUrlUserEntity> listBySecUid(List<String> secUids,Long userId,Long tenantId);


    /**
     * 获取主播的所属行业
     * @param secUids 主播secUid
     */
    Map<String, List<Long>> getAnchorTradeMap(List<String> secUids);

    /**
     * 获取主播的所属行业 - 最多选择的行业
     * @param secUids 主播secUid
     */
    default Map<String, Long> getAnchorTopTradeMap(List<String> secUids) {
        return getAnchorTradeMap(secUids)
            .entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> {
                List<Long> tradeIds = entry.getValue();
                // 从list中取最多重复的一个值
                // 预设初始容量以减少rehashing
                Map<Long, Integer> countMap = new HashMap<>((int) Math.ceil(tradeIds.size() / 0.75));

                Long maxRepeatedValue = null;
                int maxCount = 0;

                // 单次遍历同时完成计数和查找最大值
                for (Long value : tradeIds) {
                    int count = countMap.merge(value, 1, Integer::sum); // merge操作一步完成计数
                    if (count > maxCount) {
                        maxCount = count;
                        maxRepeatedValue = value;
                    }
                }
                if (maxRepeatedValue == null) {
                    return tradeIds.get(0);
                }

                return maxRepeatedValue;
            }));
    }

    /**
     * 获取当前租户已添加主播的行业ID
     * @param tenantId 租户ID
     */
    List<Long> getTenantTradeIds(long tenantId);

    /**
     * 根据secUid查询主播授权状态
     *
     * @param secUid   主播唯一标识
     * @param userId   用户ID
     * @param tenantId 租户ID
     * @return 授权状态VO，不存在返回null
     */
    AnchorAuthStatusVo getAuthStatusBySecUid(String secUid, Long userId, Long tenantId);
}
