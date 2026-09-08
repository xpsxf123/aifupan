package com.jiuyu.replay.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户和自定义版本之间关联表
 */
@Data
@TableName("tb_package_user")
public class PackageUserEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;
    /**
     * 版本id
     */
    private Long packageId;
    /**
     * 用户id
     */
    private Long userId;
    /**
     * 租户id
     */
    private Long tenantId;
    /**
     * 修改时间
     */
    private Date updateDate;
    /**
     * 创建时间
     */
    private Date createDate;
    /**
     * 是否已删除
     */
    private Integer isDeleted;

}
