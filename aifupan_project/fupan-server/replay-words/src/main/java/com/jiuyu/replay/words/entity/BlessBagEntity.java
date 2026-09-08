package com.jiuyu.replay.words.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 福袋信息
 *
 * @author lyw
 * @email 1798883178@qq.com
 * @date 2025-05-22 20:02:21
 */
@Data
@TableName("tb_bless_bag")
public class BlessBagEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 福袋 主键
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
	 * 视频id
	 */
	private String videoId;
	/**
	 * 归属批次号 批次号是指：在录制一个视频中，如果采用分段录制，那么这几段视频归属于同一批次 目前是采集的直播的房间Id, room_id
	 */
	private String batchNumber;
	/**
	 * 福袋信息
	 */
	private String lotteryInfo;
	/**
	 * 参与抽奖的条件
	 */
	private String conditions;
	/**
	 * 总奖品数量
	 */
	private Integer prizeCount;
	/**
	 * 福袋数量
	 */
	private Integer luckyCount;
	/**
	 * 倒计时（单位：秒）
	 */
	private Integer countDown;
	/**
	 * 抽奖开始时间
	 */
	private Long startTime;
	/**
	 * 抽奖结束时间
	 */
	private Long drawTime;
	/**
	 * 当前时间
	 */
	@TableField("`current_day`")
	private Long currentTime;
	/**
	 * 参与人数
	 */
	private Integer candidateNum;
	/**
	 * 当前时间
	 */
	@TableField("`now_time`")
	private Long now;
	/**
	 * 创建时间
	 */
	private Date createDate;
	/**
	 * 修改时间
	 */
	private Date updateDate;
	/**
	 * 是否已删除
	 */
	private Integer isDeleted;


}
