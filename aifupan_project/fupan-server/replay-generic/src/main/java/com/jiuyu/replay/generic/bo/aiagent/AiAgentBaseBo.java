package com.jiuyu.replay.generic.bo.aiagent;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * AI Agent 开放接口身份三件套基类。
 *
 * <p>本批接口走 API-Key 鉴权，不依赖任何 JWT / 登录会话，调用方必须在每个请求体里显式带身份。
 * 服务端据此做租户隔离 + 数据范围裁剪（userType 0/1 租户级，2 仅本人）。</p>
 *
 * @author fupan-server
 */
@Data
public class AiAgentBaseBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 调用代表的用户 ID
     */
    @NotNull(message = "userId不能为空")
    private Long userId;

    /**
     * 用户所属租户（团队）ID，租户隔离边界
     */
    @NotNull(message = "tenantId不能为空")
    private Long tenantId;

    /**
     * 用户类型：0=爱复盘普通用户(主账号) / 1=后台管理员 / 2=爱复盘子账号
     */
    @NotNull(message = "userType不能为空")
    private Integer userType;
}
