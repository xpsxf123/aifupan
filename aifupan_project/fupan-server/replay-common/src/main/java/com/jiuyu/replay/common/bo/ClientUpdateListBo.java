package com.jiuyu.replay.common.bo;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 客户端更新列表查询参数
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-10-08 15:03:35
 */
@Data
@Schema(description = "客户端更新列表查询参数")
public class ClientUpdateListBo extends PageBo implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * 模糊搜索查询条件
	 */
	@Schema(description = "模糊搜索查询条件")
	private String keyword;

	@Schema(description = "版本信息")
	private String versionNum;

	@Schema(description = "更新类型 0爱复盘主程序更新, 1更新程序更新，2爱复盘补丁")
	private Integer isFront;

	@Schema(description = "父id")
	private Long parentId;

}
