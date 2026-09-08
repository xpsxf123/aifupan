package com.jiuyu.replay.generic.bo.ai;


import com.jiuyu.replay.generic.bo.common.PageBo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * ai诊断中的模型设置-主播和视频列表查询参数
 *
 * @author lj
 * @email 
 * @date 2025-05-19 14:02:59
 */
@Data
@Schema(description = "ai诊断中的模型设置-主播和视频列表查询参数")
public class DiagnosisModelListBo extends PageBo implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * 模糊搜索查询条件
	 */
	@Schema(description = "模糊搜索查询条件")
	private String keyword;

}
