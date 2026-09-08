package com.jiuyu.replay.words.bo;


import com.jiuyu.replay.common.bo.PageBo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户视频申述表列表查询参数
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-11-14 12:12:11
 */
@Data
@Schema(description = "用户视频申述表列表查询参数")
public class UserVideoAppealListBo extends PageBo implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(description = "申述用户id")
	private Long userId;

	@Schema(description = "昵称")
	private String nickName;

	@Schema(description = "直播间账号昵称")
	private String liveUserName;

	@Schema(description = "公司名称")
	private String companyName;

	@Schema(description = "联系人")
	private String contacts;

	@Schema(description = "手机号码")
	private String phone;

	@Schema(description = "状态 0待处理，1已处理")
	private Integer status;

	@Schema(description = "开始的创建时间")
	private Date startCreateDate;

	@Schema(description = "结束的创建时间")
	private Date endCreateDate;
}
