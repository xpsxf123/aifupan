package com.jiuyu.replay.words.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户视频申述表信息
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-11-14 12:12:11
 */
@Data
@Schema(description = "用户视频申述表信息")
public class UserVideoAppealVo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * id
	 */
	@Schema(description = "id")
	private Long id;
	/**
	 * 申述用户id
	 */
	@Schema(description = "申述用户id")
	private Long userId;
	/**
	 * 昵称
	 */
	@Schema(description = "昵称")
	private String nickName;
	/**
	 * 直播间账号昵称
	 */
	@Schema(description = "直播间账号昵称")
	private String liveUserName;
	/**
	 * 公司名称
	 */
	@Schema(description = "公司名称")
	private String companyName;
	/**
	 * 联系人
	 */
	@Schema(description = "联系人")
	private String contacts;
	/**
	 * 手机号码
	 */
	@Schema(description = "手机号码")
	private String phone;
	/**
	 * 申述原因
	 */
	@Schema(description = "申述原因")
	private String appealReason;
	/**
	 * 申述的主播id
	 */
	@Schema(description = "申述的主播id")
	private String anchorUrlId;
	/**
	 * 申述的视频id
	 */
	@Schema(description = "申述的视频id")
	private String anchorVideoId;
	/**
	 * 状态 0待处理，1已处理
	 */
	@Schema(description = "状态 0待处理，1已处理")
	private Integer status;
	/**
	 * 处理人用户id
	 */
	@Schema(description = "处理人用户id")
	private Long handleUserId;
	/**
	 * 处理时间
	 */
	@Schema(description = "处理时间")
	private Date handleDate;
	/**
	 * 处理备注
	 */
	@Schema(description = "处理备注")
	private String handleRemarks;
	/**
	 * 创建时间
	 */
	@Schema(description = "创建时间")
	private Date createDate;
	/**
	 * 修改时间
	 */
	@Schema(description = "修改时间")
	private Date updateDate;
	/**
	 * 删除标记
	 */
	@Schema(description = "删除标记")
	private Integer isDeleted;


}
