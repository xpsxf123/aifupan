package com.jiuyu.replay.common.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 客户端更新信息
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-10-08 15:03:35
 */
@Data
@Schema(description = "客户端更新信息")
public class ClientUpdateBo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 主键
	 */
	@Schema(description = "主键")
	private Long id;
	/**
	 * 版本号
	 */
	@Schema(description = "版本号")
	private String versionNum;
	/**
	 * 更新描述
	 */
	@Schema(description = "更新描述")
	private String updateInfo;
	/**
	 * 上传时间
	 */
	@Schema(description = "上传时间")
	private Date updateTime;


	/**
	 * 更新类型 0正常更新（手动下载更新） 1快速更新（强制更新）
	 */
	@Schema(description = "更新类型 0正常更新（手动下载更新） 1快速更新（强制更新)")
	private Integer updateType;

	/**
	 * 更新类型 0爱复盘主程序更新, 1更新程序更新，2爱复盘补丁
	 */
	@Schema(description = "更新类型 0爱复盘主程序更新, 1更新程序更新，2爱复盘补丁")
	private Integer isFront;

	/**
	 * 是否保留/维护 0否 1是
	 */
	@Schema(description = "是否保留/维护 0否 1是")
	private Integer isPreserve;

	/**
	 * 客户端文件主键列表
	 */
	@Schema(description = "客户端文件主键列表")
	private List<String> fileIds;

	/**
	 * 版本编号 数字越大，版本越新
	 */
	@Schema(description = "版本编号 数字越大，版本越新")
	private Double version;

	/**
	 * 文件的md5
	 */
	@Schema(description = "文件的md5")
	private String fileMd5;
	/**
	 * 父级id
	 */
	@Schema(description = "父级id")
	private Long parentId;
	/**
	 * 状态，0开发状态，1发布
	 */
	@Schema(description = "状态，0开发状态，1发布")
	private Integer status;

	@Schema(description = "文件上传到cos的key")
	private String cosKey;

	@Schema(description = "客户端更新包COS key")
	private String clientCosKey;

	@Schema(description = "COS解压后的文件路径(JSON)")
	private String clientFilesPath;
}
