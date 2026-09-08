package com.jiuyu.replay.third.tablestore.entity;

import com.alicloud.openservices.tablestore.model.ColumnType;
import com.jiuyu.replay.third.tablestore.annotation.TableStoreSaveAnnotation;
import lombok.Data;

import java.io.Serializable;

/**
 * @author ：lujie
 * @description：弹幕表
 * <p>
 *     设置分区建、主键、预定义列、常规字段，要按照严格的字段顺序
 * <p/>
 * @date ：2025/1/13 下午3:08
 */
@Data
public class BarrageBo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 租户id-分区建
     */
    @TableStoreSaveAnnotation(name = "tenant_id", type = ColumnType.INTEGER, isIndex = true)
    private Long tenantId;

    /**
     * 录制用户id
     */
    @TableStoreSaveAnnotation(name = "user_id", type = ColumnType.INTEGER, isIndex = true)
    private Long userId;

    /**
     * 直播场次号
     */
    @TableStoreSaveAnnotation(name = "batch_number", type = ColumnType.INTEGER, isIndex = true)
    private Long batchNumber;

    /**
     * 消息id(当两个用户录制一个主播时，用于区分弹幕是否重复)
     */
    @TableStoreSaveAnnotation(name = "msg_id", type = ColumnType.INTEGER, isIndex = true)
    private Long msgId;

    /**
     * 视频id
     */
    @TableStoreSaveAnnotation(name = "video_id", type = ColumnType.STRING)
    private String videoId;

    /**
     * 记录时间戳
     */
    @TableStoreSaveAnnotation(name = "record_date", type = ColumnType.INTEGER)
    private Long recordDate;

    /**
     * 弹幕用户昵称(抖音限制20个字符)
     */
    @TableStoreSaveAnnotation(name = "nick_name", type = ColumnType.STRING)
    private String nickName;

    /**
     * 主播id
     */
    @TableStoreSaveAnnotation(name = "sec_uid", type = ColumnType.STRING)
    private String secUid;

    /**
     * 是否新用户
     */
    @TableStoreSaveAnnotation(name = "is_new", type = ColumnType.BOOLEAN)
    private Boolean isNew;

    /**
     * 弹幕用户等级
     */
    @TableStoreSaveAnnotation(name = "level", type = ColumnType.INTEGER)
    private Long level;

    /**
     * 场次中粉丝团等级-表格的字段是没有的
     */
    @TableStoreSaveAnnotation(name = "fans_level_current", type = ColumnType.INTEGER)
    private Long fansLevelCurrent;

    /**
     * 场次中粉丝团等级
     */
    private Long fansLevel;

    /**
     * 场次中最小粉丝团等级
     */
    @TableStoreSaveAnnotation(name = "fans_level_min", type = ColumnType.INTEGER)
    private Long fansLevelMin;

    /**
     * 场次中最大粉丝团等级
     */
    @TableStoreSaveAnnotation(name = "fans_level_max", type = ColumnType.INTEGER)
    private Long fansLevelMax;

    /**
     * 弹幕内容(抖音限制一条弹幕最多能发送50个字)
     */
    @TableStoreSaveAnnotation(name = "content", type = ColumnType.STRING)
    private String content;

    /**
     * 排序字段
     */
    @TableStoreSaveAnnotation(name = "sort", type = ColumnType.INTEGER)
    private Long sort;

    /**
     * 是否重要弹幕字段
     */
    @TableStoreSaveAnnotation(name = "important", type = ColumnType.INTEGER)
    private Long important;

    /**
     * 是否是福袋弹幕字段
     */
    @TableStoreSaveAnnotation(name = "is_bless_bag", type = ColumnType.BOOLEAN)
    private Boolean isBlessBag;
}
