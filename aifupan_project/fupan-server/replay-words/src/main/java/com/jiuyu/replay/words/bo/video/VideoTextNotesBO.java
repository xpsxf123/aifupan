package com.jiuyu.replay.words.bo.video;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.Range;

import java.io.Serial;
import java.io.Serializable;

/**
 * 视频复盘全文笔记参数
 *
 * @author HeHui
 * @date 2025-06-07 16:53
 */
@Getter
@Setter
public class VideoTextNotesBO implements Serializable {
    @Serial
    private static final long serialVersionUID = -819634998202533392L;

    /** 视频IDid */
    @NotBlank(message = "缺少视频ID")
    private String sourceId;

    /**
     * 视频类型 1本地录制，2上传文件
     * @see com.jiuyu.replay.words.enums.VideoSourceType
     */
    @NotNull(message = "缺少视频类型")
    private Integer sourceType;

    /** 笔记类型 1: 原文笔记,2：复盘小结，3分段笔记 */
    @Range(min = 1, max = 3, message = "笔记类型错误")
    @NotNull(message = "缺少笔记类型")
    private Integer notesType;

    /** 内容 */
    @NotNull(message = "请输入笔记内容")
    private String content;
}
