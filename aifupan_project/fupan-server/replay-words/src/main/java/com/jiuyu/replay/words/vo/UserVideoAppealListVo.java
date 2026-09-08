package com.jiuyu.replay.words.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户视频申述表列表项
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-11-14 12:12:11
 */
@Data
@Schema(description = "用户视频申述表列表项")
public class UserVideoAppealListVo extends UserVideoAppealVo implements Serializable {
	private static final long serialVersionUID = 1L;

	@Schema(description = "主播名称")
	private String anchorUrlName;

	@Schema(description = "视频名称")
	private String anchorVideoName;

}
