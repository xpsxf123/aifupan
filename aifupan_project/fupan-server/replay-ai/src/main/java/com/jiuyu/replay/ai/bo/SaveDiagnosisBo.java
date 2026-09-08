package com.jiuyu.replay.ai.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/5/30 下午5:01
 */
@Schema(description = "保存诊断信息")
@Data
public class SaveDiagnosisBo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "来源id")
    private String sourceId;

    @Schema(description = "提示词ids")
    private List<Long> cueWordsIds;
}
