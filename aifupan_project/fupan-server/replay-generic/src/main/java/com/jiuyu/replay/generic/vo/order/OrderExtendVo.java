package com.jiuyu.replay.generic.vo.order;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import com.jiuyu.replay.generic.vo.common.FileShowVo;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 订单的扩展表信息
 *
 * @author lyw
 * @email 1798883178@qq.com
 * @date 2025-07-05 09:56:24
 */
@Data
@Schema(description = "订单的扩展表信息")
public class OrderExtendVo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@Schema(description = "ID")
	private Long id;
	/**
	 * 订单id
	 */
	@Schema(description = "订单id")
	private Long orderId;
	/**
	 * 付费截图，最多25张图片，图片id之间使用逗号隔开
	 */
	@Schema(description = "付费截图，最多25张图片，图片id之间使用逗号隔开")
	private String payPictures;
	/**
	 * 付费截图，最多25张图片id
	 */
	@Schema(description = "付费截图列表")
	private List<Long> payPictureList;
	/**
	 * 付费截图地址列表
	 */
	@Schema(description = "付费截图地址列表")
	private List<FileShowVo> fileList;
	/**
	 * 备注
	 */
	@Schema(description = "备注")
	private String remarks;
	/**
	 * 修改者id
	 */
	@Schema(description = "修改者id")
	private Long updateId;
	/**
	 * 最后修改时间
	 */
	@Schema(description = "最后修改时间")
	private Date updateDate;
	/**
	 * 创建者id
	 */
	@Schema(description = "创建者id")
	private Long createId;
	/**
	 * 创建时间
	 */
	@Schema(description = "创建时间")
	private Date createDate;
	/**
	 * 是否已删除
	 */
	@Schema(description = "是否已删除")
	private Integer isDeleted;


}
