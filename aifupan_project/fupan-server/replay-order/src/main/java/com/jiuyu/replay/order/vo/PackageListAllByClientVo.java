package com.jiuyu.replay.order.vo;

import com.jiuyu.replay.generic.vo.order.OrderInfoVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 套餐表(用户版本)信息项
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-08 10:49:32
 */
@Data
@Schema(description = "获取套餐列表-客户端")
public class PackageListAllByClientVo {

    @Schema(description = "套餐id")
    private String packageId;

    @Schema(description = "套餐名称")
    private String packageName;

    @Schema(description = "当前在用订单")
    private List<OrderInfoVo> currentOrderList;

    @Schema(description = "套餐列表")
    private List<PackageListVo> packageList;
}
