package com.jiuyu.replay.generic.vo.words;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 本地源视频精简 VO（自动删除本地视频专用）。
 *
 * <p>仅包含自动删除逻辑所需的最小字段集，用于「客户端分页获取本地源视频列表」接口
 * {@code /replay/AnchorVideo/listLocalSourceVideo}。不复用 {@code clientVideoList}
 * 返回的 40+ 字段 {@link AnchorVideoInfoVo} 及其关联数据，避免查询与传输冗余。</p>
 */
@Data
@Schema(description = "本地源视频精简 VO")
public class LocalSourceVideoVo {

    /**
     * 视频唯一标识
     */
    @Schema(description = "视频唯一标识")
    private String videoId;

    /**
     * 视频名称
     */
    @Schema(description = "视频名称")
    private String videoName;

    /**
     * 存储路径（录制时写入的本机绝对路径，用于判存与删除源视频）
     */
    @Schema(description = "存储路径")
    private String storagePath;

    /**
     * 主播url表的唯一标识（用于查主播级自动删除配置）
     */
    @Schema(description = "主播url表的唯一标识")
    private String secUid;

    /**
     * 分析状态 0：未分析 1：分析中 2：分析完成 3分析错误
     */
    @Schema(description = "分析状态 0：未分析 1：分析中 2：分析完成 3分析错误")
    private Integer analysisStatus;
}
