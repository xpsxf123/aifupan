package com.jiuyu.replay.power.vo.crm;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * CRM 客户行为信号批量查询响应
 * 承载按手机号返回的使用深度信号集合
 */
@Data
@Schema(description = "CRM 客户行为信号批量查询响应")
public class CrmBehaviorSignalsVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 行为信号列表（按入参 phones 顺序回填）
     */
    @Schema(description = "行为信号列表")
    private List<CrmBehaviorSignalItemVo> signals = new ArrayList<>();
}
