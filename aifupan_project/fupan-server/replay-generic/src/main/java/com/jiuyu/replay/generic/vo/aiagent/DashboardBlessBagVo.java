package com.jiuyu.replay.generic.vo.aiagent;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 数据看板-福袋（压缩版）。
 *
 * <p>仅保留模型分析所需的「时点 + 奖品 + 参与人数」，省去多份冗余时间戳，控制内容体积。
 * 源自在线曲线的福袋数据（§4.8 onlineChartData）。</p>
 *
 * @author fupan-server
 */
@Data
public class DashboardBlessBagVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 相对开播毫秒（便于定位「第几分钟」开的福袋）
     */
    @Schema(description = "相对开播毫秒")
    private Long relativeTime;

    /**
     * 福袋奖品
     */
    @Schema(description = "福袋奖品")
    private String blessBagReward;

    /**
     * 参与人数
     */
    @Schema(description = "参与人数")
    private Integer candidateNum;
}
