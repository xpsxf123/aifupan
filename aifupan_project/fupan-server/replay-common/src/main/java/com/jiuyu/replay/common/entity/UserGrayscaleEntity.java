package com.jiuyu.replay.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/12/5 16:33
 */
@Data
@TableName("tb_user_grayscale")
public class UserGrayscaleEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /**
     * 版本id
     */
    private Long versionId;
    /**
     * 用户id
     */
    private Long userId;
    /**
     * 用户i手机号
     */
    private String phone;
    /**
     * 用户i昵称
     */
    private String nickName;
    /**
     * 更新时间
     */
    private Date updateDate;
    /**
     * 创建时间
     */
    private Date createDate;
    /**
     * 是否已删除
     */
    private Long isDeleted;
}
