package com.jiuyu.replay.generic.vo.aiagent;

import lombok.Data;

import java.io.Serializable;

/**
 * 达人列表项（租户维度）。
 *
 * @author fupan-server
 */
@Data
public class InfluencerItemVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 达人 id（tb_video_influencer_info.id），后续达人短视频接口用它定位
     */
    private Long id;

    /**
     * 平台类型：1-抖音 2-快手 3-视频号
     */
    private Byte platformType;

    /**
     * 平台账号（抖音号/快手号/视频号）
     */
    private String platformAccount;

    /**
     * 平台用户 id
     */
    private String platformUserId;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 头像 URL
     */
    private String avatar;

    /**
     * 粉丝数
     */
    private Long followersCount;

    /**
     * 关注数
     */
    private Long followingCount;

    /**
     * 作品总数（平台侧全量，来自达人库）
     */
    private Integer videoCount;

    /**
     * 获赞数
     */
    private Long likeCount;

    /**
     * 认证状态：0-未认证 1-个人 2-企业/机构 3-政府/官方 4-媒体/特殊
     */
    private Byte verificationStatus;

    /**
     * 认证信息（达人库 verification_info）
     */
    private String verificationInfo;

    /**
     * 个人简介（达人库 influencer_description）
     */
    private String influencerDescription;

    /**
     * 行业 id（来自当前租户对该达人的订阅 tb_video_user_influencer_subscription.industry_id）
     */
    private Long industryId;

    /**
     * 行业名称（industryId 非空时按行业库回填；industryId 为空或行业不存在时为 null）
     */
    private String industryName;


    /**
     * 最近采集时间（= 达人 last_sync_time，yyyy-MM-dd HH:mm:ss）
     */
    private String lastCollectTime;

    /**
     * 账号归属类型 0：自由账号 1：同行账号（来自基础设置）
     */
    private Integer accountType;
}
