package com.jiuyu.replay.common.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@Schema(description = "客户端日志出参")
public class ClientLogVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @Schema(description = "主键")
    private Long id;

    /**
     * 动作名称
     */
    @Schema(description = "动作名称")
    private String actionName;

    /**
     * 动作信息
     */
    @Schema(description = "动作信息")
    private String actionInfo;

    /**
     * 错误信息
     */
    @Schema(description = "错误信息")
    private String errorMsg;

    /**
     * 客户端版本
     */
    @Schema(description = "客户端版本")
    private String clientVersion;

    /**
     * 客户端用户
    */
    @Schema(description = "客户端用户")
    private String clientUser;

    /**
     * 执行时间
     */
    @Schema(description = "执行时间")
    private Date executeTime;

    @Schema(description = "日志类型,0正常日志，1错误日志")
    private Integer logType;

    /**
     * 用户id
     */
    @Schema(description = "用户id")
    private Long userId;
}
