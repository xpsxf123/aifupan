package com.jiuyu.replay.generic.bo.words.anchor;

import com.jiuyu.replay.generic.bo.common.PageBo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "客户端获取子账号主播列表查询参数")
public class SubAnchorListBo  extends PageBo implements Serializable {

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
     * 主播账号归属类型 0：自有账号 1：同行账号
     */
    @Schema(description = "主播账号归属类型 0：自有账号 1：同行账号")
    private Integer accountType;
}
