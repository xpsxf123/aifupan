package com.jiuyu.replay.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * ai诊断提示词配置
 *
 * @author lj
 * @email 
 * @date 2025-05-19 14:02:59
 */
@Data
@TableName("tb_diagnosis_cue")
public class DiagnosisCueEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * id
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
	 * 行业id
	 */
	private Long tradeId;
	/**
	 * 提示词id
	 */
	private Long cueWordsId;
	/**
	 * 状态 0未处理 1处理中 2处理完成 3处理失败
	 */
	private Integer qaStatus;
	/**
     * 最新分析时间
     */
    private Date qaHandleTime;
    /**
	 * 错误内容
	 */
	private String errorContent;
	/**
	 * 已选中，0否，1是
	 */
	private Integer isSelected;
    /**
     * 诊断报告类型 0：内容诊断，1：数据诊断
     */
    private Integer diagnosisType;
    /**
     * 是否选择数据截图 0没有，1有
     */
    private Integer selectDataScreenshot;

    /**
     * 是否选择数据看版 0没有，1有
     */
    private Integer selectBoard;
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
     * 是否已读 0未读 1已读
     */
    private Integer isRead;
    /**
	 * 是否已删除
	 */
	private Integer isDeleted;


}
