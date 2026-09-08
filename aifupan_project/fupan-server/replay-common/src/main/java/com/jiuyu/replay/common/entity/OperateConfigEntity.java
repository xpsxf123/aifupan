package com.jiuyu.replay.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 运营设置
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2025-01-02 21:22:14
 */
@Data
@TableName("tb_operate_config")
public class OperateConfigEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@TableId(value = "id", type = IdType.INPUT)
	private Long id;
	/**
	 * 用户默认的角色id
	 */
	private Long defaultRoleId;
	/**
	 * 给用户默认的商品id
	 */
	private Long defaultCommodityId;
	/**
	 * websocket地址类型 0：js获取，1：浏览器获取
	 */
	private Integer websocketType;
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
