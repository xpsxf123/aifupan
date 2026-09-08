package com.jiuyu.replay.words.controller;

import com.jiuyu.replay.generic.feign.power.UserFeign;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.power.UserCacheVo;
import com.jiuyu.replay.generic.vo.words.CueWordsListVo;
import com.jiuyu.replay.generic.vo.words.CustomizeCueWordsResponse;
import com.jiuyu.replay.words.api.CueWordsApi;
import com.jiuyu.replay.words.bll.CueWordsBll;
import com.jiuyu.replay.generic.bo.words.cue.CueWordsPageBo;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author ：lujie
 * @description：
 * @date ：2026/1/9 16:04
 */
@RestController
@RequestMapping("replay/words/cueWords")
@Tag(name = "提示词管理")
@RequiredArgsConstructor
public class WordsCueWordsController {

    private final CueWordsBll cueWordsBll;
    private final CueWordsApi cueWordsApi;
    private final UserFeign userFeign;


    /**
     * 纯平台级提示词列表
     *
     * @param pageBo bo页
     *
     * @return {@link R }<{@link PageUtils }<{@link CueWordsListVo }>>
     */
    @PostMapping("/pageCueWords")
    @Schema(description = "分页获取提示词")
    public R<PageUtils<CueWordsListVo>> pageCueWords(@RequestBody @Validated CueWordsPageBo pageBo) {
        CueWordsPageBo newCueWordsPageBo = new CueWordsPageBo();
        newCueWordsPageBo.setSourceId(pageBo.getSourceId());
        newCueWordsPageBo.setSourceType(pageBo.getSourceType());
        newCueWordsPageBo.setCueType(pageBo.getCueType());
        newCueWordsPageBo.setPage(pageBo.getPage());
        newCueWordsPageBo.setLimit(pageBo.getLimit());
        newCueWordsPageBo.setTenantId(null);
        // 设置参数
        cueWordsApi.setTradeCueWordsParams(newCueWordsPageBo);
        return R.ok(cueWordsBll.pageCueWords(newCueWordsPageBo));
    }

    /**
     * 定制提示词 + 平台级提示词混合列表
     *
     * @param pageBo bo页
     *
     * @return {@link R }<{@link CustomizeCueWordsResponse }>
     */
    @PostMapping("/customize-list")
    @Schema(description = "定制提示词 + 平台级提示词混合列表")
    public R<CustomizeCueWordsResponse> customizeList(@RequestBody @Validated CueWordsPageBo pageBo) {
        CueWordsPageBo newCueWordsPageBo = new CueWordsPageBo();
        newCueWordsPageBo.setSourceId(pageBo.getSourceId());
        newCueWordsPageBo.setSourceType(pageBo.getSourceType());
        newCueWordsPageBo.setCueType(pageBo.getCueType());
        newCueWordsPageBo.setPage(pageBo.getPage());
        newCueWordsPageBo.setLimit(pageBo.getLimit());
        newCueWordsPageBo.setTenantId(null);
        R<UserCacheVo> result = userFeign.getLocalUser();
        if (result.getData() != null) {
            newCueWordsPageBo.setTenantId(result.getData().getActiveTenantId());
        }
        // 设置参数
        cueWordsApi.setTradeCueWordsParams(newCueWordsPageBo);
        return R.ok(cueWordsBll.customizeCueWordsList(newCueWordsPageBo));
    }


}
