package com.jiuyu.replay.order.vo;

import com.jiuyu.replay.generic.vo.common.FileShowVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 套餐表(用户版本)列表项
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-08 10:49:32
 */
@Data
@Schema(description = "套餐表(用户版本)列表项")
public class PackageListVo extends PackageVo implements Serializable {
	private static final long serialVersionUID = 1L;

	@Schema(description = "套餐类型集合")
	private List<TypeConsumptionVo> typeConsumptionList;

	/**
	 * 商品价格列表
	 */
	@Schema(description = "商品价格列表")
	private List<CommodityPriceVo> commodityPriceList;

	@Schema(description = "商品列表")
	private List<IncrementInfoVo> incrementList;

	@Schema(description = "logo图片列表")
	private List<FileShowVo> logoImgList;

	@Schema(description = "官网logo图片列表")
	private List<FileShowVo> websiteLogoImagesList;

    @Schema(description = "套餐用户列表")
    private List<PackageUserVo> packageUserList;
}
