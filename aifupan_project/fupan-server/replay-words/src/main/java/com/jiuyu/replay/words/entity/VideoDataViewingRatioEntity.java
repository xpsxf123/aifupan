package com.jiuyu.replay.words.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 数据看盘比例
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-04-17 17:45:33
 */
@Data
@TableName("tb_video_data_viewing_ratio")
public class VideoDataViewingRatioEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@TableId(value = "id", type = IdType.INPUT)
	private Long id;
	/**
	 * 名称
	 */
	private String ratioName;
	/**
	 * 唯一标识码
	 */
	private String ratioCode;
	/**
	 * 比例区间-起始
	 */
	@Schema(description = "比例区间-起始")
	private Double ratioStart;
	/**
	 * 比例区间-结束
	 */
	@Schema(description = "比例区间-结束")
	private Double ratioEnd;
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
