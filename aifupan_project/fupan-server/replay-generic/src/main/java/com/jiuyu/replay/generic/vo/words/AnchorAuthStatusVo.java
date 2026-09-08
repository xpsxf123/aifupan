package com.jiuyu.replay.generic.vo.words;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@Schema(description = "主播授权状态")
public class AnchorAuthStatusVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "主播SecUid")
    private String anchorUrlSecUid;

    @Schema(description = "授权巨量百应状态 0：未授权 1：已授权 2：授权过期 3：授权失败 4：授权中 5：授权抖音号不匹配")
    private Integer authJlbyStatus;

    @Schema(description = "授权巨量百应状态修改时间")
    private Date authJlbyStatusTime;

    @Schema(description = "千川授权状态 0：未授权 1：已授权 2：授权过期 3：授权失败 4：授权中 5：授权抖音号不匹配")
    private Integer authQcStatus;

    @Schema(description = "千川授权状态修改时间")
    private Date authQcStatusTime;

    @Schema(description = "授权微信视频号状态 0：未授权 1：已授权 2：微信后台取消授权, 3：用户取消授权")
    private Integer authChannelStatus;

    @Schema(description = "授权来客状态 0：未授权 1：已授权 2：授权过期 3：授权失败 4：授权中 5：授权抖音号不匹配")
    private Integer authLifeStatus;

    @Schema(description = "授权来客状态修改时间")
    private Date authLifeStatusTime;
}
