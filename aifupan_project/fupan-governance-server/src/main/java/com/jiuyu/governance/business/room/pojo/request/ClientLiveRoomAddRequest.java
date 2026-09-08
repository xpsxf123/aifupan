package com.jiuyu.governance.business.room.pojo.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 客户端新增主播请求
 *
 * @author HeHui
 * @date 2026-04-16 18:38
 */
@Getter
@Setter
public class ClientLiveRoomAddRequest {

    /**
     * 直播平台类型 (0: 抖音, 1: 快手, 2: 视频号)
     */
    @NotNull(message = "请选择直播平台")
    private Integer platform;


    /**
     * 主播平台账号 (如抖音号)
     */
    @NotBlank(message = "请输入主播账号")
    private String anchorNumber;


    /**
     * 主播唯一标识
     */
    @NotBlank(message = "缺少主播唯一标识")
    private String secUid;

    /**
     * 主页url
     */
    private String homeUrl;
    /**
     * 直播间url
     */
    private String liveUrl;

    /**
     * 主播名称
     */
    @NotBlank(message = "请输入主播名称")
    private String anchorName;
    /**
     * 主播头像
     */
    private String anchorAvatar;


    /**
     * 绑定的行业分类ID
     */
    private Long tradeId;
}
