package com.jiuyu.replay.generic.vo.words;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 上传文件精简信息 VO（generic 跨模块共享；仅含 tb_upload_file 核心字段，不嵌套 words 内部类型）。
 *
 * @author beta
 * @date 2026-05-30
 */
@Data
public class UploadFileSimpleInfoVo {

    /**
     * 文件唯一标识（uuid）
     */
    @Schema(description = "文件唯一标识")
    private String fileId;

    /**
     * 上传用户ID
     */
    @Schema(description = "上传用户ID")
    private Long userId;

    /**
     * 租户ID
     */
    @Schema(description = "租户ID")
    private Long tenantId;

    /**
     * 文件名称
     */
    @Schema(description = "文件名称")
    private String fileName;

    /**
     * 上传时间（字符串格式）
     */
    @Schema(description = "上传时间")
    private String uploadTime;

    /**
     * 文件类型 0：视频 1：音频 2：文本
     */
    @Schema(description = "文件类型 0：视频 1：音频 2：文本")
    private Integer fileType;

    /**
     * 时长，单位：秒
     */
    @Schema(description = "时长，单位：秒")
    private String fileDuration;

    /**
     * 文件字数（仅文本文件有值）
     */
    @Schema(description = "文件字数，仅文本文件有值")
    private Integer fileWordNum;

    /**
     * 所属行业 ID（{@code tb_upload_file.trade_id}）。
     *
     * <p>用于话术质检按行业取定制提示词（{@code cueWordsFeign.getCueWordByTradeAndType}）。
     * 上传时若用户未指定行业则为 null，调用方应兜底通用行业 {@code 1L}。</p>
     */
    @Schema(description = "所属行业ID；上传时未指定则为 null")
    private Long tradeId;
}
