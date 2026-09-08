package com.jiuyu.replay.generic.bo.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * ai诊断提示词配置信息
 *
 * @author lj
 * @email 
 * @date 2025-05-19 14:02:59
 */
@Data
@Schema(description = "ai诊断提示词配置信息")
public class DiagnosisCueBo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * id
	 */
	@Schema(description = "id")
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
	 * 行业id
	 */
	@Schema(description = "行业id")
	private Long tradeId;
	/**
	 * 提示词id
	 */
	@Schema(description = "提示词id")
	private Long cueWordsId;
	/**
	 * 状态 0未处理 1处理中 2处理完成 3处理失败
	 */
	@Schema(description = "状态 0未处理 1处理中 2处理完成 3处理失败")
	private Integer qaStatus;
	/**
     * 最新分析时间
     */
    @Schema(description = "最新分析时间")
    private Date qaHandleTime;
    /**
	 * 错误内容
	 */
	@Schema(description = "错误内容")
	private String errorContent;
	/**
	 * 已选中，0否，1是
	 */
	@Schema(description = "已选中，0否，1是")
	private Integer isSelected;
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
