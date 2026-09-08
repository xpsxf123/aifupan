package com.jiuyu.replay.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/5/28 上午9:32
 */
@Data
@Schema(description = "保存分享链接记录信息")
public class ShareSaveVo  implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "分享链接记录id")
    private Long id;

    @Schema(description = "来源id")
    private String sourceId;

    @Schema(description = "来源类型 0视频，1文件，2对比分析")
    private Integer sourceType;

    @Schema(description = "分享域名")
    private String host;

    @Schema(description = "助手类型 0运营助手 1违规助手 2弹幕助手 3数据截图助手 4数据看板助手")
    private Integer askType;
}
