package com.jiuyu.replay.ai.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/6/4 下午3:24
 */
@Data
@Schema(description = "分享链接记录新增")
public class ShareLinkRecordSaveBo {

    /**
     * 链接分享记录  主键
     */
    @Schema(description = "链接分享记录  主键")
    private List<String> ids;

    /**
     * 来源id
     */
    @Schema(description = "来源id")
    private String sourceId;

    /**
     * 来源类型 0视频，1文件，2对比分析
     */
    @Schema(description = "来源类型 0视频，1文件，2对比分析")
    private Integer sourceType;

    /**
     * 助手类型 0运营助手 1违规助手 2弹幕助手 3数据截图助手 4数据看板助手
     */
    @Schema(description = "助手类型 0运营助手 1违规助手 2弹幕助手 3数据截图助手 4数据看板助手")
    private Integer askType;

}
