package com.jiuyu.replay.words.bo.file;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 文件的详情信息
 *
 * @author lyw
 * @email 1798883178@qq.com
 * @date 2025-06-03 14:14:53
 */
@Data
@Schema(description = "文件的详情信息")
public class UploadFileDetailBo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@Schema(description = "ID")
	private Long id;
	/**
	 * 文件唯一标识
	 */
	@Schema(description = "文件唯一标识")
	private String fileId;
	/**
	 * 自然原文生成状态 0待生成，1生成中，2生成成功，3生成失败
	 */
	@Schema(description = "自然原文生成状态 0待生成，1生成中，2生成成功，3生成失败")
	private Integer natureContentStatus;
	/**
	 * 优化原文生成状态 0待生成，1生成中，2生成成功，3生成失败
	 */
	@Schema(description = "优化原文生成状态 0待生成，1生成中，2生成成功，3生成失败")
	private Integer optimizeContentStatus;
	/**
	 * 是否上传诊断报告 0否，1是
	 */
	@Schema(description = "是否上传诊断报告 0否，1是")
	private Integer hasDiagnosisReport;
    /**
     * 是否已推荐行业 0未推荐，1已推荐
     */
    @Schema(description = "是否已推荐行业 0未推荐，1已推荐")
    private Integer suggestTrade;
    /**
     * 推荐行业Id
     */
    @Schema(description = "推荐行业Id")
    private Long suggestTradeId;
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
	 * 创建时间
	 */
	@Schema(description = "创建时间")
	private Date createDate;
	/**
	 * 更新时间
	 */
	@Schema(description = "更新时间")
	private Date updateDate;
	/**
	 * 是否已删除
	 */
	@Schema(description = "是否已删除")
	private Integer isDeleted;


}
