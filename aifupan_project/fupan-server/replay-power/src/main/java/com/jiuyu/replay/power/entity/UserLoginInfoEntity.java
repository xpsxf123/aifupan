package com.jiuyu.replay.power.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 用户登录信息表
 * </p>
 *
 * @author RayChou
 * @since 2025-07-10
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("tb_user_login_info")
public class UserLoginInfoEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId("id")
    private Long id;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 登录token
     */
    @TableField("token")
    private String token;

    /**
     * 登录来源
     */
    @TableField("source_info")
    private String sourceInfo;

    /**
     * 设备指纹
     */
    @TableField("fingerprint")
    private String fingerprint;

    /**
     * 最后请求时间
     */
    @TableField("last_request_time")
    private LocalDateTime lastRequestTime;

    /**
     * 过期时间
     */
    @TableField("expire_time")
    private LocalDateTime expireTime;

    /**
     * 创建时间
     */
    @TableField("create_date")
    private LocalDateTime createDate;

    /**
     * 更新时间
     */
    @TableField("update_date")
    private LocalDateTime updateDate;

    /**
     * 是否已删除
     */
    @TableField("is_deleted")
    @TableLogic
    private Byte isDeleted;
}
