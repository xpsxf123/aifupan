package com.jiuyu.replay.power.vo.crm;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * CRM AI 画像写入响应对象
 */
@Data
@Schema(description = "CRM AI画像写入响应")
public class CrmAiProfileUpsertVo implements Serializable {

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
     * 画像版本标识
     */
    @Schema(description = "画像版本标识")
    private String profileId;
}
