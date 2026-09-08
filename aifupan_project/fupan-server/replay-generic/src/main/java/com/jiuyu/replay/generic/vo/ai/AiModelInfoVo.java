package com.jiuyu.replay.generic.vo.ai;


import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;

/**
 * AI模型配置表信息项
 *
 * @author DearSil
 * @email 2437495924@qq.com
 * @date 2025-02-27 17:19:05
 */
@Data
@Schema(description = "AI模型配置表信息项")
public class AiModelInfoVo extends AiModelVo implements Serializable {
	private static final long serialVersionUID = 1L;

	@Schema(description = "ai模型, 给客户端使用")
	private Integer aiModel;

}
