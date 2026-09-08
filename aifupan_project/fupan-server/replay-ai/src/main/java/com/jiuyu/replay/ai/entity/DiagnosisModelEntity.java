package com.jiuyu.replay.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * ai诊断中的模型设置-主播和视频
 *
 * @author lj
 * @email 
 * @date 2025-05-19 14:02:59
 */
@Data
@TableName("tb_diagnosis_model")
public class DiagnosisModelEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	@TableId(value = "id", type = IdType.INPUT)
	private Long id;
	/**
	 * 来源id
	 */
	private String sourceId;
	/**
	 * 来源类型 0主播 1视频
	 */
	private Integer sourceType;
	/**
	 * 模型id
	 */
	private Long modelId;
    /**
     * 诊断报告类型 0：内容诊断，1：数据诊断
     */
    private Integer diagnosisType;
	/**
	 * 用户id
	 */
	private Long userId;
	/**
	 * 租户id
	 */
	private Long tenantId;
	/**
	 * 更新人
	 */
	private Long updateUserId;
	/**
	 * 更新时间
	 */
	private Date updateDate;
	/**
	 * 创建人
	 */
	private Long createUserId;
	/**
	 * 创建时间
	 */
	private Date createDate;
	/**
	 * 是否已删除
	 */
	private Integer isDeleted;


}
