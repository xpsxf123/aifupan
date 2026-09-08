package com.jiuyu.replay.api.controller.words;

import com.jiuyu.replay.api.logic.words.ChanmamaSendRecordLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bo.viewing.ChanmamaSendRecordBo;
import com.jiuyu.replay.words.bo.viewing.ChanmamaSendRecordListBo;
import com.jiuyu.replay.words.vo.viewing.ChanmamaSendRecordInfoVo;
import com.jiuyu.replay.words.vo.viewing.ChanmamaSendRecordListVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;




/**
 * 第三方数据平台发送记录
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-04-22 14:21:09
 */
@RestController
@CrossOrigin
@RequestMapping("replay/chanmamasendrecord")
@Tag(name = "第三方数据平台发送记录")
public class ChanmamaSendRecordController {

    @Resource
    private ChanmamaSendRecordLogic chanmamaSendRecordLogic;

    /**
     * 第三方数据平台发送记录列表
     * @param chanmamaSendRecordListBo 第三方数据平台发送记录列表查询参数
     * @return
     */
    @PostMapping("/list")
    @Operation(summary = "第三方数据平台发送记录列表")
    public R<PageUtils<ChanmamaSendRecordListVo>> list(@Parameter(description = "第三方数据平台发送记录列表查询参数", required = true) @RequestBody ChanmamaSendRecordListBo chanmamaSendRecordListBo){

        return chanmamaSendRecordLogic.queryPage(chanmamaSendRecordListBo);
    }


    /**
     * 第三方数据平台发送记录信息
     * @param id 第三方数据平台发送记录id
     * @return
     */
    @GetMapping("/info")
    @Operation(summary = "第三方数据平台发送记录信息")
    public R<ChanmamaSendRecordInfoVo> info(@Parameter(description = "第三方数据平台发送记录id", required = true) @RequestParam("id") Long id){

        return chanmamaSendRecordLogic.info(id);
    }

    /**
     * 新增第三方数据平台发送记录
     * @param chanmamaSendRecordBo 第三方数据平台发送记录对象
     * @return
     */
    @PostMapping("/save")
    @Operation(summary = "新增第三方数据平台发送记录")
    public R<String> save(@Parameter(description = "第三方数据平台发送记录对象", required = true) @RequestBody ChanmamaSendRecordBo chanmamaSendRecordBo){

        return chanmamaSendRecordLogic.save(chanmamaSendRecordBo);
    }

    /**
     * 修改第三方数据平台发送记录
     * @param chanmamaSendRecordBo 第三方数据平台发送记录对象
     * @return
     */
    @PostMapping("/update")
    @Operation(summary = "修改第三方数据平台发送记录")
    public R<String> update(@Parameter(description = "第三方数据平台发送记录对象", required = true) @RequestBody ChanmamaSendRecordBo chanmamaSendRecordBo){

        return chanmamaSendRecordLogic.update(chanmamaSendRecordBo);
    }

    /**
     * 删除第三方数据平台发送记录
     * @param id 第三方数据平台发送记录id
     * @return
     */
    @GetMapping("/delete")
    @Operation(summary = "删除第三方数据平台发送记录")
    public R<String> delete(@Parameter(description = "第三方数据平台发送记录id", required = true) @RequestParam("id") Long id){

        return chanmamaSendRecordLogic.delete(id);
    }

}
