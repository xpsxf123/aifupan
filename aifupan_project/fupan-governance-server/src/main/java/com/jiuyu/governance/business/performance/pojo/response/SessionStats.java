package com.jiuyu.governance.business.performance.pojo.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 直播场次统计
 * <p>
 * 包含场次数量和直播时长
 * </p>
 *
 * @author lj
 * @date 2026-03-24
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionStats {

    /**
     * 场次数量
     * 统计口径：按实际开播场次，一场直播对应多条业绩记录算一条
     */
    private Integer count;

    /**
     * 直播时长（单位：分钟）
     * 计算公式：(结束时间 - 开始时间) / 60
     */
    private Integer duration;
}
