package com.jiuyu.replay.api.controller.openapi;

import com.jiuyu.replay.api.logic.words.ThirdpartyLiveRecordLogic;
import com.jiuyu.replay.generic.bo.words.RandomLiveRecordBo;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.words.ThirdpartyLiveRecordVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 第三方开放接口 - 直播录制记录
 *
 * @author System
 * @date 2026-04-09
 */
@RestController
@CrossOrigin
@RequestMapping("replay/openapi/thirdparty")
@Tag(name = "第三方开放接口")
@AllArgsConstructor
public class ThirdpartyLiveRecordController {

    private final ThirdpartyLiveRecordLogic thirdpartyLiveRecordLogic;

    /**
     * 按行业随机获取直播录制记录
     *
     * @param bo 请求参数
     * @return 3-5条记录
     */
    @PostMapping("/randomLiveRecords")
    @Operation(summary = "按行业随机获取直播录制记录")
    public R<List<ThirdpartyLiveRecordVo>> randomLiveRecords(@RequestBody @Validated RandomLiveRecordBo bo) {
        return thirdpartyLiveRecordLogic.randomLiveRecords(bo.getTradeId());
    }
}
