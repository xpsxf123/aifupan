package com.jiuyu.replay.words.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * websocket采集的信息
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-12-03 15:37:56
 */
@Data
@TableName("tb_socket_collect_message")
public class SocketCollectMessageEntity implements Serializable {
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
	 * 主播secUid
	 */
	private String secUid;
	/**
	 * 直播场次号
	 */
	private String batchNumber;
	/**
	 * 视频唯一标识
	 */
	private String videoId;
	/**
	 * 文件地址
	 */
	private String fileAddress;
	/**
	 * cos文件的key
	 */
	private String cosKey;
	/**
	 * 开始时间
	 */
	private Date startDate;
	/**
	 * 结束时间
	 */
	private Date endDate;
	/**
	 * 累计场观人数
	 */
	private String totalOnlineNum;
	/**
	 * 场观人数
	 */
	private String observationNum;
	/**
	 * 弹幕总数
	 */
	private Integer totalBarrageNum;
	/**
	 * 最高在线人数
	 */
	private Integer onlineMaxNum;
	/**
	 * 租户id
	 */
	private Long tenantId;
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
