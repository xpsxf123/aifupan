package com.jiuyu.replay.video.project.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 热搜邮箱账号 Redis 存储 DTO
 *
 * @author RayChou
 * @date 2025-11-28
 * @description 存储在 Redis Hash 中的账号详情
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HotSearchAccountRedisDTO {

    /**
     * 账号ID
     */
    private String accountId;

    /**
     * 可用次数（剩余并发数）
     * 计算公式：max_concurrent_users - current_user_count
     */
    private Integer availableCount;

    /**
     * 城市（账号归属城市或最后使用城市）
     */
    private String city;

    /**
     * 最后使用时间戳（毫秒）
     */
    private Long lastUseTime;

    /**
     * 最后使用的 IP
     */
    private String lastIp;

    /**
     * 是否来自缓存（IP 映射）
     */
    private Boolean fromCache;

    /**
     * 当前所在池 key（用于释放时精确删除）
     */
    private String currentPool;

    /**
     * 最后所在池 key（当 availableCount = 0 时记录，用于释放时恢复）
     */
    private String lastPool;
}

