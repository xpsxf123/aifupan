package com.jiuyu.replay.order.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/8/6 下午4:15
 */
@Data
@Schema(description = "更新巨量监控位的资产入参")
public class UpdateRpaAmountNumBo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "数量")
    private Long num;

}
