package com.jiuyu.replay.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 文件
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-07-10 11:13:51
 */
@Data
@TableName("tb_file")
public class FileEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@TableId(value = "id", type = IdType.INPUT)
	private Long id;
	/**
	 * 文件名字
	 */
	private String fileName;
	/**
	 * 文件大小
	 */
	private Long fileSize;
	/**
	 * 文件类型 如：png、word、pdf等
	 */
	private String fileType;
	/**
	 * 文件存放的url
	 */
	private String fileUrl;
	/**
	 * 文件在cos的存储key
	 */
	private String cosSaveKey;
	/**
	 * 来源id
	 */
	private Long resourceId;
	/**
	 * 来源类型 0：用户头像图片 1：商品主图 2：商品详情图 999：未定义
	 */
	private Integer resourceType;
	/**
	 * 描述
	 */
	private String remarks;
	/**
	 * 排序
	 */
	private Integer sort;
	/**
	 * 创建时间
	 */
	private Date createDate;
	/**
	 * 最后修改时间
	 */
	private Date updateDate;
	/**
	 * 是否已删除
	 */
	private Integer isDeleted;


}
