package com.jiuyu.replay.generic.dto.activity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author RayChou
 * @date 2025/6/3 19:37
 */
@Data
public class UserInviteDto implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
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
     * 状态 0：生效中 1：已作废
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
