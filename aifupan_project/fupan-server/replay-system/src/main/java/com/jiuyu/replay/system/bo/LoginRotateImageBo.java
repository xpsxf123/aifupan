package com.jiuyu.replay.system.bo;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 客户端登录页轮播图信息
 *
 * @author lyw
 * @email 1798883178@qq.com
 * @date 2025-05-22 15:27:24
 */
@Data
@Schema(description = "客户端登录页轮播图信息")
public class LoginRotateImageBo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 客户端登录页面，轮播图 主键
	 */
	@Schema(description = "客户端登录页面，轮播图 主键")
	private Long id;
	/**
	 * 存入者id
	 */
	@Schema(description = "存入者id")
	private Long userId;
	/**
	 * 默认0 启用，1停止展示
	 */
	@Schema(description = "默认0 启用，1停止展示")
	private Integer imgStatus;
	/**
	 * 内容，每张图底下的内容
	 */
	@Schema(description = "内容，每张图底下的内容")
	private String content;
	/**
	 * 每张图片的file_id 关联tb_file_id
	 */
	@Schema(description = "每张图片的file_id 关联tb_file_id")
	private Long fileId;

	@Schema(description = "排序，从0开始")
	private Integer sort;

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
	/**
	 * 是否已删除
	 */
	@Schema(description = "是否已删除")
	private Integer isDeleted;


}
