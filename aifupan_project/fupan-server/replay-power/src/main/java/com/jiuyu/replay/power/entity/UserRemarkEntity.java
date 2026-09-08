package com.jiuyu.replay.power.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jiuyu.replay.common.baseEntity.BaseEntity;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户备注表
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-02-12 15:59:28
 */
@Data
@TableName("tb_user_remark")
public class UserRemarkEntity extends BaseEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@TableId(value = "id", type = IdType.INPUT)
	private Long id;
	/**
	 * 用户ID
	 */
	private Long userId;
	/**
	 * 用户备注
	 */
	private String remark;
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
	/**
	 * 跟进类型
	 */
	private Integer followType;
    /**
     * 跟进状态
     */
    private Integer followStatus;
	/**
	 * 下次跟进时间
	 */
	private Date nextFolTime;
    /**
     * 跟进时间
     */
    private Date followUpTime;



}
