package com.jiuyu.replay.power.bo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serializable;

/**
 * @author RayChou
 * @date 2025/6/10 18:30
 */
@Data
public class UserDeviceFingerprintBo implements Serializable {

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


}
