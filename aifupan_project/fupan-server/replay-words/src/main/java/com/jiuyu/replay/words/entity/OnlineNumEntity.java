package com.jiuyu.replay.words.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 直播实时在线人数
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-11-14 10:05:41
 */
@Data
@TableName("tb_online_num")
public class OnlineNumEntity implements Serializable {
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
	 * 记录时间
	 */
	private String recordDate;
	/**
	 * 在线人数
	 */
	private String peopleNum;
	/**
	 * 在线人数数据 2024-11-12 17:30:47@2503_2024-11-12 17:31:09@9750
	 */
	private String peopleNumData;
	/**
	 * 视频唯一标识
	 */
	private String videoId;
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
