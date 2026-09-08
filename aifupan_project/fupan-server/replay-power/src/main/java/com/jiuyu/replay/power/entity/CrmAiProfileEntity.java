package com.jiuyu.replay.power.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * CRM AI画像表
 */
@Data
@TableName("tb_crm_ai_profile")
@Schema(description = "CRM AI画像表")
public class CrmAiProfileEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.INPUT)
    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "客户ID(user_id)")
    private Long userId;
    @Schema(description = "画像版本标识(profile_id)，用于幂等覆盖")
    private String profileId;
    @Schema(description = "来源")
    private String source;
    @Schema(description = "业务更新时间")
    private Date updatedAt;
    @Schema(description = "AI画像完整JSON")
    private String profileJson;
    @Schema(description = "画像摘要")
    private String summary;
    @Schema(description = "删除标记：0未删除/1已删除")
    private Integer isDeleted;
    @Schema(description = "创建时间")
    private Date createDate;
    @Schema(description = "更新时间")
    private Date updateDate;
}
