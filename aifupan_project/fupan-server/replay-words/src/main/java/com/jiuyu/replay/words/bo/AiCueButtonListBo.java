package com.jiuyu.replay.words.bo;


import com.jiuyu.replay.common.bo.PageBo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 固定提示按钮
列表查询参数
 *
 * @author DearSil
 * @email 2437495924@qq.com
 * @date 2025-02-25 11:11:43
 */
@Data
@Schema(description = "固定提示按钮列表查询参数")
public class AiCueButtonListBo extends PageBo implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * 模糊搜索查询条件
	 */
	@Schema(description = "模糊搜索查询条件")
	private String keyword;

	/**
	 * 查询行业id
	 */
	@Schema(description = "查询行业id")
	private Long tradeId;

}
