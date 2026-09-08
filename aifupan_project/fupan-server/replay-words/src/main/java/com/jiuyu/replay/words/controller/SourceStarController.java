package com.jiuyu.replay.words.controller;

import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bll.SourceStarBll;
import com.jiuyu.replay.words.bo.SourceStarAddBo;
import com.jiuyu.replay.words.bo.SourceStarCancelBo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

/**
 * 星标Controller
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2026-01-07
 */
@RestController
@RequestMapping("replay/sourceStar")
@Tag(name = "星标管理")
public class SourceStarController {

    @Resource
    private SourceStarBll sourceStarBll;

    /**
     * 添加星标
     *
     * @param addBo 添加星标参数
     * @return 操作结果
     */
    @PostMapping("/add")
    @Operation(summary = "添加星标")
    public R<String> add(@RequestBody SourceStarAddBo addBo) {

        return sourceStarBll.addStar(addBo);
    }

    /**
     * 取消星标
     *
     * @param cancelBo 取消星标参数
     * @return 操作结果
     */
    @PostMapping("/cancel")
    @Operation(summary = "取消星标")
    public R<String> cancel(@RequestBody SourceStarCancelBo cancelBo) {

        return sourceStarBll.cancelStar(cancelBo);
    }
}
