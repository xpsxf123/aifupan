package com.jiuyu.replay.api.controller.third;

import com.jiuyu.replay.api.logic.third.ProxyIpRecordLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.third.bo.ProxyIpRecordBo;
import com.jiuyu.replay.third.bo.ProxyIpRecordListBo;
import com.jiuyu.replay.third.vo.ProxyIpRecordInfoVo;
import com.jiuyu.replay.third.vo.ProxyIpRecordListVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;



/**
 * 代理ip提取记录
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-02-27 10:37:17
 */
@RestController
@CrossOrigin
@RequestMapping("replay/proxyiprecord")
@Tag(name = "代理ip提取记录")
public class ProxyIpRecordController {

    @Resource
    private ProxyIpRecordLogic proxyIpRecordLogic;

    /**
     * 代理ip提取记录列表
     * @param proxyIpRecordListBo 代理ip提取记录列表查询参数
     * @return
     */
    @PostMapping("/list")
    @Operation(summary = "代理ip提取记录列表")
    public R<PageUtils<ProxyIpRecordListVo>> list(@Parameter(description = "代理ip提取记录列表查询参数", required = true) @RequestBody ProxyIpRecordListBo proxyIpRecordListBo){

        return proxyIpRecordLogic.queryPage(proxyIpRecordListBo);
    }


    /**
     * 代理ip提取记录信息
     * @param id 代理ip提取记录id
     * @return
     */
    @GetMapping("/info")
    @Operation(summary = "代理ip提取记录信息")
    public R<ProxyIpRecordInfoVo> info(@Parameter(description = "代理ip提取记录id", required = true) @RequestParam("id") Long id){

        return proxyIpRecordLogic.info(id);
    }

    /**
     * 新增代理ip提取记录
     * @param proxyIpRecordBo 代理ip提取记录对象
     * @return
     */
    @PostMapping("/save")
    @Operation(summary = "新增代理ip提取记录")
    public R<String> save(@Parameter(description = "代理ip提取记录对象", required = true) @RequestBody ProxyIpRecordBo proxyIpRecordBo){

        return proxyIpRecordLogic.save(proxyIpRecordBo);
    }

    /**
     * 修改代理ip提取记录
     * @param proxyIpRecordBo 代理ip提取记录对象
     * @return
     */
    @PostMapping("/update")
    @Operation(summary = "修改代理ip提取记录")
    public R<String> update(@Parameter(description = "代理ip提取记录对象", required = true) @RequestBody ProxyIpRecordBo proxyIpRecordBo){

        return proxyIpRecordLogic.update(proxyIpRecordBo);
    }

    /**
     * 删除代理ip提取记录
     * @param id 代理ip提取记录id
     * @return
     */
    @GetMapping("/delete")
    @Operation(summary = "删除代理ip提取记录")
    public R<String> delete(@Parameter(description = "代理ip提取记录id", required = true) @RequestParam("id") Long id){

        return proxyIpRecordLogic.delete(id);
    }

}
