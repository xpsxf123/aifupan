package com.jiuyu.replay.api.controller.words;

import com.jiuyu.replay.api.logic.words.SensitiveWordsClientLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bo.SensitiveWordsClientBo;
import com.jiuyu.replay.words.bo.SensitiveWordsClientListBo;
import com.jiuyu.replay.words.vo.SensitiveWordsClientListVo;
import com.jiuyu.replay.words.vo.SensitiveWordsClientInfoVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 客户端自定义词语
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-06-20 16:22:18
 */
@RestController
@CrossOrigin
@RequestMapping("replay/sensitivewordsClient")
@Tag(name = "客户端自定义词语")
public class SensitiveWordsClientController {

    @Resource
    private SensitiveWordsClientLogic sensitiveWordsClientLogic;

    /**
     * 客户端自定义词语列表
     * @param sensitiveWordsListBo 客户端自定义词语列表查询参数
     * @return
     */
    @PostMapping("/list")
    @Operation(summary = "客户端自定义词语列表")
    public R<PageUtils<SensitiveWordsClientListVo>> list(@Parameter(description = "客户端自定义词语列表查询参数", required = true) @RequestBody SensitiveWordsClientListBo sensitiveWordsListBo){

        return sensitiveWordsClientLogic.queryPage(sensitiveWordsListBo);
    }


    /**
     * 获取客户端自定义词语信息
     * @param id 客户端自定义词语id
     * @return
     */
    @GetMapping("/info")
    @Operation(summary = "获取客户端自定义词语信息")
    public R<SensitiveWordsClientInfoVo> info(@Parameter(description = "客户端自定义词语id", required = true) @RequestParam("id") Long id){

        return sensitiveWordsClientLogic.info(id);
    }

    /**
     * 新增客户端自定义词语
     * @param sensitiveWordsBo 客户端自定义词语对象
     * @return
     */
    @PostMapping("/save")
    @Operation(summary = "新增客户端自定义词语")
    public R<List<String>> save(@Parameter(description = "客户端自定义词语对象", required = true) @RequestBody SensitiveWordsClientBo sensitiveWordsBo){

        return sensitiveWordsClientLogic.save(sensitiveWordsBo);
    }

    /**
     * 修改客户端自定义词语
     * @param sensitiveWordsBo 客户端自定义词语对象
     * @return
     */
    @PostMapping("/update")
    @Operation(summary = "修改客户端自定义词语")
    public R<String> update(@Parameter(description = "客户端自定义词语对象", required = true) @RequestBody SensitiveWordsClientBo sensitiveWordsBo){

        return sensitiveWordsClientLogic.update(sensitiveWordsBo);
    }

    /**
     * 删除客户端自定义词语
     * @param id 客户端自定义词语id
     * @return
     */
    @GetMapping("/delete")
    @Operation(summary = "删除客户端自定义词语")
    public R<String> delete(@Parameter(description = "客户端自定义词语id", required = true) @RequestParam("id") Long id){

        return sensitiveWordsClientLogic.delete(id);
    }

}
