package com.jiuyu.replay.api.controller.agent;

import com.jiuyu.replay.agent.bo.ClientInviteProgressBo;
import com.jiuyu.replay.agent.bo.ClientInviteProgressListBo;
import com.jiuyu.replay.agent.vo.ClientInviteProgressInfoVo;
import com.jiuyu.replay.agent.vo.ClientInviteProgressListVo;
import com.jiuyu.replay.api.logic.agent.ClientInviteProgressLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 邀请进度
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-20 10:36:55
 */
//@RestController
@CrossOrigin
@RequestMapping("replay/clientinviteprogress")
@Tag(name = "邀请进度")
public class ClientInviteProgressController {

    @Resource
    private ClientInviteProgressLogic clientInviteProgressLogic;

//    /**
//     * 设置邀请进度和进度奖励
//     * @param clientInviteProgressBoList 进度集合
//     * @return
//     */
//    @PostMapping("/setInviteProgressAndReward")
//    @Operation(summary = "设置邀请进度和进度奖励")
//    public R<String> setInviteProgressAndReward(@RequestBody List<ClientInviteProgressBo> clientInviteProgressBoList) {
//
//        return this.clientInviteProgressLogic.setInviteProgressAndReward(clientInviteProgressBoList);
//    }

//    /**
//     * 获取邀请进度和进度奖励
//     * @return
//     */
//    @GetMapping("/getInviteProgressAndReward")
//    @Operation(summary = "获取邀请进度和进度奖励")
//    public R<List<ClientInviteProgressInfoVo>> getInviteProgressAndReward() {
//
//        return this.clientInviteProgressLogic.getInviteProgressAndReward();
//    }

    /**
     * 客户端获取邀请进度和进度奖励
     * @return
     */
    @GetMapping("/clientGetInviteProgressAndReward")
    @Operation(summary = "客户端获取邀请进度和进度奖励")
    public R<List<ClientInviteProgressInfoVo>> clientGetInviteProgressAndReward() {

        return this.clientInviteProgressLogic.clientGetInviteProgressAndReward();
    }

    /**
     * 邀请进度列表
     * @param clientInviteProgressListBo 邀请进度列表查询参数
     * @return
     */
    @PostMapping("/list")
    @Operation(summary = "邀请进度列表")
    public R<PageUtils<ClientInviteProgressListVo>> list(@Parameter(description = "邀请进度列表查询参数", required = true) @RequestBody ClientInviteProgressListBo clientInviteProgressListBo){

        return clientInviteProgressLogic.queryPage(clientInviteProgressListBo);
    }


    /**
     * 邀请进度信息
     * @param id 邀请进度id
     * @return
     */
    @GetMapping("/info")
    @Operation(summary = "邀请进度信息")
    public R<ClientInviteProgressInfoVo> info(@Parameter(description = "邀请进度id", required = true) @RequestParam("id") Long id){

        return clientInviteProgressLogic.info(id);
    }

    /**
     * 新增邀请进度
     * @param clientInviteProgressBo 邀请进度对象
     * @return
     */
    @PostMapping("/save")
    @Operation(summary = "新增邀请进度")
    public R<String> save(@Parameter(description = "邀请进度对象", required = true) @RequestBody ClientInviteProgressBo clientInviteProgressBo){

        return clientInviteProgressLogic.save(clientInviteProgressBo);
    }

    /**
     * 修改邀请进度
     * @param clientInviteProgressBo 邀请进度对象
     * @return
     */
    @PostMapping("/update")
    @Operation(summary = "修改邀请进度")
    public R<String> update(@Parameter(description = "邀请进度对象", required = true) @RequestBody ClientInviteProgressBo clientInviteProgressBo){

        return clientInviteProgressLogic.update(clientInviteProgressBo);
    }

    /**
     * 删除邀请进度
     * @param id 邀请进度id
     * @return
     */
    @GetMapping("/delete")
    @Operation(summary = "删除邀请进度")
    public R<String> delete(@Parameter(description = "邀请进度id", required = true) @RequestParam("id") Long id){

        return clientInviteProgressLogic.delete(id);
    }

}
