package com.jiuyu.replay.api.controller.words;

import com.jiuyu.replay.api.annotation.UserLock;
import com.jiuyu.replay.api.logic.words.TotalOnlineNumLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bo.TotalOnlineNumBo;
import com.jiuyu.replay.words.bo.TotalOnlineNumListBo;
import com.jiuyu.replay.words.vo.TotalOnlineNumInfoVo;
import com.jiuyu.replay.words.vo.TotalOnlineNumListVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;



/**
 * 直播总观看人次
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-11-14 10:05:41
 */
@RestController
@CrossOrigin
@RequestMapping("replay/totalonlinenum")
@Tag(name = "直播总观看人次")
public class TotalOnlineNumController {

    @Resource
    private TotalOnlineNumLogic totalOnlineNumLogic;

    /**
     * 直播总观看人次列表
     * @param totalOnlineNumListBo 直播总观看人次列表查询参数
     * @return
     */
    @PostMapping("/list")
    @Operation(summary = "直播总观看人次列表")
    public R<PageUtils<TotalOnlineNumListVo>> list(@Parameter(description = "直播总观看人次列表查询参数", required = true) @RequestBody TotalOnlineNumListBo totalOnlineNumListBo){

        return totalOnlineNumLogic.queryPage(totalOnlineNumListBo);
    }


    /**
     * 直播总观看人次信息
     * @param id 直播总观看人次id
     * @return
     */
    @GetMapping("/info")
    @Operation(summary = "直播总观看人次信息")
    public R<TotalOnlineNumInfoVo> info(@Parameter(description = "直播总观看人次id", required = true) @RequestParam("id") Long id){

        return totalOnlineNumLogic.info(id);
    }

    /**
     * 新增直播总观看人次
     * @param totalOnlineNumBo 直播总观看人次对象
     * @return
     */
    @PostMapping("/save")
    @Operation(summary = "新增直播总观看人次")
    public R<String> save(@Parameter(description = "直播总观看人次对象", required = true) @RequestBody TotalOnlineNumBo totalOnlineNumBo){

        return totalOnlineNumLogic.save(totalOnlineNumBo);
    }

    /**
     * 新增或修改直播总观看人次
     * @param totalOnlineNumBo 直播总观看人次对象
     * @return
     */
    @PostMapping("/saveOrUpdate")
    @Operation(summary = "新增或修改直播总观看人次")
    @UserLock(prefixKey = "user_lock:onlineNum")
    public R<String> saveOrUpdate(@Parameter(description = "直播总观看人次对象", required = true) @RequestBody TotalOnlineNumBo totalOnlineNumBo){

        return totalOnlineNumLogic.saveOrUpdate(totalOnlineNumBo);
    }

    /**
     * 修改直播总观看人次
     * @param totalOnlineNumBo 直播总观看人次对象
     * @return
     */
    @PostMapping("/update")
    @Operation(summary = "修改直播总观看人次")
    public R<String> update(@Parameter(description = "直播总观看人次对象", required = true) @RequestBody TotalOnlineNumBo totalOnlineNumBo){

        return totalOnlineNumLogic.update(totalOnlineNumBo);
    }

    /**
     * 删除直播总观看人次
     * @param id 直播总观看人次id
     * @return
     */
    @GetMapping("/delete")
    @Operation(summary = "删除直播总观看人次")
    public R<String> delete(@Parameter(description = "直播总观看人次id", required = true) @RequestParam("id") Long id){

        return totalOnlineNumLogic.delete(id);
    }

}
