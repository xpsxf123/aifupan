package com.jiuyu.replay.power.bo.crm;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * CRM AI 画像写入请求对象
 */
@Data
@Schema(description = "CRM AI画像写入请求")
public class CrmAiProfileUpsertBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 客户手机号
     */
    @NotBlank(message = "客户手机号不能为空")
    @Schema(description = "客户手机号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String phone;

    /**
     * 画像版本标识
     */
    @NotBlank(message = "画像版本标识不能为空")
    @Schema(description = "画像版本标识", requiredMode = Schema.RequiredMode.REQUIRED)
    private String profileId;

    /**
     * 来源
     */
    @Schema(description = "来源")
    private String source = "";

    /**
     * 业务更新时间
     */
    @Schema(description = "业务更新时间")
    private String updatedAt = "";

    /**
     * AI 画像完整 JSON
     */
    @Schema(description = "AI画像完整JSON")
    private String profileJson = "";

    /**
     * 画像摘要
     */
    @Schema(description = "画像摘要")
    private String summary = "";
}
