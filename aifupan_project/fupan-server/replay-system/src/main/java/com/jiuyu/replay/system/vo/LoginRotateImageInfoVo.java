package com.jiuyu.replay.system.vo;


import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;

/**
 * 客户端登录页轮播图信息项
 *
 * @author lyw
 * @email 1798883178@qq.com
 * @date 2025-05-22 15:27:24
 */
@Data
@Schema(description = "客户端登录页轮播图信息项")
public class LoginRotateImageInfoVo extends LoginRotateImageVo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 文件名字
	 */
	@Schema(description = "文件名字")
	private String name;
	/**
	 * 来源id
	 */
	@Schema(description = "来源id")
	private Long resourceId;
	/**
	 * 文件显示/下载地址
	 */
	@Schema(description = "文件显示/下载地址")
	private String url;


}
