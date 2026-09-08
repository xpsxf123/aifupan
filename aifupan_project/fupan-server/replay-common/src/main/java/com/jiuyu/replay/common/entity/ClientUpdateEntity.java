package com.jiuyu.replay.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 客户端更新
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-10-08 15:03:35
 */
@Data
@TableName("tb_client_update")
public class ClientUpdateEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 主键
	 */
	@TableId(value = "id", type = IdType.INPUT)
	private Long id;
	/**
	 * 版本号
	 */
	private String versionNum;
	/**
	 * 更新描述
	 */
	private String updateInfo;

	/**
	 * 更新类型  0用户手动更新，1系统强制更新
	 */
	private Integer updateType;

	/**
	 * 更新类型 0爱复盘主程序更新, 1更新程序更新，2爱复盘补丁
	 */
	private Integer isFront;

	/**
	 * 上传时间
	 */
	private Date updateTime;
	/**
	 * 是否删除 0否 1是
	 */
	private Integer isDeleted;

	/**
	 * 是否保留/维护 0否 1是
	 */
	private Integer isPreserve;

	/**
	 * 版本编号 数字越大，版本越新
	 */
	private Double version;
	/**
	 * 文件的md5
	 */
	private String fileMd5;
	/**
	 * 父级id
	 */
	private Long parentId;
	/**
	 * 状态，0开发状态，1发布
	 */
	private Integer status;
	/**
	 * 文件上传到cos的key
	 */
	private String cosKey;

	/**
	 * 客户端更新包COS key
	 */
	@TableField(exist = false)
	private String clientCosKey;

	/**
	 * COS解压后的文件路径(JSON)
	 */
	@TableField(exist = false)
	private String clientFilesPath;

}
