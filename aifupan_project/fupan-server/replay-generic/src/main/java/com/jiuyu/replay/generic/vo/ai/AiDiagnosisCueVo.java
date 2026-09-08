package com.jiuyu.replay.generic.vo.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/5/20 上午10:32
 */
@Data
@Schema(description = "ai诊断提示词配置信息项")
public class AiDiagnosisCueVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "模型id")
    private Long modelId;

    @Schema(description = "模型名称")
    private String modelName;

    @Schema(description = "提示词集合")
    private List<ListDiagnosisCueVo> list;
}
