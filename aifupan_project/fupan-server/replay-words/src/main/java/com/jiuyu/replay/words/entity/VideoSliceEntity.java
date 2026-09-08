package com.jiuyu.replay.words.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 视频切片
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-11-03 11:39:28
 */
@Data
@TableName("tb_video_slice")
public class VideoSliceEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@TableId(value = "id", type = IdType.INPUT)
	private Long id;
	/**
	 * 用户id
	 */
	private Long userId;
	/**
	 * 租户id
	 */
	private Long tenantId;
	/**
	 * 来源id，视频id/文件id
	 */
	private String sourceId;
	/**
	 * 来源类型 0：视频 1：文件
	 */
	private Integer sourceType;
	/**
	 * 切片视频所属原视频id
	 */
	private String sourceParentId;
	/**
	 * 切片类型 0：复盘切片 1：短视频切片
	 */
	private Integer sliceType;
	/**
	 * 切片分类，取字典值
	 */
	private String sliceClass;
	/**
	 * 切片开始时间-毫秒
	 */
	private Long startMillisecond;
	/**
	 * 切片开始时间 mm:ss
	 */
	private String startTime;
	/**
	 * 切片结束时间-毫秒
	 */
	private Long endMillisecond;
	/**
	 * 切片结束时间 mm:ss
	 */
	private String endTime;
	/**
	 * 切片备注
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
	/**
	 * 是否自动上传云空间 0：否 1：是
	 */
	private Integer isAutoUploadCloud;
	/**
	 * 路径保存类型 0：保存在原视频文件夹 1：自定义文件夹
	 */
	private Integer savePathType;
	/**
	 * 保存路径
	 */
	private String savePath;
	/**
	 * 切片视频名称
	 */
	private String sliceVideoName;
	/**
	 * 切片时间类型 0：视频时间 1：北京时间
	 */
	private Integer sliceTimeType;

}
