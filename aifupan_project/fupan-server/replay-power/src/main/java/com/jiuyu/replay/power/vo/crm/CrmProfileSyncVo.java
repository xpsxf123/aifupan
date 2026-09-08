package com.jiuyu.replay.power.vo.crm;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * CRM 画像字段同步响应对象
 */
@Data
@Schema(description = "CRM 画像字段同步响应")
public class CrmProfileSyncVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 客户ID
     */
    @Schema(description = "客户ID")
    private Long userId;

    /**
     * 客户手机号
     */
    @Schema(description = "客户手机号")
    private String phone;

    /**
     * 数据来源
     */
    @Schema(description = "数据来源")
    private String source;

    /**
     * 业务更新时间
     */
    @Schema(description = "业务更新时间")
    private String updatedAt;

    /**
     * 用户意向
     */
    @Schema(description = "用户意向")
    private String userAmbition;

    /**
     * 客户类型
     */
    @Schema(description = "客户类型")
    private Integer userBelongType;

    /**
     * 实际更新字段
     */
    @Schema(description = "实际更新字段")
    private List<String> updatedFields;
}
