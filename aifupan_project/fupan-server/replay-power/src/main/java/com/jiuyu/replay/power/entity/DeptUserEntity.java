package com.jiuyu.replay.power.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 部门对应下的用户
 *
 * @author jxy
 * @date 2024-07-08
 */
@Data
@TableName("tb_dept_user")
public class DeptUserEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /**
     * 部门id
     */
    private Long deptId;

    /**
     * 用户Id
     */
    private Long userId;

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
