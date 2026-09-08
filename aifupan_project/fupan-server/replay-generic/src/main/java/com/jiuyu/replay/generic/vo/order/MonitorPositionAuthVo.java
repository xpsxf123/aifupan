package com.jiuyu.replay.generic.vo.order;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 监控位授权量信息
 *
 * <p>用于表示某类 AI 监控能力（话术质检/话术还原度/互动巡检）的授权量、使用量及剩余量，
 * 供前端展示资产统计和控制开关开启逻辑。</p>
 *
 * @author beta
 * @date 2026-06-02
 */
@Data
@Schema(description = "监控位授权量信息")
public class MonitorPositionAuthVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 资产 code，如 scriptQualityNum / scriptFidelityNum / interactionPatrolNum
     */
    @Schema(description = "资产 code")
    private String code;

    /**
     * 监控能力名称，如"话术质检监控位"
     */
    @Schema(description = "监控能力名称")
    private String name;

    /**
     * 是否有授权（totalQuantity > 0），用于自动触发校验；仅供内部 SPI 判断，不序列化给前端
     */
    @JsonIgnore
    @Schema(description = "是否有授权（totalQuantity > 0）")
    private Boolean hasAuth;

    /**
     * 是否有剩余（surplus > 0），用于开关开启时校验；仅供内部 SPI 判断，不序列化给前端
     */
    @JsonIgnore
    @Schema(description = "是否有剩余（surplus > 0）")
    private Boolean hasSurplus;

    /**
     * 授权总量
     */
    @Schema(description = "授权总量")
    private Long totalQuantity;

    /**
     * 已使用量
     */
    @Schema(description = "已使用量")
    private Long useQuantity;

    /**
     * 剩余量 = max(totalQuantity - useQuantity, 0)，前端字段 remainingQuantity
     */
    @Schema(description = "剩余量（remainingQuantity = max(totalQuantity - useQuantity, 0)）")
    private Long remainingQuantity;
}
