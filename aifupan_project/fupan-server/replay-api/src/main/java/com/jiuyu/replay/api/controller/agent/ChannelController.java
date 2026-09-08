package com.jiuyu.replay.api.controller.agent;

import com.jiuyu.replay.agent.bo.ChannelBo;
import com.jiuyu.replay.agent.bo.ChannelListBo;
import com.jiuyu.replay.agent.vo.ChannelInfoVo;
import com.jiuyu.replay.agent.vo.ChannelListVo;
import com.jiuyu.replay.agent.vo.ChannelTreeVo;
import com.jiuyu.replay.api.logic.agent.ChannelLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;



/**
 * 用户来源渠道表
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-01-21 10:16:14
 */
@RestController
@CrossOrigin
@RequestMapping("replay/channel")
@Tag(name = "用户来源渠道表")
public class ChannelController {

    @Resource
    private ChannelLogic channelLogic;

    /**
     * 用户来源渠道表列表
     * @param channelListBo 用户来源渠道表列表查询参数
     * @return
     */
    @PostMapping("/list")
    @Operation(summary = "用户来源渠道表列表")
    public R<PageUtils<ChannelListVo>> list(@Parameter(description = "用户来源渠道表列表查询参数", required = true) @RequestBody ChannelListBo channelListBo){

        return channelLogic.queryPage(channelListBo);
    }


    /**
     * 用户来源渠道表信息
     * @param id 用户来源渠道表id
     * @return
     */
    @GetMapping("/info")
    @Operation(summary = "用户来源渠道表信息")
    public R<ChannelInfoVo> info(@Parameter(description = "用户来源渠道表id", required = true) @RequestParam("id") Long id){

        return channelLogic.info(id);
    }

    /**
     * 新增用户来源渠道表
     * @param channelBo 用户来源渠道表对象
     * @return
     */
    @PostMapping("/save")
    @Operation(summary = "新增用户来源渠道表")
    public R<String> save(@Parameter(description = "用户来源渠道表对象", required = true) @RequestBody ChannelBo channelBo){

        return channelLogic.save(channelBo);
    }

    /**
     * 修改用户来源渠道表
     * @param channelBo 用户来源渠道表对象
     * @return
     */
    @PostMapping("/update")
    @Operation(summary = "修改用户来源渠道表")
    public R<String> update(@Parameter(description = "用户来源渠道表对象", required = true) @RequestBody ChannelBo channelBo){

        return channelLogic.update(channelBo);
    }

    /**
     * 删除用户来源渠道表
     * @param id 用户来源渠道表id
     * @return
     */
    @GetMapping("/delete")
    @Operation(summary = "删除用户来源渠道表")
    public R<String> delete(@Parameter(description = "用户来源渠道表id", required = true) @RequestParam("id") Long id){

        return channelLogic.delete(id);
    }


    /**
     * 获取来源渠道列表(树型结构)
     * @param childrenNotNull 当没有子行业时，子行业列表是否返回空集合 0：直接返回null 1：返回空集合
     * @return
     */
    @GetMapping("/listTree")
    @Operation(summary = "获取来源渠道列表(树型结构)")
    public R<List<ChannelTreeVo>> listTree(@RequestParam(required = false) Integer childrenNotNull){
        return channelLogic.listTree(childrenNotNull);
    }

}
