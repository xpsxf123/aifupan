package com.jiuyu.replay.words.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 运营/违规收藏列表
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-03-12 16:42:18
 */
@Data
@TableName("tb_client_ai_fav")
public class ClientAiFavEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@TableId(value = "id", type = IdType.INPUT)
	private Long id;
	/**
	 * 租户ID
	 */
	private Long tenantId;
	/**
	 * 用户ID
	 */
	private Long userId;
	/**
	 * 收藏类型 0：运营助手 1：违规助手
	 */
	private Integer favType;
	/**
	 * 数据来源类型 0：录制视频 1：文件上传 2：对比
	 */
	private Integer dataResourceType;
	/**
	 * 数据来源id
	 */
	private String dataResourceUuid;
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
