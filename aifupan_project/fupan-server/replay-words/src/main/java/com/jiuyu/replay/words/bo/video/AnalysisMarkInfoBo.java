package com.jiuyu.replay.words.bo.video;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.io.Serializable;
import java.util.Date;

/**
 * 详情查询BO
 *
 * @author liaoxin
 * @date 2025-06-07
 */
@Data
@Schema(description = "标注")
public class AnalysisMarkInfoBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 验证组：新增操作
     */
    public interface Add {
    }

    /**
     * 验证组：修改操作
     */
    public interface Update {
    }

    /**
     * 验证组：delete操作
     */
    public interface Delete {
    }

    /**
     * 主键
     */
    @Schema(description = "主键")
    @NotNull(message = "缺少主键ID", groups = {AnalysisMarkInfoBo.Update.class})
    private Long id;

    /**
     * 视频ID
     */
    @NotBlank(message = "缺少视频ID", groups = {AnalysisMarkInfoBo.Add.class})
    private String sourceId;

    /**
     * 视频类型 1本地录制，2上传文件
     *
     * @see com.jiuyu.replay.words.enums.VideoSourceType
     */
    @NotNull(message = "缺少视频类型", groups = {AnalysisMarkInfoBo.Add.class})
    private Integer sourceType;

    /**
     * 开始段落编号
     */
    @Schema(description = "开始段落编号")
    @NotNull(message = "缺少开始段落编号", groups = {AnalysisMarkInfoBo.Add.class})
    private Integer paraphStartNo;

    /**
     * 结束段落编号
     */
    @Schema(description = "结束段落编号")
    @NotNull(message = "缺少结束段落编号", groups = {AnalysisMarkInfoBo.Add.class})
    private Integer paraphEndNo;

    /**
     * 标注内容
     */
    @Schema(description = "标注内容")
    @NotBlank(message = "请输入标注内容", groups = {AnalysisMarkInfoBo.Add.class, AnalysisMarkInfoBo.Update.class})
    @Length(message = "标注内容过长,请少于200字", max = 200, groups = {AnalysisMarkInfoBo.Add.class, AnalysisMarkInfoBo.Update.class})
    private String markContent;

    /**
     * 标注编号
     */
    @Schema(description = "标注编号")
    private Integer markNo;

    /**
     * 开始索引
     */
    @Schema(description = "开始索引")
    @NotNull(message = "缺少开始索引", groups = {AnalysisMarkInfoBo.Add.class})
    @Min(value = 0, message = "开始索引不能小于0", groups = {AnalysisMarkInfoBo.Add.class})
    private Integer markStartIndex;

    /**
     * 结束索引
     */
    @Schema(description = "结束索引")
    @NotNull(message = "缺少结束索引", groups = {AnalysisMarkInfoBo.Add.class})
    @Min(value = 0, message = "结束索引不能小于0", groups = {AnalysisMarkInfoBo.Add.class})
    private Integer markEndIndex;

    /**
     * 创建用户id
     */
    @Schema(description = "创建用户id")
    private Long createUserId;

    /**
     * 修改用户id
     */
    @Schema(description = "修改用户id")
    private Long updateUserId;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private Date createTime;

    /**
     * 修改时间
     */
    @Schema(description = "修改时间")
    private Date updateTime;

    /**
     *
     */
    @Schema(description = "是否删除")
    private Integer isDeleted;

}