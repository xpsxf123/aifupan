package com.jiuyu.replay.third.tablestore.entity;

import com.alicloud.openservices.tablestore.model.ColumnType;
import com.jiuyu.replay.third.tablestore.annotation.TableStoreSaveAnnotation;
import lombok.Data;

import java.io.Serializable;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/1/14 上午11:27
 */
@Data
public class AnchorUserBo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 租户id-分区建
     */
    @TableStoreSaveAnnotation(name = "tenant_id", type = ColumnType.INTEGER, isIndex = true)
    private Long tenantId;
    /**
     * 主播id
     */
    @TableStoreSaveAnnotation(name = "sec_uid", type = ColumnType.STRING, isIndex = true)
    private String secUid;
    /**
     * 弹幕用户昵称(抖音限制20个字符)
     */
    @TableStoreSaveAnnotation(name = "nick_name", type = ColumnType.STRING, isIndex = true)
    private String nickName;
    /**
     * 直播场次号
     */
    @TableStoreSaveAnnotation(name = "batch_number", type = ColumnType.INTEGER)
    private Long batchNumber;
    /**
     * 记录时间戳
     */
    @TableStoreSaveAnnotation(name = "record_date", type = ColumnType.INTEGER)
    private Long recordDate;

    /**
     * 真实用户昵称
     */
    @TableStoreSaveAnnotation(name = "real_nick_name", type = ColumnType.STRING)
    private String realNickName;

    /**
     * 用户等级
     */
    @TableStoreSaveAnnotation(name = "user_level", type = ColumnType.INTEGER)
    private Long userLevel;
}
