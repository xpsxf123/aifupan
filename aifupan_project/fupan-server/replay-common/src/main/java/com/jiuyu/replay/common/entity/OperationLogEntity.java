package com.jiuyu.replay.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;

import io.swagger.v3.core.util.Json;
import lombok.Data;

/**
 * 操作日志表
 *
 * @author lyw
 * @email 1798883178@qq.com
 * @date 2025-06-26 13:59:24
 */
@Data
@TableName("tb_operation_log")
public class OperationLogEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 主键ID
	 */
	@TableId(value = "id", type = IdType.INPUT)
	private Long id;
	/**
	 * 业务类型(USER_DETAILS/USER/ORDER/OTHER)
	 */
	private String businessType;
	/**
	 * 业务ID 例如操作用户详情表对应详情表id
	 */
	private Long businessId;
	/**
	 * 业务用户ID
	 */
	private Long businessUserId;
	/**
	 * 操作类型(UPDATE/DELETE)
	 */
	private String operationType;
	/**
	 * 操作前数据(JSON格式)
	 */
	private String beforeData;
	/**
	 * 操作后数据(JSON格式)
	 */
	private String afterData;
	/**
	 * 操作人ID
	 */
	private Long operatorId;
	/**
	 * 操作人姓名
	 */
	private String operatorName;
	/**
	 * 操作时间
	 */
	private Date operationTime;
	/**
	 * 操作IP
	 */
	private String operationIp;
	/**
	 * 备注
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


}
