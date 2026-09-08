package com.jiuyu.replay.power.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户跟进销售人员表
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-01-21 10:16:13
 */
@Data
@TableName("tb_sales")
public class SalesEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * id
	 */
	@TableId(value = "id", type = IdType.INPUT)
	private Long id;
	/**
	 * 父ID
	 */
	private Long parentId;
	/**
	 * 跟进销售人员名
	 */
	private String salesName;
	/**
	 * 销售人员手机号
	 */
	private String phone;
	/**
	 * 二维码图片文件id
	 */
	private Long qrcodeImgId;
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
     * 是否分配线索 0不开  1开
	 */
	private Integer isChoose;

    /**
     * 是否开启用户轮询 0否 1是
     */
    private Integer userPolling;
    /**
     * 获客助手连接
     */
    private String salesIntroductionUrl;

    /**
     * 销售类型 0平台销售，1代理商销售
     */
    private Integer salesType;

    /**
     * 代理商id
     */
    private Long agentId;
    /**
     * 用户id
     */
    private Long userId;

    /**
     * 员工状态 0离职  1在职
     */
    private Integer employeeStatus;
}
