package com.jiuyu.replay.words.bo;


import com.jiuyu.replay.common.bo.PageBo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 运营/违规收藏列表列表查询参数
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-03-12 16:42:18
 */
@Data
@Schema(description = "运营/违规收藏列表列表查询参数")
public class ClientAiFavListBo extends PageBo implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * 模糊搜索查询条件
	 */
	@Schema(description = "模糊搜索查询条件")
	private String keyword;
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
	 * 行业id
	 */
	@Schema(description = "行业id")
	private Long tradeId;
	/**
	 * 主播secUid集合
	 */
	@Schema(description = "主播secUid集合")
	private List<String> secUidArr;
	/**
	 * 文件名称
	 */
	@Schema(description = "文件名称")
	private String fileName;
	/**
	 * 来源id集合
	 */
	@Schema(description = "来源id集合")
	private List<String> resourceIds;
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
	 * 对比类型 0：视频对比 1：文件对比
	 */
	@Schema(description = "对比类型 0：视频对比 1：文件对比")
	private Integer contrastType;

}
