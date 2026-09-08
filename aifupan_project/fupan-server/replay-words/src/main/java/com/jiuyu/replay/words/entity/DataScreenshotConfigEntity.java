package com.jiuyu.replay.words.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 数据截图配置
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2025-03-11 17:30:33
 */
@Data
@TableName("tb_data_screenshot_config")
public class DataScreenshotConfigEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * id
	 */
	@TableId(value = "id", type = IdType.INPUT)
	private Long id;
	/**
	 * code
	 */
	private String screenshotCode;
	/**
	 * 标题
	 */
	private String title;
	/**
	 * 备注
	 */
	private String remarks;
	/**
	 * 排序
	 */
	private Integer sort;
	/**
	 * 图片来源 0：oss
	 */
	private Integer sourceType;
	/**
	 * 示例图片
	 */
	private String example;
	/**
	 * 更新时间
	 */
	private Date updateDate;
	/**
	 * 创建时间
	 */
	private Date createDate;


}
