package com.jiuyu.replay.third.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 点赞问答文件上传cos记录表
 *
 * @author DearSil
 * @email 2437495924@qq.com
 * @date 2025-02-21 16:50:31
 */
@Data
@TableName("tb_cos_thumbs_file")
public class CosThumbsFileEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@TableId(value = "id", type = IdType.INPUT)
	private Long id;
	/**
	 * 行业id，0表示全行业
	 */
	private Long tradeId;
	/**
	 * 用户id
	 */
	private Long userId;
	/**
	 * 资源id
	 */
	private String sourceId;

	/**
	 * 平台类型 0:单个视频 1: 文件 2：对比分析
	 */
	private Integer sourceType;
	/**
	 * 文件大小
	 */
	private Long fileSize;
	/**
	 * cosKey
	 */
	private String coskey;
	/**
	 * 上下文Id
	 */
	private String contextId;
	/**
	 * 文件名称
	 */
	private String fileName;
	/**
	 * 问答次数
	 */
	private Integer askCount;
	/**
	 * 点赞状态 0：未点赞 1：已点赞 2：点踩
	 */
	private Integer thumbState;
	/**
	 * 上传时间
	 */
	private Date uploadDate;
	/**
	 * 来源类型 0：系统
	 */
	private Integer resourceType;
	/**
	 * 平台类型 0：全平台 1：抖音 2：快手 3：微视
	 */
	private Integer platformType;
	/**
	 * 回话类型 0:运营问题 1:违规
	 */
	private Integer cosType;
	/**
	 * 排序
	 */
	private Integer sort;
	/**
	 * 描述
	 */
	private String remarks;
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
