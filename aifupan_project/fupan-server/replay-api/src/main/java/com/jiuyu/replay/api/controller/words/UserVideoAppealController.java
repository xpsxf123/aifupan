package com.jiuyu.replay.api.controller.words;

import com.jiuyu.replay.api.logic.words.UserVideoAppealLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bo.UserVideoAppealBo;
import com.jiuyu.replay.words.bo.UserVideoAppealListBo;
import com.jiuyu.replay.words.vo.UserVideoAppealInfoVo;
import com.jiuyu.replay.words.vo.UserVideoAppealListVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

/**
 * 用户视频申述表
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-11-14 12:12:11
 */
@RestController
@CrossOrigin
@RequestMapping("replay/uservideoappeal")
@Tag(name = "用户视频申述表")
public class UserVideoAppealController {

    @Resource
    private UserVideoAppealLogic userVideoAppealLogic;

    /**
     * 用户视频申述表列表
     * @param userVideoAppealListBo 用户视频申述表列表查询参数
     * @return
     */
    @PostMapping("/list")
    @Operation(summary = "用户视频申述表列表")
    public R<PageUtils<UserVideoAppealListVo>> list(@Parameter(description = "用户视频申述表列表查询参数", required = true) @RequestBody UserVideoAppealListBo userVideoAppealListBo){

        return userVideoAppealLogic.queryPage(userVideoAppealListBo);
    }


    /**
     * 用户视频申述表信息
     * @param id 用户视频申述表id
     * @return
     */
    @GetMapping("/info")
    @Operation(summary = "用户视频申述表信息")
    public R<UserVideoAppealInfoVo> info(@Parameter(description = "用户视频申述表id", required = true) @RequestParam("id") Long id){

        return userVideoAppealLogic.info(id);
    }

    /**
     * 新增用户视频申述表
     * @param userVideoAppealBo 用户视频申述表对象
     * @return
     */
    @PostMapping("/save")
    @Operation(summary = "新增用户视频申述表")
    public R<String> save(@Parameter(description = "用户视频申述表对象", required = true) @RequestBody UserVideoAppealBo userVideoAppealBo){

        return userVideoAppealLogic.save(userVideoAppealBo);
    }

    /**
     * 修改用户视频申述表
     * @param userVideoAppealBo 用户视频申述表对象
     * @return
     */
    @PostMapping("/update")
    @Operation(summary = "修改用户视频申述表")
    public R<String> update(@Parameter(description = "用户视频申述表对象", required = true) @RequestBody UserVideoAppealBo userVideoAppealBo){

        return userVideoAppealLogic.update(userVideoAppealBo);
    }

    /**
     * 删除用户视频申述表
     * @param id 用户视频申述表id
     * @return
     */
    @GetMapping("/delete")
    @Operation(summary = "删除用户视频申述表")
    public R<String> delete(@Parameter(description = "用户视频申述表id", required = true) @RequestParam("id") Long id){

        return userVideoAppealLogic.delete(id);
    }

    @PostMapping("/handleAppeal")
    @Operation(summary = "处理申述")
    public R<String> handleAppeal(@Parameter(description = "用户视频申述表对象", required = true) @RequestBody UserVideoAppealBo userVideoAppealBo){
        return userVideoAppealLogic.handleAppeal(userVideoAppealBo);
    }

}
