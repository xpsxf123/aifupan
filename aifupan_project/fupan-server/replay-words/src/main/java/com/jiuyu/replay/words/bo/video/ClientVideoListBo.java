package com.jiuyu.replay.words.bo.video;

import com.jiuyu.replay.common.bo.PageBo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Schema(description = "客户端获取视频列表查询参数")
public class ClientVideoListBo extends PageBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主播Id
     */
    @Schema(description = "主播Id")
    private String secUid;
    /**
     * 主播Id集合
     */
    @Schema(description = "主播Id集合")
    private List<String> secUidArr;
    /**
     * 行业id
     */
    @Schema(description = "行业id")
    private Long tradeId;
    /**
     * 视频上传云空间状态 0：未上传 1：已上传
     */
    @Schema(description = "视频上传云空间状态 0：未上传 1：已上传")
    private Integer uploadStatus;
    /**
     * 视频删除状态 0：未删除 1：已从复盘列表删除 2：已从复盘列表和云空间删除
     */
    @Schema(description = "视频删除状态 0：未删除 1：已从复盘列表删除 2：已从复盘列表和云空间删除")
    private Integer deleteStatus;
    /**
     * 分析状态 0：未分析 1：分析中 2：分析完成 3分析错误
     */
    @Schema(description = "分析状态 0：未分析 1：分析中 2：分析完成 3分析错误")
    private Integer analysisStatus;
    /**
     * 录制时间范围-开始 yyyy-MM-dd
     */
    @Schema(description = "录制时间范围-开始 yyyy-MM-dd")
    private String recordStartDate;
    /**
     * 录制时间范围-结束 yyyy-MM-dd
     */
    @Schema(description = "录制时间范围-结束 yyyy-MM-dd")
    private String recordEndDate;
    /**
     * 分析时间范围-开始 yyyy-MM-dd
     */
    @Schema(description = "分析时间范围-开始 yyyy-MM-dd")
    private String analysisStartDate;
    /**
     * 分析时间范围-结束 yyyy-MM-dd
     */
    @Schema(description = "分析时间范围-结束 yyyy-MM-dd")
    private String analysisEndDate;
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
     * 用户昵称或手机号
     */
    @Schema(description = "用户昵称或手机号")
    private String userKeyword;
    /**
     * 主播名称
     */
    @Schema(description = "主播名称")
    private String anchorName;
    /**
     * 用户id集合
     */
    @Schema(description = "用户id集合")
    private List<Long> userIdList;

    /**
     * 是否有诊断报告 0：没有 1：有
     */
    @Schema(description = "是否有内容诊断报告 0：没有 1：有")
    private Integer hasDiagnosisReport;

    /**
     * 是否有诊断报告 0：没有 1：有
     */
    @Schema(description = "是否有诊断报告 0：没有 1：有")
    private Integer hasDiagnosis;

    /**
     * 是否有数据诊断报告
     */
    @Schema(description = "是否有数据诊断报告")
    private Integer hasDataDiagnosisReport;

    /**
     * 是否云空间列表查询
     */
    @Schema(description = "是否云空间列表查询")
    private Boolean isCloud;
//    /**
//     * 主播账号归属类型 0：自有账号 1：同行账号
//     */
//    @Schema(description = "主播账号归属类型 0：自有账号 1：同行账号")
//    private Integer anchorAccountType;
    /**
     * 视频videoId集合
     */
    @Schema(description = "视频videoId集合")
    private List<String> videoIdList;

    /**
     * 账号类型筛选
     */
    @Schema(description = "账号类型：0：自有账号 1：同行业账号")
    private Integer accountType;
    /**
     * 视频切片类型 0：原视频 1：复盘切片视频 2：短视频切片视频
     */
    @Schema(description = "视频切片类型 0：原视频 1：复盘切片视频 2：短视频切片视频")
    private Integer videoSliceType;
    /**
     * 切片分类，取字典值
     */
    @Schema(description = "切片分类，取字典值")
    private Integer sliceClass;
}
