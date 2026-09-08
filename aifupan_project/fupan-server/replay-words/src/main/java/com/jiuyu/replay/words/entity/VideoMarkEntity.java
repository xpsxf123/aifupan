package com.jiuyu.replay.words.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 视频标记
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-10-10 18:09:05
 */
@Data
@TableName("tb_video_mark")
public class VideoMarkEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@TableId(value = "id", type = IdType.INPUT)
	private Long id;
	/**
	 * 视频/音频文件的唯一标识
	 */
	private String fileUuid;
	/**
	 * 标记的类型
	 */
	private Integer type;
	/**
	 * 标记类型的文字说明
	 */
	private String typeStr;
	/**
	 * 标记开始时间
	 */
	private String startDate;
	/**
	 * 标记结束时间
	 */
	private String endDate;
	/**
	 * 标记颜色
	 */
	private String color;
	/**
	 * 备注
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
