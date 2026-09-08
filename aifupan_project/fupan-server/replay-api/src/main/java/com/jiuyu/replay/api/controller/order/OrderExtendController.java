package com.jiuyu.replay.api.controller.order;



import com.jiuyu.replay.api.logic.order.OrderExtendLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.order.OrderExtendInfoVo;
import com.jiuyu.replay.generic.vo.order.OrderExtendListVo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;


import com.jiuyu.replay.common.utils.SnowflakeManager;

import com.jiuyu.replay.order.bo.OrderExtendBo;
import com.jiuyu.replay.order.bo.OrderExtendListBo;



/**
 * 订单的扩展表
 *
 * @author lyw
 * @email 1798883178@qq.com
 * @date 2025-07-05 09:56:24
 */
@RestController
@CrossOrigin
@RequestMapping("order/orderextend")
@Tag(name = "订单的扩展表")
public class OrderExtendController {

    @Resource
    private OrderExtendLogic orderExtendLogic;

    /**
     * 订单的扩展表列表
     * @param orderExtendListBo 订单的扩展表列表查询参数
     * @return
     */
    @PostMapping("/list")
    @Operation(summary = "订单的扩展表列表")
    public R<PageUtils<OrderExtendListVo>> list(@Parameter(description = "订单的扩展表列表查询参数", required = true) @RequestBody OrderExtendListBo orderExtendListBo){

        return orderExtendLogic.queryPage(orderExtendListBo);
    }


    /**
     * 订单的扩展表信息
     * @param id 订单的扩展表id
     * @return
     */
    @GetMapping("/info")
    @Operation(summary = "订单的扩展表信息")
    public R<OrderExtendInfoVo> info(@Parameter(description = "订单的扩展表id", required = true) @RequestParam("id") Long id){

        return orderExtendLogic.info(id);
    }

    /**
     * 新增订单的扩展表
     * @param orderExtendBo 订单的扩展表对象
     * @return
     */
    @PostMapping("/save")
    @Operation(summary = "新增订单的扩展表")
    public R<String> save(@Parameter(description = "订单的扩展表对象", required = true) @RequestBody OrderExtendBo orderExtendBo){

        return orderExtendLogic.save(orderExtendBo);
    }

    /**
     * 修改订单的扩展表
     * @param orderExtendBo 订单的扩展表对象
     * @return
     */
    @PostMapping("/update")
    @Operation(summary = "修改订单的扩展表")
    public R<String> update(@Parameter(description = "订单的扩展表对象", required = true) @RequestBody OrderExtendBo orderExtendBo){

        return orderExtendLogic.update(orderExtendBo);
    }

    /**
     * 删除订单的扩展表
     * @param id 订单的扩展表id
     * @return
     */
    @GetMapping("/delete")
    @Operation(summary = "删除订单的扩展表")
    public R<String> delete(@Parameter(description = "订单的扩展表id", required = true) @RequestParam("id") Long id){

        return orderExtendLogic.delete(id);
    }

}
