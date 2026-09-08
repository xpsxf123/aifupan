package com.jiuyu.replay.generic.vo.words;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/5/26 下午7:32
 */
@Data
@Schema(description = "视频或文件内容出参")
public class VideoContentVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    private String id;

    /**
     * 来源id
     */
    @Schema(description = "来源id")
    private String sourceId;

    /**
     * 来源类型
     */
    @Schema(description = "来源类型0视频、1文件、2对比分析")
    private Integer sourceType;

    /**
     * 内容
     */
    @Schema(description = "内容")
    private String content;


    /**
     * 提示词
     */
    @Schema(description = "提示词")
    private String cueWord;

    /**
     * 生成状态 0未生成，1已生成
     */
    @Schema(description = "生成状态 0未生成，1已生成")
    private Integer generateStatus;

    /**
     * 内容List
     */
    @Schema(description = "内容List")
    private List<String> contentList;

    /**
     * 段落 从0开始
     */
    @Schema(description = "段落 从0开始")
    private Integer paragraph;

    /**
     * 内容类型 1自然原文，2优化原文
     */
    @Schema(description = "内容类型 1自然原文，2优化原文")
    private Integer type;

    /**
     * 用户id
     */
    @Schema(description = "用户id")
    private Long userId;

    /**
     * 租户id
     */
    @Schema(description = "租户id")
    private Long tenantId;

    @Schema(description = "是否删除")
    private Integer isDeleted;

    @Schema(description = "创建时间")
    private Date createDate;

    @Schema(description = "更新时间")
    private Date updateDate;
}
