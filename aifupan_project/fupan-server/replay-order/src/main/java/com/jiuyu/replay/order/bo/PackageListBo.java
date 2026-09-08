package com.jiuyu.replay.order.bo;


import com.jiuyu.replay.common.bo.PageBo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 套餐表(用户版本)列表查询参数
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-08 10:49:32
 */
@Data
@Schema(description = "套餐表(用户版本)列表查询参数")
public class PackageListBo extends PageBo implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * 模糊搜索查询条件
	 */
	@Schema(description = "模糊搜索查询条件")
	private String name;

	/**
	 * 状态 0未上架，1已上架
	 */
	@Schema(description = "状态 0未上架，1已上架")
	private Integer status;

	/**
	 * 套餐类型(用户只能购买主要套餐)：1主要套餐，2次要套餐
	 */
	@Schema(description = "套餐类型(用户只能购买主要套餐)：1主要套餐，2次要套餐")
	private Integer packageType;

    /**
     * 是否自定义 0否，1是
     */
    @Schema(description = "是否自定义 0否，1是")
    private Integer customizeType;

    /**
     * 手机号
     */
    @Schema(description = "手机号")
    private String phone;

    /**
     * 用户ID列表
     */
    @Schema(description = "用户ID列表")
    private List<Long> userIds;
}
