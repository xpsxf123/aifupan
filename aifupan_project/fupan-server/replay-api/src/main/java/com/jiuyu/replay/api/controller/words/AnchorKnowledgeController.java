package com.jiuyu.replay.api.controller.words;

import com.jiuyu.replay.api.logic.words.AnchorKnowledgeLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.power.vo.UserCacheVo;
import com.jiuyu.replay.power.utils.GlobalObject;
import com.jiuyu.replay.words.bo.AnchorKnowledgeListBo;
import com.jiuyu.replay.words.bo.AnchorKnowledgeSaveBo;
import com.jiuyu.replay.words.vo.AnchorKnowledgeListVo;
import com.jiuyu.replay.words.vo.AnchorKnowledgeVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

/**
 * 主播级别知识库
 *
 * @author jy
 * @date 2026-06-29
 */
@RestController
@CrossOrigin
@RequestMapping("replay/anchorKnowledge")
@Tag(name = "主播级别知识库")
public class AnchorKnowledgeController {

    @Resource
    private AnchorKnowledgeLogic anchorKnowledgeLogic;

    @PostMapping("/list")
    @Operation(summary = "知识库列表")
    public R<PageUtils<AnchorKnowledgeListVo>> list(@Parameter(description = "列表查询参数", required = true) @RequestBody AnchorKnowledgeListBo listBo) {
        UserCacheVo localUser = GlobalObject.getLocalUser();
        listBo.setUserId(localUser.getId());
        return anchorKnowledgeLogic.queryPage(listBo);
    }

    @GetMapping("/info")
    @Operation(summary = "知识库详情")
    public R<AnchorKnowledgeVo> info(@Parameter(description = "主播唯一标识", required = true) @RequestParam("secUid") String secUid) {
        UserCacheVo localUser = GlobalObject.getLocalUser();
        return anchorKnowledgeLogic.info(secUid, localUser.getId());
    }

    @PostMapping("/save")
    @Operation(summary = "新增知识库")
    public R<String> save(@Parameter(description = "知识库对象", required = true) @RequestBody AnchorKnowledgeSaveBo bo) {
        UserCacheVo localUser = GlobalObject.getLocalUser();
        bo.setUserId(localUser.getId());
        return anchorKnowledgeLogic.save(bo);
    }

    @PostMapping("/update")
    @Operation(summary = "修改知识库")
    public R<String> update(@Parameter(description = "知识库对象", required = true) @RequestBody AnchorKnowledgeSaveBo bo) {
        UserCacheVo localUser = GlobalObject.getLocalUser();
        bo.setUserId(localUser.getId());
        return anchorKnowledgeLogic.update(bo);
    }

    @GetMapping("/delete")
    @Operation(summary = "删除知识库")
    public R<String> delete(@Parameter(description = "主播唯一标识", required = true) @RequestParam("secUid") String secUid) {
        UserCacheVo localUser = GlobalObject.getLocalUser();
        return anchorKnowledgeLogic.delete(secUid, localUser.getId());
    }
}
