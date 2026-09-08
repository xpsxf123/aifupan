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
 * 用户Token表
 * </p>
 *
 * @author RayChou
 * @since 2025-07-10
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("tb_user_token")
public class UserTokenEntity implements Serializable {

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
     * 用户信息JSON
     */
    @TableField("user_info")
    private String userInfo;

    /**
     * 过期时间
     */
    @TableField("expire_time")
    private LocalDateTime expireTime;

    /**
     * 最后访问时间
     */
    @TableField("last_access_time")
    private LocalDateTime lastAccessTime;

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
