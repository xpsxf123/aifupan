package com.jiuyu.governance.business.performance.pojo.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jiuyu.governance.business.performance.pojo.base.BasePerformanceMetrics;
import com.jiuyu.governance.business.performance.pojo.bo.CommodityProcessDataBo;
import com.jiuyu.governance.business.performance.pojo.bo.OceanEngineProcessBo;
import com.jiuyu.governance.business.room.pojo.constants.LivePlatformType;
import com.jiuyu.governance.plugins.webmvc.serializer.EnumDesc;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 *
 *
 * @author ：lujie
 * @date ：2026/4/13 11:16
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class ClientPushVideoRequest extends BasePerformanceMetrics implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 租户ID
     */
    @JsonIgnore
    private Long tenantId;

    /**
     * 直播批次号
     */
    @NotBlank(message = "直播批次号不能为空")
    @Size(max = 100,message = "直播批次号最大长度要小于 100")
    private String batchNumber;

    /**
     * 视频开始时间
     */
    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    /**
     * 视频结束时间
     */
    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;

    /**
     * 主播secUid
     */
    @NotBlank(message = "主播secUid不能为空")
    @Size(max = 128,message = "主播secUid最大长度要小于 128")
    private String secUid;

    /**
     * 平台类型：0-抖音，1-快手，2-视频号
     */
    @NotNull(message = "平台类型不能为空")
    @EnumDesc(LivePlatformType.class)
    private Integer platform;

    /**
     * 直播过程数据
     */
    @Valid
    @NotNull(message = "直播过程数据不能为空")
    private List<OceanEngineProcessBo> oceanEngineProcessList;

    /**
     * 商品列表
     */
    @Valid
    private List<VideoProductRequest> productList;

}
