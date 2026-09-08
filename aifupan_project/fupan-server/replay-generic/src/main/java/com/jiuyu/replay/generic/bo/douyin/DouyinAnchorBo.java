package com.jiuyu.replay.generic.bo.douyin;

import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

/**
 * 抖音主播信息
 *
 * @author HeHui
 * @date 2026-02-06 10:39
 */
@Getter
@Setter
public class DouyinAnchorBo implements Serializable {


    @Serial
    private static final long serialVersionUID = -5077795962356138634L;

    /**
     *  昵称
     */
    private String nickname;

    /**
     *  抖音号
     */
    private String unique_id;

    /**
     *  作品数
     */
    private String aweme_count;

    /**
     *  粉丝数
     */
    private String follow_count;

    /**
     *  头像
     */
    private String avatar;

    /**
     *  抖音主页
     */
    private String aweme_url;

    /**
     *  抖音唯一ID
     */
    private String sec_uid;
}
