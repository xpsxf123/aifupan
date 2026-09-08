package com.jiuyu.replay.words.vo.anchor;

import lombok.Getter;
import lombok.Setter;

/**
 * 榜单主播检测
 *
 * @author HeHui
 * @date 2026-02-02 11:04
 */
@Getter
@Setter
public class RankAnchorCheckResult {


    /**
     * 是否通过
     */
    private Boolean passed;

    /**
     * 未通过原因
     */
    private String reason;

    /**
     * 抖音号
     */
    private String anchorNumber;


    /**
     * 昵称
     */
    private String anchorName;

    /**
     * 主播头像
     */
    private String anchorAvatar;

    /**
     *  粉丝数
     */
    private String followCount;

    /**
     * 主播唯一标识
     */
    private String secUid;
}
