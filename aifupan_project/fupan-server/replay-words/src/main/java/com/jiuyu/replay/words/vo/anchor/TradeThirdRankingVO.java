package com.jiuyu.replay.words.vo.anchor;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 行业热榜第三方榜单关联
 *
 * @author HeHui
 * @date 2026-01-28 16:12
 */
@Getter
@Setter
public class TradeThirdRankingVO implements Serializable {
    @Serial
    private static final long serialVersionUID = -5227617543600639690L;

    /**
     * 关联表的ID 删除接口使用
     */
    private Long id;

    /**
     * 行业ID
     */
    private Long tradeId;

    /**
     * 第三方榜单唯一ID（第三方平台的榜单ID）
     */
    private String thirdRankingId;

    /**
     * 第三方榜单名称
     */
    private String thirdRankingName;

    /**
     * 上次采集时间
     */
    private LocalDateTime lastCollectTime;

    /**
     * 是否采集
     */
    private Boolean enableCollect;

    /**
     * 每个周期的周几采集 1-7
     */
    private Integer dayOfWeek;



}
