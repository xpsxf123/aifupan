package com.jiuyu.replay.words.bo;

import com.jiuyu.replay.common.bo.PageBo;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 视频笔记查询
 *
 * @author HeHui
 * @date 2025-09-26 15:58
 */
@Getter
@Setter
public class VideoNotesQueryBo extends PageBo implements Serializable {


    @Serial
    private static final long serialVersionUID = -4722357746160584598L;


    /**
     * 租户id
     */
    private List<Long> tenantIds;


    /**
     * 用户id
     */
    private List<Long> userIds;


    /**
     * 开始时间 - 创建时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间 - 创建时间
     */
    private LocalDateTime endTime;


    /**
     * 类型 0视频，1文件，2对比分析
     */
    private List<Integer> sourceTypes;

    /**
     * 笔记类型
     * 1: 原文笔记,2：复盘小结，3分段笔记
     */
    private Integer notesType;
}
