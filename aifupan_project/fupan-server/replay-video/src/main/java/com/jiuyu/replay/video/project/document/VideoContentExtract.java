package com.jiuyu.replay.video.project.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;


/**
 * 视频内容提取MongoDB文档
 *
 * @author RayChou
 * @date 2025-08-11
 * @description 视频内容提取文档，存储音频转文字、字幕、OCR等内容
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "replay_video_content_extract")
public class VideoContentExtract {

    @Id
    private String id;

    /**
     * 视频hash值（与MySQL中的video_hash对应）
     */
    @Field("video_hash")
    private String videoHash;

    /**
     * 音频转文字内容（AI优化后）
     */
    @Field("audio_content")
    private String audioContent;

    /**
     * 原文音频转文字内容（未优化）
     */
    @Field("original_audio_content")
    private String originalAudioContent;

    /**
     * 字幕内容（预留字段）
     */
    @Field("subtitle_content")
    private Object subtitleContent;

    /**
     * OCR识别内容（预留字段）
     */
    @Field("ocr_content")
    private Object ocrContent;

    /**
     * 创建时间
     */
    @Field("created_date")
    private LocalDateTime createdDate;

    /**
     * 更新时间
     */
    @Field("update_date")
    private LocalDateTime updateDate;

    /**
     * 逻辑删除标记
     */
    @Field("is_deleted")
    private Integer isDeleted;

}
