package com.jiuyu.replay.power.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 部门表
 *
 * @author jxy
 * @date 2024-07-08
 */
@Data
@TableName("tb_dept")
public class DeptEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /**
     * 部门名称
     */
    private String name;

    /**
     * 父ID
     */
    private Long parentId;

    /**
     * 创建时间
     */
    private Date createDate;

    /**
     * 最后修改时间
     */
    private Date updateDate;

    /**
     * 是否已删除
     */
    private Integer isDeleted;
}
