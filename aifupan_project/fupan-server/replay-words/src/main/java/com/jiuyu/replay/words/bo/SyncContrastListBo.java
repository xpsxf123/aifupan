package com.jiuyu.replay.words.bo;


import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

import com.jiuyu.replay.common.bo.PageBo;

/**
 * 客户端对比数据列表查询参数
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-09-26 14:51:59
 */
@Data
@Schema(description = "客户端对比数据列表查询参数")
public class SyncContrastListBo extends PageBo implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * 模糊搜索查询条件
	 */
	@Schema(description = "模糊搜索查询条件")
	private String keyword;
	/**
	 * 用户昵称
	 */
	@Schema(description = "用户昵称")
	private String userNickName;

	/**
	 * 用户IDs
	 */
	@Schema(description = "用户IDs")
	private Long userID;

	/**
	 * 查询开始时间
	 */
	@Schema(description = "查询开始时间")
	private Date startTime;

	/**
	 * 查询结束时间
	 */
	@Schema(description = "查询结束时间")
	private Date endTime;
	/**
	 * 用户ids
	 */
	@Schema(description = "用户ids")
	private List<Long> userIds;
	/**
	 * 销售人员ID
	 */
	@Schema(description = "销售人员ID")
	private Long userSalesID;
}
