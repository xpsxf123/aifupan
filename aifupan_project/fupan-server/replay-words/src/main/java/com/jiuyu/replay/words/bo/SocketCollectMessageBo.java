package com.jiuyu.replay.words.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * websocket采集的信息信息
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-12-03 15:37:56
 */
@Data
@Schema(description = "websocket采集的信息信息")
public class SocketCollectMessageBo implements Serializable {
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
	 * 视频唯一标识
	 */
	@Schema(description = "视频唯一标识")
	private String videoId;
	/**
	 * 文件地址
	 */
	@Schema(description = "文件地址")
	private String fileAddress;
	/**
	 * cos文件的key
	 */
	@Schema(description = "cos文件的key")
	private String cosKey;
	/**
	 * 开始时间
	 */
	@Schema(description = "开始时间")
	private Date startDate;
	/**
	 * 结束时间
	 */
	@Schema(description = "结束时间")
	private Date endDate;
	/**
	 * 累计场观人数
	 */
	@Schema(description = "累计场观人数")
	private String totalOnlineNum;
	/**
	 * 场观人数
	 */
	@Schema(description = "累计场观人数")
	private String observationNum;
	/**
	 * 最高在线人数
	 */
	@Schema(description = "最高在线人数")
	private Integer onlineMaxNum;
	/**
	 * 租户id
	 */
	@Schema(description = "租户id")
	private Long tenantId;
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
