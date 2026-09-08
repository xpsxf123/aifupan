package com.jiuyu.replay.generic.dto.third;

import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

/**
 * 第三方榜单主播信息
 *
 * @author HeHui
 * @date 2026-02-02 14:08
 */
@Getter
@Setter
public class ThirdAnchorBo implements Serializable {
    @Serial
    private static final long serialVersionUID = -1037306487873147488L;

    /**
     * 主播ID
     */
    private Long id;

    /**
     * 带货类目
     */
    private String productCategory;

    /**
     * 粉丝数
     */
    private Integer followerCount;

    /**
     * 直播销售额
     */
    private String salesVolumeText;

    /**
     * 抖音号
     */
    private String uniqueId;

    /**
     * 销售客单价
     */
    private String averagePriceText;

    /**
     * 蝉妈妈抖音唯一作者Id
     */
    private String authorId;

    /**
     * 作者头像
     */
    private String avatar;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 直播销量
     */
    private String salesText;

    /**
     * 直播场数
     */
    private Integer liveShowCount;

    /**
     * 抖音行业Id
     */
    private String categoryId;

    /**
     * 查询一个人在一定时间范围之内的数据统计的开始时间 2025-12-01
     */
    private String beginDate;

    /**
     * 查询一个人在一定时间范围之内的数据统计的结束时间 2025-12-01
     */
    private String endDate;

    /**
     * 行业名称
     */
    private String categoryName;

    /**
     * 采集时间
     */
    private String collectionTime;

    /**
     * 榜单月份
     */
    private String rankingMonth;

    /**
     * 更新索引
     */
    private Integer updateIndex;

    /**
     * 是否更新 0否 1是
     */
    private Integer isUpdate;

    /**
     * 更新时间
     */
    private String updateTime;

    /**
     * 场均观看
     */
    private String avgDailyUserCount;
}

