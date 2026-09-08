package com.jiuyu.replay.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 客户端登录页轮播图
 *
 * @author lyw
 * @email 1798883178@qq.com
 * @date 2025-05-22 15:27:24
 */
@Data
@TableName("tb_login_rotate_image")
public class LoginRotateImageEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 客户端登录页面，轮播图 主键
	 */
	@TableId(value = "id", type = IdType.INPUT)
	private Long id;
	/**
	 * 存入者id
	 */
	private Long userId;
	/**
	 * 默认0 启用，1停止展示
	 */
	private Integer imgStatus;
	private Integer sort;
	/**
	 * 内容，每张图底下的内容
	 */
	private String content;
	/**
	 * 每张图片的file_id 关联tb_file_id
	 */
	private Long fileId;
	/**
	 * 创建时间
	 */
	private Date createDate;
	/**
	 * 修改时间
	 */
	private Date updateDate;
	/**
	 * 是否已删除
	 */
	private Integer isDeleted;


}
