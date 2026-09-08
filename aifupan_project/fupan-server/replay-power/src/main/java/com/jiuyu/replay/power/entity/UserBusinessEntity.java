package com.jiuyu.replay.power.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户业务表
 *
 * @author jxy
 * @date 2024-07-08
 */
@Data
@TableName("tb_user_business")
public class UserBusinessEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 用户id
     */
    @TableId(value = "user_id", type = IdType.INPUT)
    private Long userId;

    /**
     * 创建时间
     */
    private Date createDate;

    /**
     * 更新时间
     */
    private Date updateDate;

    /**
     * 根据状态
     */
    private Integer accordingStatus;

    /**
     * 根据内容
     */
    private String accordingContent;

    /**
     * 根据时间
     */
    private Date accordingDate;
    /**
     * 下次跟进时间
     */
    private Date nextFolTime;
    /**
     * 客户端版本: record-纯录制版, replay-复盘版
     */
    private String clientVersion;
}
