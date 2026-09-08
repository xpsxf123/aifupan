package com.jiuyu.governance.business.performance.pojo.bo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 待处理视频分组信息
 *
 * @author lj
 * @date 2026-03-23
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PendingVideoGroup {

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 直播批次号
     */
    private String batchNumber;
}
