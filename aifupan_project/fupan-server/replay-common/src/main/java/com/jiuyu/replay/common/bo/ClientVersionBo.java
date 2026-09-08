package com.jiuyu.replay.common.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 客户端对应的版本信息
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-11-13 16:29:50
 */
@Data
@Schema(description = "客户端对应的版本信息")
public class ClientVersionBo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * id
	 */
	@Schema(description = "id")
	private Long id;
	/**
	 * 客户端的cpuid，每一台电脑都有一个
	 */
	@Schema(description = "客户端的cpuid，每一台电脑都有一个")
	private String clientCpuid;
	/**
	 * 客户端的uuid，每一个客户端都有一个
	 */
	@Schema(description = "客户端的uuid，每一个客户端都有一个")
	private String clientUuid;
	/**
	 * 客户端版本号
	 */
	@Schema(description = "客户端版本号")
	private String clientVersion;
	/**
	 * 用户id
	 */
	@Schema(description = "用户id")
	private Long userId;
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


}
