package com.jiuyu.replay.generic.vo.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * AI模型配置表列表项
 *
 * @author DearSil
 * @email 2437495924@qq.com
 * @date 2025-02-27 17:19:05
 */
@Data
@Schema(description = "AI模型配置表列表项")
public class AiModelListVo extends AiModelVo implements Serializable {
	private static final long serialVersionUID = 1L;



}
