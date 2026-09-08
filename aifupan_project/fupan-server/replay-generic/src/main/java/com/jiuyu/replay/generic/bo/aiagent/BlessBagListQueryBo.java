package com.jiuyu.replay.generic.bo.aiagent;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * AI Agent 福袋列表查询参数（租户 + 视频维度，不做用户级裁剪）。
 *
 * @author fupan-server
 */
@Data
public class BlessBagListQueryBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 租户（团队）ID，租户隔离边界
     */
    @NotNull(message = "tenantId不能为空")
    private Long tenantId;

    /**
     * 视频 ID 列表（IN 查询）
     */
    @NotEmpty(message = "videoIds不能为空")
    private List<String> videoIds;
}
