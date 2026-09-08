package com.jiuyu.replay.api.controller.words;

import com.jiuyu.replay.api.logic.words.UserAnalysisRollupLogic;
import com.jiuyu.replay.api.task.UserRollupScheduledTasks;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bo.UserAnalysisRollupBo;
import com.jiuyu.replay.words.bo.UserAnalysisRollupListBo;
import com.jiuyu.replay.words.vo.UserAnalysisRollupInfoVo;
import com.jiuyu.replay.words.vo.UserAnalysisRollupListVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;



/**
 * 用户分析汇总表
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-01-19 16:56:41
 */
@RestController
@CrossOrigin
@RequestMapping("words/useranalysisrollup")
@Tag(name = "用户分析汇总表")
public class UserAnalysisRollupController {

    @Resource
    private UserAnalysisRollupLogic userAnalysisRollupLogic;
    @Resource
    private UserRollupScheduledTasks userRollupScheduledTasks;

    /**
     * 用户分析汇总表列表
     * @param userAnalysisRollupListBo 用户分析汇总表列表查询参数
     * @return
     */
    @PostMapping("/list")
    @Operation(summary = "用户分析汇总表列表")
    public R<PageUtils<UserAnalysisRollupListVo>> list(@Parameter(description = "用户分析汇总表列表查询参数", required = true) @RequestBody UserAnalysisRollupListBo userAnalysisRollupListBo){

        return userAnalysisRollupLogic.queryPage(userAnalysisRollupListBo);
    }


    /**
     * 用户分析汇总表信息
     * @param id 用户分析汇总表id
     * @return
     */
    @GetMapping("/info")
    @Operation(summary = "用户分析汇总表信息")
    public R<UserAnalysisRollupInfoVo> info(@Parameter(description = "用户分析汇总表id", required = true) @RequestParam("id") Long id){

        return userAnalysisRollupLogic.info(id);
    }

    /**
     * 新增用户分析汇总表
     * @param userAnalysisRollupBo 用户分析汇总表对象
     * @return
     */
    @PostMapping("/save")
    @Operation(summary = "新增用户分析汇总表")
    public R<String> save(@Parameter(description = "用户分析汇总表对象", required = true) @RequestBody UserAnalysisRollupBo userAnalysisRollupBo){

        return userAnalysisRollupLogic.save(userAnalysisRollupBo);
    }

    /**
     * 修改用户分析汇总表
     * @param userAnalysisRollupBo 用户分析汇总表对象
     * @return
     */
    @PostMapping("/update")
    @Operation(summary = "修改用户分析汇总表")
    public R<String> update(@Parameter(description = "用户分析汇总表对象", required = true) @RequestBody UserAnalysisRollupBo userAnalysisRollupBo){

        return userAnalysisRollupLogic.update(userAnalysisRollupBo);
    }

    /**
     * 删除用户分析汇总表
     * @param id 用户分析汇总表id
     * @return
     */
    @GetMapping("/delete")
    @Operation(summary = "删除用户分析汇总表")
    public R<String> delete(@Parameter(description = "用户分析汇总表id", required = true) @RequestParam("id") Long id){

        return userAnalysisRollupLogic.delete(id);
    }

    /**
     * 定时更新用户分析汇总数据
     */
    @GetMapping("/timingUpdateData")
    @Operation(summary = "定时更新用户分析汇总数据")
    public void timingUpdateData() throws ParseException {
        userRollupScheduledTasks.timingUpdateData();
    }
}