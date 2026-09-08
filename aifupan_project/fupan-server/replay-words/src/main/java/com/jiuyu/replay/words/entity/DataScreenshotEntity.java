package com.jiuyu.replay.words.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 数据截图记录
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2025-03-11 17:30:33
 */
@Data
@TableName("tb_data_screenshot")
public class DataScreenshotEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * id
	 */
	@TableId(value = "id", type = IdType.INPUT)
	private Long id;
	/**
	 *租户id
	 */
	private Long tenantId;
	/**
	 *用户id
	 */
	private Long userId;
	/**
	 * 数据截图code
	 */
	private String screenshotCode;
	/**
	 * 数据类型 0视频，1文件，2对比分析
	 */
	private Integer sourceType;
	/**
	 * 来源id
	 */
	private String sourceId;
	/**
	 * 图片来源 0：oss
	 */
	private Integer sourceImagesType;
	/**
	 * 上传的图片地址
	 */
	private String sourceImagesAddress;
	/**
	 * ai识别的内容
	 */
	private String aiContent;
	/**
	 * 状态；0:图片未上传，1：图片已上传，2：ai识别中，3：ai识别完成，4：ai识别失败
	 */
	private Integer screenshotStatus;
	/**
	 * 更新时间
	 */
	private Date updateDate;
	/**
	 * 创建时间
	 */
	private Date createDate;


}
