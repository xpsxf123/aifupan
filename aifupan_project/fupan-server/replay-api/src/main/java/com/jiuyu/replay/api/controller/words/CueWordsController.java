package com.jiuyu.replay.api.controller.words;

import com.jiuyu.framework.function.complete.Complete;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.replay.api.logic.words.CueWordsLogic;
import com.jiuyu.replay.generic.bo.words.CueWordsBo;
import com.jiuyu.replay.generic.bo.words.CueWordsListBo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.words.CueWordsInfoVo;
import com.jiuyu.replay.generic.vo.words.CueWordsListVo;
import com.jiuyu.replay.power.bll.TenantBll;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 提示词
 *
 * @author DearSil
 * @email 2437495924@qq.com
 * @date 2025-02-20 15:23:28
 */
@RestController
@CrossOrigin
@RequestMapping("replay/cuewords")
@Tag(name = "提示词")
public class CueWordsController {

    @Resource
    private CueWordsLogic cueWordsLogic;

    @Resource
    private TenantBll tenantBll;

    /**
     * 提示词列表
     *
     * @param cueWordsListBo 提示词列表查询参数
     *
     * @return
     */
    @PostMapping("/list")
    @Operation(summary = "提示词列表")
    public R<PageUtils<CueWordsListVo>> list(@Parameter(description = "提示词列表查询参数", required = true) @RequestBody CueWordsListBo cueWordsListBo) {
        R<PageUtils<CueWordsListVo>> result = cueWordsLogic.queryPage(cueWordsListBo);
        if (EmptyUtil.isNotEmpty(result.getData()) && cueWordsListBo.getQueryType() == 1) {
            Complete.start(result.getData().getList())
                .build(tenantBll::getTenantAccountMap)
                .add(CueWordsListVo::getTenantId, (cueWordsListVo, tenantOptionVo) -> {
                    cueWordsListVo.setAccountName(tenantOptionVo.getAccountName());
                    cueWordsListVo.setAccountMobile(tenantOptionVo.getAccountMobile());
                }).then().over();
        }
        return result;
    }


    /**
     * 提示词信息
     *
     * @param id 提示词id
     *
     * @return
     */
    @GetMapping("/info")
    @Operation(summary = "提示词信息")
    public R<CueWordsInfoVo> info(@Parameter(description = "提示词id", required = true) @RequestParam("id") Long id) {
        R<CueWordsInfoVo> result = cueWordsLogic.info(id);
        if (EmptyUtil.isNotEmpty(result.getData())) {
            Complete.start(List.of(result.getData()))
                .build(tenantBll::getTenantAccountMap)
                .filter(word -> word.getTenantId() != null && word.getTenantId() > 0)
                .add(CueWordsInfoVo::getTenantId, (cueWordsInfoVo, tenantOptionVo) -> {
                    cueWordsInfoVo.setAccountName(tenantOptionVo.getAccountName());
                    cueWordsInfoVo.setAccountMobile(tenantOptionVo.getAccountMobile());
                }).then().over();
        }
        return result;
    }

    /**
     * 新增提示词
     *
     * @param cueWordsBo 提示词对象
     *
     * @return
     */
    @PostMapping("/save")
    @Operation(summary = "新增提示词")
    public R<String> save(@Parameter(description = "提示词对象", required = true) @RequestBody CueWordsBo cueWordsBo) {

        return cueWordsLogic.save(cueWordsBo);
    }

    /**
     * 修改提示词
     *
     * @param cueWordsBo 提示词对象
     *
     * @return
     */
    @PostMapping("/update")
    @Operation(summary = "修改提示词")
    public R<String> update(@Parameter(description = "提示词对象", required = true) @RequestBody CueWordsBo cueWordsBo) {

        return cueWordsLogic.update(cueWordsBo);
    }

    /**
     * 删除提示词
     *
     * @param id 提示词id
     *
     * @return
     */
    @GetMapping("/delete")
    @Operation(summary = "删除提示词")
    public R<String> delete(@Parameter(description = "提示词id", required = true) @RequestParam("id") Long id) {

        return cueWordsLogic.delete(id);
    }

}
