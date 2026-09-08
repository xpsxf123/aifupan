package com.jiuyu.replay.third.controller;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.jiuyu.replay.generic.feign.words.SensitiveWordsFeign;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.words.AnalysisResultVo;
import com.jiuyu.replay.third.bll.TableStoreBll;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/6/16 下午8:31
 */
@RestController
@CrossOrigin
@RequestMapping("replay/third/tableStore")
@Tag(name = "表格存储相关的控制器")
@AllArgsConstructor
public class TableStoreController {

    private final TableStoreBll tableStoreBll;
    private final SensitiveWordsFeign sensitiveWordsFeign;

    @GetMapping("/getBarrageDataList")
    public R<List<Map<String, Object>>> getBarrageDataList(String videoId, @Parameter(description = "是否是福袋弹幕", required = false) Integer isBlessBag) {
        R<AnalysisResultVo> analysisData = sensitiveWordsFeign.getAnalysisData(0, videoId);

        if (ObjectUtil.isNotEmpty(analysisData.getData())) {
            return R.ok(tableStoreBll.getBarrageDataList(videoId, analysisData.getData().getSentenceMarkVos().stream().map(JSONUtil::toJsonStr).toList(), isBlessBag));
        }

        return R.ok(new ArrayList<>());
    }

}
