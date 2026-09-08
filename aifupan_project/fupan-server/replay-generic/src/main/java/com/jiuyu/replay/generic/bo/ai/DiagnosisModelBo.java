package com.jiuyu.replay.generic.bo.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * ai诊断中的模型设置-主播和视频信息
 *
 * @author lj
 * @email 
 * @date 2025-05-19 14:02:59
 */
@Data
@Schema(description = "ai诊断中的模型设置-主播和视频信息")
public class DiagnosisModelBo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	@Schema(description = "")
	private Long id;
	/**
	 * 来源id
	 */
	@Schema(description = "来源id")
	private String sourceId;
	/**
	 * 来源类型 0主播 1视频
	 */
	@Schema(description = "来源类型 0主播 1视频")
	private Integer sourceType;
	/**
	 * 模型id
	 */
	@Schema(description = "模型id")
	private Long modelId;
    /**
     * 诊断报告类型 0：内容诊断，1：数据诊断
     */
    @Schema(description = "诊断报告类型 0：内容诊断，1：数据诊断")
    private Integer diagnosisType;

    @Schema(description = "是否选择数据截图 0没有，1有")
    private Integer selectDataScreenshot;

    @Schema(description = "是否选择数据看版 0没有，1有")
    private Integer selectBoard;
	/**
	 * 用户id
	 */
	@Schema(description = "用户id")
	private Long userId;
	/**
	 * 租户id
	 */
	@Schema(description = "租户id")
	private Long tenantId;
	/**
	 * 更新人
	 */
	@Schema(description = "更新人")
	private Long updateUserId;
	/**
	 * 更新时间
	 */
	@Schema(description = "更新时间")
	private Date updateDate;
	/**
	 * 创建人
	 */
	@Schema(description = "创建人")
	private Long createUserId;
	/**
	 * 创建时间
	 */
	@Schema(description = "创建时间")
	private Date createDate;
	/**
	 * 是否已删除
	 */
	@Schema(description = "是否已删除")
	private Integer isDeleted;


}
