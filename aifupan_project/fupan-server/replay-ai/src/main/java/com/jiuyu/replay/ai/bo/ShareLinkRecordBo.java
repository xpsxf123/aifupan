package com.jiuyu.replay.ai.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 分享链接记录信息
 *
 * @author lyw
 * @email 1798883178@qq.com
 * @date 2025-05-21 18:39:53
 */
@Data
@Schema(description = "分享链接记录信息")
public class ShareLinkRecordBo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 链接分享记录  主键
	 */
	@Schema(description = "链接分享记录  主键")
	private Long id;
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
	 * 分享时间
	 */
	@Schema(description = "分享时间")
	private Date shareTime;
	/**
	 * 问答的code
	 */
	@Schema(description = "问答的code")
	private String codes;

	/**
	 * 来源id
	 */
	@Schema(description = "来源id")
	private String sourceId;
	/**
	 * 来源类型 0视频，1文件，2对比分析
	 */
	@Schema(description = "来源类型 0视频，1文件，2对比分析")
	private Integer sourceType;
	/**
	 * 失效时间
	 */
	@Schema(description = "失效时间")
	private Date expireTime;
	/**
	 * 链接当前状态 0正常（默认）   1已失效
	 */
	@Schema(description = "链接当前状态 0正常（默认）   1已失效")
	private Integer urlStatus;
	/**
	 * 创建时间
	 */
	@Schema(description = "创建时间")
	private Date createDate;
	/**
	 * 最后修改时间
	 */
	@Schema(description = "最后修改时间")
	private Date updateDate;
	/**
	 * 是否已删除
	 */
	@Schema(description = "是否已删除")
	private Integer isDeleted;


}
