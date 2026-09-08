package com.jiuyu.replay.common.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 系统配置信息
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-04-24 18:55:01
 */
@Data
@Schema(description = "系统配置信息")
public class SystemConfigVo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@Schema(description = "ID")
	private Long id;
	/**
	 * 超过多少秒的视频会自动生成看盘数据
	 */
	@Schema(description = "超过多少秒的视频会自动生成看盘数据")
	private Integer autoCreateDataViewingSecond;
	/**
	 * 允许匹配前后多少毫秒的数据看板，毫秒
	 */
	@Schema(description = "允许匹配前后多少毫秒的数据看板，毫秒")
	private Long dataViewingTimeDifference;
	/**
	 * 多少秒内有相同主播场次的数据看板请求就不发送，秒
	 */
	@Schema(description = "多少秒内有相同主播场次的数据看板请求就不发送，秒")
	private Integer dataViewingTimeSend;
	/**
	 * 创建时间
	 */
	@Schema(description = "创建时间")
	private Date createDate;
	/**
	 * 最后修改时间
	 */
	@Schema(description = "最后修改时间")
	private Date updateDate;
	/**
	 * 是否已删除
	 */
	@Schema(description = "是否已删除")
	private Integer isDeleted;


}
