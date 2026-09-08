package com.jiuyu.replay.activity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户-邀请关联
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-27 15:15:49
 */
@Data
@TableName("tb_user_invite")
public class UserInviteEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /**
     * 邀请用户id
     */
    private Long inviteUserId;

    /**
     * 邀请子账号用户id
     */
    private Long inviteSubUserId;

    /**
     * 被邀请的用户id
     */
    private Long passiveUserId;
    /**
     * 邀请链接的code
     */
    private String inviteCode;
    /**
     * 邀请类型 0：代理商链接 1：推广渠道链接 2：用户链接 3：代理商销售链接
     */
    private Integer inviteType;
    /**
     * 状态  1：有效 0：无效
     */
    private Integer inviteStatus;
    /**
     * 创建时间
     */
    private Date createDate;
    /**
     * 最后修改时间
     */
    private Date updateDate;
    /**
     * 是否已删除
     */
    private Integer isDeleted;


}
