package com.jiuyu.replay.common.bo.clientlog;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;


@Data
@Schema(description = "客户端日志请求参数")
public class RequestBo implements Serializable {
    private static final long serialVersionUID = 1L;



    /**
     * 动作名称
     */
    @Schema(description = "动作名称")
    private String actionName;

    /**
     * 查询的开始时间
     */
    @Schema(description = "查询的开始时间")
    private String startTime;

    /**
     * 查询的结束时间
     */
    @Schema(description = "查询的结束时间")
    private String endTime;

    /**
     * 日志类型 0正常日志，1错误日志
     */
    @Schema(description = "日志类型,0正常日志，1错误日志")
    private Integer logType;
}
