package com.jiuyu.governance.business.performance.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 商品表
 *
 * @author lj
 * @date 2026-03-24
 */
@Getter
@Setter
@TableName("product")
public class Product {

    /**
     * 主键，雪花ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 租户ID
     */
    @TableField("tenant_id")
    private Long tenantId;

    /**
     * 商品ID
     */
    @TableField("product_id")
    private String productId;

    /**
     * 商品名称
     */
    @TableField("name")
    private String name;

    /**
     * 商品图片URL
     */
    @TableField("image_uri")
    private String imageUri;

    /**
     * 创建时间
     */
    @TableField(value = "create_date", fill = FieldFill.INSERT)
    private LocalDateTime createDate;

    /**
     * 更新时间
     */
    @TableField(value = "update_date", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateDate;

    /**
     * 创建人
     */
    @TableField("create_by")
    private Long createBy;

    /**
     * 更新人
     */
    @TableField("update_by")
    private Long updateBy;

    /**
     * 逻辑删除：0-正常，-1-已删除
     */
    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;
}
