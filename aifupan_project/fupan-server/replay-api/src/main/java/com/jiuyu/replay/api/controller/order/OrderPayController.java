package com.jiuyu.replay.api.controller.order;

import com.jiuyu.replay.api.logic.order.OrderPayLogic;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

import com.jiuyu.replay.generic.vo.order.OrderPayListVo;
import com.jiuyu.replay.generic.vo.order.OrderPayInfoVo;
import com.jiuyu.replay.order.bo.OrderPayBo;
import com.jiuyu.replay.order.bo.OrderPayListBo;



/**
 * 订单-支付信息
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-10 11:42:33
 */
@RestController
@CrossOrigin
@RequestMapping("replay/orderpay")
@Tag(name = "订单-支付信息")
public class OrderPayController {

    @Resource
    private OrderPayLogic orderPayLogic;

    /**
     * 订单-支付信息列表
     * @param orderPayListBo 订单-支付信息列表查询参数
     * @return
     */
    @PostMapping("/list")
    @Operation(summary = "订单-支付信息列表")
    public R<PageUtils<OrderPayListVo>> list(@Parameter(description = "订单-支付信息列表查询参数", required = true) @RequestBody OrderPayListBo orderPayListBo){

        return orderPayLogic.queryPage(orderPayListBo);
    }


    /**
     * 订单-支付信息信息
     * @param id 订单-支付信息id
     * @return
     */
    @GetMapping("/info")
    @Operation(summary = "订单-支付信息信息")
    public R<OrderPayInfoVo> info(@Parameter(description = "订单-支付信息id", required = true) @RequestParam("id") Long id){

        return orderPayLogic.info(id);
    }

    /**
     * 新增订单-支付信息
     * @param orderPayBo 订单-支付信息对象
     * @return
     */
    @PostMapping("/save")
    @Operation(summary = "新增订单-支付信息")
    public R<String> save(@Parameter(description = "订单-支付信息对象", required = true) @RequestBody OrderPayBo orderPayBo){

        return orderPayLogic.save(orderPayBo);
    }

    /**
     * 修改订单-支付信息
     * @param orderPayBo 订单-支付信息对象
     * @return
     */
    @PostMapping("/update")
    @Operation(summary = "修改订单-支付信息")
    public R<String> update(@Parameter(description = "订单-支付信息对象", required = true) @RequestBody OrderPayBo orderPayBo){

        return orderPayLogic.update(orderPayBo);
    }

    /**
     * 删除订单-支付信息
     * @param id 订单-支付信息id
     * @return
     */
    @GetMapping("/delete")
    @Operation(summary = "删除订单-支付信息")
    public R<String> delete(@Parameter(description = "订单-支付信息id", required = true) @RequestParam("id") Long id){

        return orderPayLogic.delete(id);
    }

}
