package com.jiuyu.replay.generic.bo.aiagent;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 主播详情查询入参。
 *
 * @author fupan-server
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class AnchorDetailQueryBo extends AiAgentBaseBo {

    private static final long serialVersionUID = 1L;

    /**
     * 主播唯一标识
     */
    @NotBlank(message = "secUid不能为空")
    private String secUid;

    /**
     * 数据源类型：1=自己录制（userId+tenantId）2=全租户（仅 tenantId，仅 userType=0 生效，子账号自动回落为 1）
     * 3=云空间（isAutoUploadCloud=1）4=自己录制+云空间（本人 OR 已上传云空间）；不传默认 1
     */
    private Integer dataSourceType = 1;
}
