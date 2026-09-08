package com.jiuyu.replay.words.bo;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 运营/违规收藏列表信息
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-03-12 16:42:18
 */
@Data
@Schema(description = "运营/违规收藏列表信息")
public class ClientAiFavBo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@Schema(description = "ID")
	private Long id;
	/**
	 * 租户ID
	 */
	@Schema(description = "租户ID")
	private Long tenantId;
	/**
	 * 用户ID
	 */
	@Schema(description = "用户ID")
	private Long userId;
	/**
	 * 收藏类型 0：运营助手 1：违规助手
	 */
	@Schema(description = "收藏类型 0：运营助手 1：违规助手")
	private Integer favType;
	/**
	 * 数据来源类型 0：录制视频 1：文件上传 2：对比
	 */
	@Schema(description = "数据来源类型 0：录制视频 1：文件上传 2：对比")
	private Integer dataResourceType;
	/**
	 * 数据来源id
	 */
	@Schema(description = "数据来源id")
	private String dataResourceUuid;
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
