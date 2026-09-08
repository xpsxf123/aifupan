package com.jiuyu.replay.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 客户端文件
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-10-08 15:03:35
 */
@Data
@TableName("tb_client_file")
public class ClientFileEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 主键
	 */
	@TableId(value = "id", type = IdType.INPUT)
	private Long id;
	/**
	 * 文件名称
	 */
	private String fileName;
	/**
	 * 文件类型 0exe,1dll,2sql文件
	 */
	private Integer fileType;
	/**
	 * 更新表主键
	 */
	private Long updateId;


}
