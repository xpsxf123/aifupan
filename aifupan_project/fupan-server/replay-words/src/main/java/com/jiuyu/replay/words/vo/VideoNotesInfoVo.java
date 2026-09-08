package com.jiuyu.replay.words.vo;

import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 视频笔记信息
 *
 * @author HeHui
 * @date 2025-09-26 15:53
 */
@Getter
@Setter
public class VideoNotesInfoVo implements Serializable {

    @Serial
    private static final long serialVersionUID = -4822190381429430789L;


    /** 主键id */
    private Long id;

    /** 所属租户id */
    private Long tenantId;

    /** 所属用户id */
    private Long userId;

    /**
     * 视频id
     */
    private String sourceId;

    /**
     * 类型 0视频，1文件，2对比分析
     */
    private Integer sourceType;

    /**
     * 笔记类型
     * 1: 原文笔记,2：复盘小结，3分段笔记
     */
    private Integer notesType;


    /** 最新版本 */
    private Integer lastVersion;


    /** 创建时间 */
    private LocalDateTime createTime;

    /** 最后修改时间 */
    private LocalDateTime updateTime;


    /** 编辑人 */
    private List<NotesContentVo.Editor> editors;
}
