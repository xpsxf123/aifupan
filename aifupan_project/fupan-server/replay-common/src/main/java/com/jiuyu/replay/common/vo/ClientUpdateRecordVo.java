package com.jiuyu.replay.common.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 客户端更新记录信息
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-10-08 15:03:35
 */
@Data
@Schema(description = "客户端更新记录信息")
public class ClientUpdateRecordVo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 主键
	 */
	@Schema(description = "")
	private Long id;
	/**
	 * 旧版本号
	 */
	@Schema(description = "旧版本号")
	private String oldVersion;
	/**
	 * 新版本号
	 */
	@Schema(description = "新版本号")
	private String updateVersion;
	/**
	 * 更新时间
	 */
	@Schema(description = "更新时间")
	private Date updateTime;
	/**
	 * 客户端当前登录的用户名，如果是启动的时候更新，则为0
	 */
	@Schema(description = "客户端当前登录的用户名，如果是启动的时候更新，则为0")
	private String clientUser;
	/**
	 * 更新类型  0用户手动更新，1系统强制更新
	 */
	@Schema(description = "更新类型  0用户手动更新，1系统强制更新")
	private Integer updateType;
	/**
	 * 更新表的主键
	 */
	@Schema(description = "更新表的主键")
	private Long updateId;

	/**
	 * 更新类型 0爱复盘主程序更新, 1更新程序更新，2爱复盘补丁
	 */
	@Schema(description = "更新类型 0爱复盘主程序更新, 1更新程序更新，2爱复盘补丁")
	private Integer isFront;


}
