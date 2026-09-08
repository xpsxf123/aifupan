package com.jiuyu.replay.words.bo;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 直播场次的websocket记录统计信息
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2025-01-04 15:15:15
 */
@Data
@Schema(description = "直播场次的websocket记录统计信息")
public class TotalSocketMessageBo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * id
	 */
	@Schema(description = "id")
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
	 * 直播开始时间
	 */
	@Schema(description = "直播开始时间")
	private Date startDate;
	/**
	 * 直播结束时间
	 */
	@Schema(description = "直播结束时间")
	private Date endDate;
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
	/**
	 * 累计场观人数
	 */
	@Schema(description = "累计场观人数")
	private String totalOnlineNum;
	/**
	 * 弹幕总数
	 */
	@Schema(description = "弹幕总数")
	private String totalBulletChatNum;


}
