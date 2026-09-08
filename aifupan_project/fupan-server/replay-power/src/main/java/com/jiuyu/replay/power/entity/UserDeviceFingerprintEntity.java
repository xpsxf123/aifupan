package com.jiuyu.replay.power.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 用户设备指纹表
 * </p>
 *
 * @author RayChou
 * @since 2025-06-10
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("tb_user_device_fingerprint")
public class UserDeviceFingerprintEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键id
     */
    @TableId("id")
    private Long id;

    /**
     * 用户id
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 设备类型：0:desktop 1:web
     */
    @TableField("device_type")
    private Integer deviceType;

    /**
     * 设备指纹
     */
    @TableField("fingerprint")
    private String fingerprint;

    /**
     * 创建时间
     */
    @TableField("create_date")
    private LocalDateTime createDate;

    /**
     * 最后修改时间
     */
    @TableField("update_date")
    private LocalDateTime updateDate;

    /**
     * 是否已删除
     */
    @TableField("is_deleted")
    @TableLogic
    private Byte isDeleted;
}
