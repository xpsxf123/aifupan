package com.jiuyu.replay.words.bo;


import com.jiuyu.replay.common.bo.PageBo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 主播白名单表列表查询参数
 *
 * @author hts
 * @email 1776764427@qq.com
 * @date 2024-09-15 09:50:55
 */
@Data
@Schema(description = "主播白名单表列表查询参数")
public class AnchorUrlWhiteListBo extends PageBo implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * 模糊搜索查询条件
	 */
	@Schema(description = "模糊搜索查询条件")
	private String keyword;

	/***
	 * 主播的唯一标识
	 */
	@Schema(description = "主播sec_uid")
	private String secUid;


}
