package com.jiuyu.replay.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 客户端对应的版本
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-11-13 16:29:50
 */
@Data
@TableName("tb_client_version")
public class ClientVersionEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * id
	 */
	@TableId(value = "id", type = IdType.INPUT)
	private Long id;
	/**
	 * 客户端的cpuid，每一台电脑都有一个
	 */
	private String clientCpuid;
	/**
	 * 客户端的uuid，每一个客户端都有一个
	 */
	private String clientUuid;
	/**
	 * 客户端版本号
	 */
	private String clientVersion;
	/**
	 * 用户id
	 */
	private Long userId;
	/**
	 * 创建时间
	 */
	private Date createDate;
	/**
	 * 修改时间
	 */
	private Date updateDate;


}
