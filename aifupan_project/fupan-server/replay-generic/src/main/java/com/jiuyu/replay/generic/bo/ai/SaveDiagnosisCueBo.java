package com.jiuyu.replay.generic.bo.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/5/19 下午8:06
 */
@Data
@Schema(description = "ai诊断中的模型设置-保存")
public class SaveDiagnosisCueBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 来源id
     */
    @Schema(description = "来源id")
    private String sourceId;
    /**
     * 来源类型 0主播 1视频
     */
    @Schema(description = "来源类型 0主播 1视频")
    private Integer sourceType;

    /**
     * 模型id
     */
    @Schema(description = "模型id")
    private Long modelId;

    @Schema(description = "诊断类型 0内容诊断 1结构诊断")
    private Integer diagnosisType;

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
    /**
     * 更新人
     */
    @Schema(description = "更新人")
    private Long updateUserId;
    /**
     * 更新时间
     */
    @Schema(description = "更新时间")
    private Date updateDate;
    /**
     * 创建人
     */
    @Schema(description = "创建人")
    private Long createUserId;
    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private Date createDate;

    @Schema(description = "选中的提示词id列表")
    private List<Long> selectCueWordsIdsList;


    @Schema(description = "是否有数据截图 0没有，1有")
    private Integer selectDataScreenshot;

    @Schema(description = "是否有数据看版 0没有，1有")
    private Integer selectBoard;

}
