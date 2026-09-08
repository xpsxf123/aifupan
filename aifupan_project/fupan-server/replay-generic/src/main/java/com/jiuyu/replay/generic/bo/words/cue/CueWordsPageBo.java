package com.jiuyu.replay.generic.bo.words.cue;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jiuyu.replay.generic.bo.common.PageBo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * @author ：lujie
 * @description：
 * @date ：2026/1/9 16:07
 */
@Data
@Schema(description = "获取提示词")
public class CueWordsPageBo extends PageBo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "来源id")
    @NotNull(message = "来源id不能为空")
    private String sourceId;

    @Schema(description = "来源类型 1：视频，2：文件，3：对比分析")
    @NotNull(message = "来源类型不能为空")
    private Integer sourceType;

    @Schema(description = "助手类型")
    @NotNull(message = "助手类型不能为空")
    private Integer cueType;

    /**
     * 范围 0:全文，1:段落
     */
    private Integer scope;

    /**
     * 提示词用于：0：单个分析，1：对比分析
     */
    private Integer applyTo;

    /**
     * 账号归属类型(单个分析才有) 0：自由账号 1：同行账号
     */
    private Integer accountType;


    /**
     * 定制账号归属类型(单个分析才有) 0：自由账号 1：同行账号
     */
    @JsonIgnore
    private Integer customizeAccountType;

    /**
     * 对比使用场景(对比分析才有) 1：对比上一次场、2：不同直播间对比、3：同直播间对比
     */
    private Integer syncScene;

    /**
     * 行业id
     */
    private Long tradeId;


    /**
     * 定制提示词行业查询
     */
    @JsonIgnore
    private Long customizeTradeId;

    /**
     * 租户id
     */
    @JsonIgnore
    private Long tenantId = 0L;
}
