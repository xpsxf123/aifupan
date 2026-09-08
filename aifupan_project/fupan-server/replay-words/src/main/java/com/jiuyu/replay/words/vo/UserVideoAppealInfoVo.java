package com.jiuyu.replay.words.vo;


import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;

/**
 * 用户视频申述表信息项
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-11-14 12:12:11
 */
@Data
@Schema(description = "用户视频申述表信息项")
public class UserVideoAppealInfoVo extends UserVideoAppealVo implements Serializable {
	private static final long serialVersionUID = 1L;



}
