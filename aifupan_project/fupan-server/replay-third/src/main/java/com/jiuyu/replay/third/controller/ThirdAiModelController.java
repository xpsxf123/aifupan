package com.jiuyu.replay.third.controller;

import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.third.AiTempTokenVo;
import com.jiuyu.replay.third.bll.AiModelBll;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author ：lujie
 * &#064;description：ai模型相关的接口
 * @date ：2025/7/5 下午6:55
 */
@RestController
@RequestMapping("replay/third/aiModel")
@Tag(name = "表格存储相关的控制器")
@AllArgsConstructor
public class ThirdAiModelController {

    private final AiModelBll aiModelBll;

    @GetMapping("/getAnalysisTempToken")
    @Schema(description = "获取分析模型临时token")
    public R<AiTempTokenVo> getAnalysisTempToken(@Parameter(description = "ai模型 字典：client_ai_model的value") @RequestParam(required = false) Integer aiModel,
                                                 @Parameter(description = "ai模型code") @RequestParam(required = false) String code) {
        return R.ok(aiModelBll.getAnalysisTempToken(aiModel, code));
    }

}
