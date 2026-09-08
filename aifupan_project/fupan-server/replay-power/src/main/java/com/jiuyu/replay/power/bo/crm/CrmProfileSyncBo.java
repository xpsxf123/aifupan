package com.jiuyu.replay.power.bo.crm;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * CRM 画像字段同步请求对象
 */
@Data
@Schema(description = "CRM 画像字段同步请求")
public class CrmProfileSyncBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 客户手机号
     */
    @NotBlank(message = "客户手机号不能为空")
    @Schema(description = "客户手机号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String phone;

    /**
     * 来源
     */
    @Schema(description = "来源")
    private String source = "";

    /**
     * 业务更新时间
     */
    @Schema(description = "业务更新时间")
    private String updatedAt;

    /**
     * 成交意向
     */
    @Schema(description = "成交意向")
    private String dealIntent = "";

    /**
     * 客户类型
     */
    @Schema(description = "客户类型")
    private String leadType = "";
}
