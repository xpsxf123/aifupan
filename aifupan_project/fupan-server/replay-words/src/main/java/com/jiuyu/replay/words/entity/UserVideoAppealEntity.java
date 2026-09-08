package com.jiuyu.replay.words.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户视频申述表
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-11-14 12:12:11
 */
@Data
@TableName("tb_user_video_appeal")
public class UserVideoAppealEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * id
	 */
	@TableId(value = "id", type = IdType.INPUT)
	private Long id;
	/**
	 * 申述用户id
	 */
	private Long userId;
	/**
	 * 昵称
	 */
	private String nickName;
	/**
	 * 直播间账号昵称
	 */
	private String liveUserName;
	/**
	 * 公司名称
	 */
	private String companyName;
	/**
	 * 联系人
	 */
	private String contacts;
	/**
	 * 手机号码
	 */
	private String phone;
	/**
	 * 申述原因
	 */
	private String appealReason;
	/**
	 * 申述的主播id
	 */
	private String anchorUrlId;
	/**
	 * 申述的视频id
	 */
	private String anchorVideoId;
	/**
	 * 状态 0待处理，1已处理
	 */
	private Integer status;
	/**
	 * 处理人用户id
	 */
	private Long handleUserId;
	/**
	 * 处理时间
	 */
	private Date handleDate;
	/**
	 * 处理备注
	 */
	private String handleRemarks;
	/**
	 * 创建时间
	 */
	private Date createDate;
	/**
	 * 修改时间
	 */
	private Date updateDate;
	/**
	 * 删除标记
	 */
	private Integer isDeleted;


}
