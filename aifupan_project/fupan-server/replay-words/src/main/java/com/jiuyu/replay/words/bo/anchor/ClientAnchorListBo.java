package com.jiuyu.replay.words.bo.anchor;

import com.jiuyu.replay.common.bo.PageBo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Schema(description = "客户端获取主播列表查询参数")
public class ClientAnchorListBo extends PageBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主播名称
     */
    @Schema(description = "主播名称")
    private String anchorName;
    /**
     * 主播secuid集合
     */
    @Schema(description = "主播secuid集合")
    private List<String> secUidArr;
    /**
     * 录制状态 0未开始录播，1正在录播 2手动停止录播，3录播完成
     */
    @Schema(description = "录制状态 0未开始录播，1正在录播 2手动停止录播，3录播完成")
    private Integer recordStatus;
    /**
     * 是否从录制列表删除了 0：未移除 1：已移除
     */
    @Schema(description = "是否从录制列表删除了 0：未移除 1：已移除")
    private Integer isRemoveRecord;
    /**
     * 行业id
     */
    @Schema(description = "行业id")
    private Long tradeId;
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
     * 视频切片类型 0：原视频 1：复盘切片视频 2：短视频切片视频
     */
    @Schema(description = "视频切片类型 0：原视频 1：复盘切片视频 2：短视频切片视频")
    private Integer videoSliceType;
}
