package com.jiuyu.replay.api.controller.words;

import com.jiuyu.replay.api.annotation.UserLock;
import com.jiuyu.replay.api.logic.words.OnlineNumLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bo.OnlineNumBo;
import com.jiuyu.replay.words.bo.OnlineNumListBo;
import com.jiuyu.replay.words.vo.OnlineNumInfoVo;
import com.jiuyu.replay.words.vo.OnlineNumListVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;


/**
 * 直播实时在线人数
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-11-14 10:05:41
 */
@RestController
@CrossOrigin
@RequestMapping("replay/onlinenum")
@Tag(name = "直播实时在线人数")
public class OnlineNumController {

    @Resource
    private OnlineNumLogic onlineNumLogic;

    /**
     * 直播实时在线人数列表
     * @param onlineNumListBo 直播实时在线人数列表查询参数
     * @return
     */
    @PostMapping("/list")
    @Operation(summary = "直播实时在线人数列表")
    public R<PageUtils<OnlineNumListVo>> list(@Parameter(description = "直播实时在线人数列表查询参数", required = true) @RequestBody OnlineNumListBo onlineNumListBo){

        return onlineNumLogic.queryPage(onlineNumListBo);
    }


    /**
     * 直播实时在线人数信息
     * @param id 直播实时在线人数id
     * @return
     */
    @GetMapping("/info")
    @Operation(summary = "直播实时在线人数信息")
    public R<OnlineNumInfoVo> info(@Parameter(description = "直播实时在线人数id", required = true) @RequestParam("id") Long id){

        return onlineNumLogic.info(id);
    }

    /**
     * 新增或修改直播实时在线人数
     * @param onlineNumBo 直播实时在线人数对象
     * @return
     */
    @PostMapping("/saveOrUpdate")
    @Operation(summary = "新增或修改直播实时在线人数")
    @UserLock(prefixKey = "user_lock:onlineNum")
    public R<String> saveOrUpdate(@Parameter(description = "直播实时在线人数对象", required = true) @RequestBody OnlineNumBo onlineNumBo) throws Exception {

        return onlineNumLogic.saveOrUpdate(onlineNumBo);
    }

    /**
     * 新增直播实时在线人数
     * @param onlineNumBo 直播实时在线人数对象
     * @return
     */
    @PostMapping("/save")
    @Operation(summary = "新增直播实时在线人数")
    public R<String> save(@Parameter(description = "直播实时在线人数对象", required = true) @RequestBody OnlineNumBo onlineNumBo){

        return onlineNumLogic.save(onlineNumBo);
    }

    /**
     * 修改直播实时在线人数
     * @param onlineNumBo 直播实时在线人数对象
     * @return
     */
    @PostMapping("/update")
    @Operation(summary = "修改直播实时在线人数")
    public R<String> update(@Parameter(description = "直播实时在线人数对象", required = true) @RequestBody OnlineNumBo onlineNumBo){

        return onlineNumLogic.update(onlineNumBo);
    }

    /**
     * 删除直播实时在线人数
     * @param id 直播实时在线人数id
     * @return
     */
    @GetMapping("/delete")
    @Operation(summary = "删除直播实时在线人数")
    public R<String> delete(@Parameter(description = "直播实时在线人数id", required = true) @RequestParam("id") Long id){

        return onlineNumLogic.delete(id);
    }

}
