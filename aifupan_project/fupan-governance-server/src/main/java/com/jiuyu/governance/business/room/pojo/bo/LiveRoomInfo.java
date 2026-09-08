package com.jiuyu.governance.business.room.pojo.bo;

import com.jiuyu.governance.business.room.pojo.constants.LivePlatformType;
import com.jiuyu.governance.plugins.webmvc.serializer.EnumDesc;
import lombok.Getter;
import lombok.Setter;

/**
 * 直播间基础信息
 *
 * @author HeHui
 * @date 2026-03-28 15:58
 */
@Getter
@Setter
public class LiveRoomInfo {


    /**
     * 直播间主键ID
     */
    private Long id;

    /**
     * 直播平台类型 (0: 抖音, 1: 快手, 2: 视频号)
     */
    @EnumDesc(LivePlatformType.class)
    private Integer platform;

    /**
     * 主播平台账号 (如抖音号)
     */
    private String anchorNumber;

    /**
     * 主播在第三方平台的唯一标识
     */
    private String secUid;

    /**
     * 主播个人主页的URL链接
     */
    private String homeUrl;

    /**
     * 直播间外显的URL链接
     */
    private String liveUrl;

    /**
     * 主播名称/昵称
     */
    private String anchorName;

    /**
     * 主播头像的URL链接
     */
    private String anchorAvatar;
}
