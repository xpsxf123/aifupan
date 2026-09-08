package com.jiuyu.replay.api.controller.third;

import com.jiuyu.replay.api.logic.third.ProxyIpLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.third.bo.ProxyIpBo;
import com.jiuyu.replay.third.bo.ProxyIpListBo;
import com.jiuyu.replay.third.vo.ProxyIpInfoVo;
import com.jiuyu.replay.third.vo.ProxyIpListVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;



/**
 * 代理ip提取
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-02-27 10:37:17
 */
@RestController
@CrossOrigin
@RequestMapping("replay/proxyip")
@Tag(name = "代理ip提取")
public class ProxyIpController {

    @Resource
    private ProxyIpLogic proxyIpLogic;

    /**
     * 代理ip提取列表
     * @param proxyIpListBo 代理ip提取列表查询参数
     * @return
     */
    @PostMapping("/list")
    @Operation(summary = "代理ip提取列表")
    public R<PageUtils<ProxyIpListVo>> list(@Parameter(description = "代理ip提取列表查询参数", required = true) @RequestBody ProxyIpListBo proxyIpListBo){

        return proxyIpLogic.queryPage(proxyIpListBo);
    }


    /**
     * 代理ip提取信息
     * @param id 代理ip提取id
     * @return
     */
    @GetMapping("/info")
    @Operation(summary = "代理ip提取信息")
    public R<ProxyIpInfoVo> info(@Parameter(description = "代理ip提取id", required = true) @RequestParam("id") Long id){

        return proxyIpLogic.info(id);
    }

    /**
     * 新增代理ip提取
     * @param proxyIpBo 代理ip提取对象
     * @return
     */
    @PostMapping("/save")
    @Operation(summary = "新增代理ip提取")
    public R<String> save(@Parameter(description = "代理ip提取对象", required = true) @RequestBody ProxyIpBo proxyIpBo){

        return proxyIpLogic.save(proxyIpBo);
    }

    /**
     * 修改代理ip提取
     * @param proxyIpBo 代理ip提取对象
     * @return
     */
    @PostMapping("/update")
    @Operation(summary = "修改代理ip提取")
    public R<String> update(@Parameter(description = "代理ip提取对象", required = true) @RequestBody ProxyIpBo proxyIpBo){

        return proxyIpLogic.update(proxyIpBo);
    }

    /**
     * 删除代理ip提取
     * @param id 代理ip提取id
     * @return
     */
    @GetMapping("/delete")
    @Operation(summary = "删除代理ip提取")
    public R<String> delete(@Parameter(description = "代理ip提取id", required = true) @RequestParam("id") Long id){

        return proxyIpLogic.delete(id);
    }

}
