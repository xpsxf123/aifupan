package com.jiuyu.replay.words.bo;

import com.jiuyu.replay.common.bo.PageBo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Schema(description = "客户端获取对比列表查询参数")
public class ClientContrastListBo extends PageBo implements Serializable {

    private static final long serialVersionUID = 1L;

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
     * 行业id
     */
    @Schema(description = "行业id")
    private Long tradeId;
    /**
     * 对比类型 0：视频对比 1：文件对比
     */
    @Schema(description = "对比类型 0：视频对比 1：文件对比")
    private Integer contrastType;
    /**
     * 切片对比类型 0：原视频 1：复盘切片视频 2：短视频切片视频
     */
    @Schema(description = "切片对比类型 0：原视频 1：复盘切片视频 2：短视频切片视频")
    private Integer sliceContrastType;
    /**
     * 主播secuid
     */
    @Schema(description = "主播secuid")
    private String secUid;
    /**
     * 主播Id集合
     */
    @Schema(description = "主播Id集合")
    private List<String> secUidArr;
    /**
     * 删除状态 0：未删除 1：已从对比复盘列表删除 2：已从对比复盘列表和云空间删除
     */
    @Schema(description = "删除状态 0：未删除 1：已从对比复盘列表删除 2：已从对比复盘列表和云空间删除")
    private Integer deleteStatus;
    /**
     * 是否已分享到云空间 0：否 1：是
     */
    @Schema(description = "是否已分享到云空间 0：否 1：是")
    private Integer isShard;
    /**
     * 对比时间范围-开始 yyyy-MM-dd
     */
    @Schema(description = "录制时间范围-开始 yyyy-MM-dd")
    private String contrastStartDate;
    /**
     * 对比时间范围-结束 yyyy-MM-dd
     */
    @Schema(description = "录制时间范围-结束 yyyy-MM-dd")
    private String contrastEndDate;
    /**
     * 文件名
     */
    @Schema(description = "文件名")
    private String fileName;
    /**
     * 用户昵称或手机号
     */
    @Schema(description = "用户昵称或手机号")
    private String userKeyword;
    /**
     * 用户id集合
     */
    @Schema(description = "用户id集合")
    private List<Long> userIdList;

}
