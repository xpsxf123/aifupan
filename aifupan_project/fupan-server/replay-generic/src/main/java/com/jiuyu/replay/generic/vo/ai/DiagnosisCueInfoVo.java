package com.jiuyu.replay.generic.vo.ai;


import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;

/**
 * ai诊断提示词配置信息项
 *
 * @author lj
 * @email 
 * @date 2025-05-19 14:02:59
 */
@Data
@Schema(description = "ai诊断提示词配置信息项")
public class DiagnosisCueInfoVo extends DiagnosisCueVo implements Serializable {
	private static final long serialVersionUID = 1L;

	@Schema(description = "提示词类型")
	private Integer cueType;

}
