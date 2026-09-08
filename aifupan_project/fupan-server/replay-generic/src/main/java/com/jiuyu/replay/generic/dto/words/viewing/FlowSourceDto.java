package com.jiuyu.replay.generic.dto.words.viewing;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Schema(description = "巨量流量结构")
public class FlowSourceDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 流量来源
     */
    @Schema(description = "流量来源")
    private String channelName;
    /**
     * 流量占比，1表示100%
     */
    @Schema(description = "流量占比，1表示100%")
    private Double ratio;
    /**
     * 子流量结构信息
     */
    @Schema(description = "子流量结构信息")
    private List<FlowSourceDto> subFlow;
}
