package com.jiuyu.replay.generic.bo.words;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "分享对比到云空间数据对象")
public class ShareContrastCloudBo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 对比id
     */
    @Schema(description = "对比id")
    private String contrastId;
    /**
     * 云空间备注
     */
    @Schema(description ="云空间备注")
    private String cloudRemarks;
}
