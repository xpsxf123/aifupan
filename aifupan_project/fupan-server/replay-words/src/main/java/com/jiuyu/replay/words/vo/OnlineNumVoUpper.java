package com.jiuyu.replay.words.vo;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 直播实时在线人数信息
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-11-14 10:05:41
 */
@Data
@Schema(description = "直播实时在线人数信息")
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class OnlineNumVoUpper implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@Schema(description = "ID")
	private Long id;
	/**
	 * 用户id
	 */
	@Schema(description = "用户id")
	private Long userId;
	/**
	 * 主播secUid
	 */
	@Schema(description = "主播secUid")
	private String secUid;
	/**
	 * 直播场次号
	 */
	@Schema(description = "直播场次号")
	private String batchNumber;
	/**
	 * 记录时间
	 */
	@Schema(description = "记录时间")
	private String recordDate;
	/**
	 * 在线人数
	 */
	@Schema(description = "在线人数")
	private String peopleNum;
	/**
	 * 在线人数数据 2024-11-12 17:30:47@2503_2024-11-12 17:31:09@9750
	 */
	@Schema(description = "在线人数数据 2024-11-12 17:30:47@2503_2024-11-12 17:31:09@9750")
	private String peopleNumData;
	/**
	 * 视频唯一标识
	 */
	@Schema(description = "视频唯一标识")
	private String videoId;
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
