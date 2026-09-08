package com.jiuyu.replay.api.controller.words;

import com.jiuyu.replay.api.logic.words.IndustryKnowledgeLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bo.IndustryKnowledgeBo;
import com.jiuyu.replay.words.bo.IndustryKnowledgeListBo;
import com.jiuyu.replay.words.vo.IndustryKnowledgeListVo;
import com.jiuyu.replay.words.vo.IndustryKnowledgeVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

/**
 * 系统行业知识库
 *
 * @author jxy
 * @date 2024-06-24
 */
@RestController
@CrossOrigin
@RequestMapping("replay/industryKnowledge")
@Tag(name = "系统行业知识库")
public class IndustryKnowledgeController {

    @Resource
    private IndustryKnowledgeLogic industryKnowledgeLogic;

    /**
     * 知识库列表
     *
     * @param listBo 列表查询参数
     * @return
     */
    @PostMapping("/list")
    @Operation(summary = "知识库列表")
    public R<PageUtils<IndustryKnowledgeListVo>> list(@Parameter(description = "列表查询参数", required = true) @RequestBody IndustryKnowledgeListBo listBo) {
        return industryKnowledgeLogic.queryPage(listBo);
    }

    /**
     * 知识库信息
     *
     * @param id 知识库id
     * @return
     */
    @GetMapping("/info")
    @Operation(summary = "知识库信息")
    public R<IndustryKnowledgeVo> info(@Parameter(description = "知识库id", required = true) @RequestParam("id") Long id) {
        return industryKnowledgeLogic.info(id);
    }

    /**
     * 新增知识库
     *
     * @param bo 知识库对象
     * @return
     */
    @PostMapping("/save")
    @Operation(summary = "新增知识库")
    public R<String> save(@Parameter(description = "知识库对象", required = true) @RequestBody IndustryKnowledgeBo bo) {
        return industryKnowledgeLogic.save(bo);
    }

    /**
     * 修改知识库
     *
     * @param bo 知识库对象
     * @return
     */
    @PostMapping("/update")
    @Operation(summary = "修改知识库")
    public R<String> update(@Parameter(description = "知识库对象", required = true) @RequestBody IndustryKnowledgeBo bo) {
        return industryKnowledgeLogic.update(bo);
    }

    /**
     * 删除知识库
     *
     * @param id 知识库id
     * @return
     */
    @GetMapping("/delete")
    @Operation(summary = "删除知识库")
    public R<String> delete(@Parameter(description = "知识库id", required = true) @RequestParam("id") Long id) {
        return industryKnowledgeLogic.delete(id);
    }

}
