package com.jiuyu.replay.generic.vo.aiagent;

import lombok.Data;

import java.io.Serializable;

/**
 * 视频弹幕项。
 *
 * @author fupan-server
 */
@Data
public class BarrageItemVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 记录时间戳（毫秒）
     */
    private Long recordDate;

    /**
     * 直播场次号
     */
    private String batchNumber;

    /**
     * 弹幕用户昵称
     */
    private String nickName;

    /**
     * 弹幕内容
     */
    private String content;

    /**
     * 是否新用户
     */
    private Boolean isNew;

    /**
     * 用户等级
     */
    private Long level;

    /**
     * 当前粉丝团等级
     */
    private Long fansLevelCurrent;

    /**
     * 弹幕发送次数
     */
    private Integer countSendNum;

    /**
     * 是否福袋弹幕
     */
    private Boolean isBlessBag;
}
