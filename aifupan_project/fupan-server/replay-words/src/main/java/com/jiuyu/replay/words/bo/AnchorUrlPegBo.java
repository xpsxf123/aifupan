package com.jiuyu.replay.words.bo;

import com.jiuyu.replay.common.bo.PageBo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Schema(description = "服务端根据用户id查询绑定的主播")
public class AnchorUrlPegBo extends PageBo implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 用户的id
     */
    @Schema(description = "用户id")
    private Long userId;

    @Schema(description = "用户名称")
    private String userName;


    @Schema(description = "搜索")
    private String keyword;

    @Schema(description = "平台 ")
    private Integer platform;

    @Schema(description = "主播名称")
    private String anchorName;

    @Schema(description = "行业id")
    private Long  tradeId;

    @Schema(description = "系统行业id")
    private Long systemTradeId;

    @Schema(description = "是否已绑定系统行业 0:未绑定 1:已绑定")
    private Integer isBindSystemTrade;

    @Schema(description = "用户Id集合")
    private List<Long> userIds;


    @Schema(description = "是否查询以添加有白名单")
    private Integer isNon;

    @Schema(description = "主播关键词")
    private String anchorKeyword;
}
