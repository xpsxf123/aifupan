package com.jiuyu.replay.agent.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class ChannelTreeVo {
    /**
     * ID
     */
    @Schema(description = "ID")
    private Long id;
    /**
     * 父ID
     */
    @Schema(description = "父ID")
    private Long parentId;
    /**
     * 渠道名称
     */
    @Schema(description = "渠道名称")
    private String channelName;
    /**
     * 子渠道
     */
    @Schema(description = "渠道名称")
    private List<ChannelTreeVo> children;
    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private Date createDate;
    /**
     * 最后修改时间
     */
    @Schema(description = "最后修改时间")
    private Date updateDate;
    /**
     * 是否已删除
     */
    @Schema(description = "是否已删除")
    private Integer isDeleted;
}
