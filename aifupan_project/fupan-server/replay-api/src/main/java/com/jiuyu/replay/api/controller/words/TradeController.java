package com.jiuyu.replay.api.controller.words;

import com.jiuyu.replay.api.logic.words.TradeLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.words.TradeInfoVo;
import com.jiuyu.replay.generic.vo.words.TradeListVo;
import com.jiuyu.replay.words.bo.TradeBo;
import com.jiuyu.replay.words.bo.TradeListBo;
import com.jiuyu.replay.words.vo.AnchorTradeListVo;
import com.jiuyu.replay.words.vo.TradeSimpleTreeVo;
import com.jiuyu.replay.words.vo.TradeTreeVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 行业
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-06-20 16:22:18
 */
@RestController
@CrossOrigin
@RequestMapping("replay/trade")
@Tag(name = "行业")
public class TradeController {

    @Resource
    private TradeLogic tradeLogic;

    /**
     * 根据用户租户主播返回行业列表
     * @return
     */
    @PostMapping("/listByTenantAnchor")
    @Operation(summary = "根据用户租户主播返回行业列表")
    public R<List<AnchorTradeListVo>> listByTenantAnchor(){

        return tradeLogic.listByTenantAnchor();
    }

    /**
     * 根据用户添加的主播返回行业列表
     * @return
     */
    @PostMapping("/listByAnchor")
    @Operation(summary = "根据用户添加的主播返回行业列表")
    public R<List<AnchorTradeListVo>> listByAnchor(){

        return tradeLogic.listByAnchor();
    }

    /**
     * 获取行业列表（树形结构）
     * @param childrenNotNull 当没有子行业时，子行业列表是否返回空集合 0：直接返回null 1：返回空集合
     * @return
     */
    @GetMapping("/listTree")
    @Operation(summary = "获取行业列表（树形结构）")
    public R<List<TradeTreeVo>> listTree(@RequestParam(required = false) Integer childrenNotNull) {

        return tradeLogic.listTree(childrenNotNull);
    }

    /**
     * 获取行业列表（树形结构，简化版-客户端用）
     * @param childrenNotNull 当没有子行业时，子行业列表是否返回空集合 0：直接返回null 1：返回空集合
     * @return
     */
    @GetMapping("/listSimpleTree")
    @Operation(summary = "获取行业列表（树形结构，简化版-客户端用）")
    public R<List<TradeSimpleTreeVo>> listSimpleTree(@RequestParam(required = false) Integer childrenNotNull) {

        return tradeLogic.listSimpleTree(childrenNotNull);
    }

    /**
     * 行业列表
     * @param tradeListBo 行业列表查询参数
     * @return
     */
    @PostMapping("/list")
    @Operation(summary = "行业列表")
    public R<PageUtils<TradeListVo>> list(@Parameter(description = "行业列表查询参数", required = true) @RequestBody TradeListBo tradeListBo){

        return tradeLogic.queryPage(tradeListBo);
    }


    /**
     * 行业信息
     * @param id 行业id
     * @return
     */
    @GetMapping("/info")
    @Operation(summary = "行业信息")
    public R<TradeInfoVo> info(@Parameter(description = "行业id", required = true) @RequestParam("id") Long id){

        return tradeLogic.info(id);
    }

    /**
     * 新增行业
     * @param tradeBo 行业对象
     * @return
     */
    @PostMapping("/save")
    @Operation(summary = "新增行业")
    public R<String> save(@Parameter(description = "行业对象", required = true) @RequestBody TradeBo tradeBo){

        return tradeLogic.save(tradeBo);
    }

    /**
     * 修改行业
     * @param tradeBo 行业对象
     * @return
     */
    @PostMapping("/update")
    @Operation(summary = "修改行业")
    public R<String> update(@Parameter(description = "行业对象", required = true) @RequestBody TradeBo tradeBo){

        return tradeLogic.update(tradeBo);
    }

    /**
     * 删除行业
     * @param id 行业id
     * @return
     */
    @GetMapping("/delete")
    @Operation(summary = "删除行业")
    public R<String> delete(@Parameter(description = "行业id", required = true) @RequestParam("id") Long id){

        return tradeLogic.delete(id);
    }


    /**
     * 获取一二级行业树形结构
     * @return
     */
    @GetMapping("/listSimpleTreeL2")
    @Operation(summary = "获取一二级行业树形结构")
    public R<List<TradeSimpleTreeVo>> listSimpleTreeL2() {
        return tradeLogic.listSimpleTreeL2();
    }

    /**
     * 删除行业模型
     * @param tradeId
     * @param modelId
     * @return
     */
    @GetMapping("/deleteTradeModel")
    @Operation(summary = "删除行业模型")
    public R<String> deleteTradeModel(@Parameter(description = "行业id", required = false) @RequestParam("tradeId") Long tradeId,
                                      @Parameter(description = "行业模型id", required = true) @RequestParam("modelId") Long modelId){

        return tradeLogic.deleteTradeModel(tradeId,modelId);

    }
}
