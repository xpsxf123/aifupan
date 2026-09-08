package com.jiuyu.replay.order.bo;


import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import com.jiuyu.replay.common.bo.PageBo;

/**
 * ai的token使用记录列表查询参数
 *
 * @author lj
 * @email 
 * @date 2025-03-21 18:00:33
 */
@Data
@Schema(description = "ai的token使用记录列表查询参数")
public class AiTokenUseRecordListBo extends PageBo implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * 模糊搜索查询条件
	 */
	@Schema(description = "模糊搜索查询条件")
	private String keyword;

}
