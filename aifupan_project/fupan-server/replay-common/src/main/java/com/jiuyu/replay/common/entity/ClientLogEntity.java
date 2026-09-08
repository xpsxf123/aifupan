package com.jiuyu.replay.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("tb_client_log")
public class ClientLogEntity implements Serializable {
    private static final long serialVersionUID = 1L;



    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /**
     * 动作名称
     */
    private String actionName;

    /**
     *  动作信息
     */
    private String actionInfo;

    /**
     * 错误信息
     */
    private String errorMsg;

    /**
     * 客户端版本
     */
    private String clientVersion;

    /**
     * 客户端用户
     */
    private String clientUser;

    /**
     * 执行时间
     */
    private Date executeTime;

    /**
     * 日志类型,0正常日志，1错误日志
     */
    private Integer logType;

    /**
     * 用户id
     */
    private Long userId;

}
