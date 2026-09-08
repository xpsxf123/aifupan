package com.jiuyu.replay.api.controller.openapi;

import cn.hutool.core.util.ObjectUtil;
import com.jiuyu.replay.api.logic.order.PackageLogic;
import com.jiuyu.replay.api.logic.third.AiRelatedLogic;
import com.jiuyu.replay.api.logic.words.AnchorVideoLogic;
import com.jiuyu.replay.api.logic.words.SensitiveWordsLogic;
import com.jiuyu.replay.api.logic.words.SocketCollectMessageLogic;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.order.vo.CurrentFreeVersionVo;
import com.jiuyu.replay.words.vo.anchor.HistoryBatchNumberVideoListVo;
import com.jiuyu.replay.words.vo.video.BarrageDataVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/4/18 下午8:17
 */
@RestController
@CrossOrigin
@RequestMapping("replay/openapi/v2400")
@Tag(name = "客户端openAPI-2.4.00")
@AllArgsConstructor
public class ClientOpenApi2400 {

    private final AnchorVideoLogic anchorVideoLogic;
    private final AiRelatedLogic aiRelatedLogic;
    private final PackageLogic packageLogic;
    private final SensitiveWordsLogic sensitiveWordsLogic;
    private final SocketCollectMessageLogic socketCollectMessageLogic;

    @GetMapping("/historyBatchNumberVideoList")
    @Operation(summary = "查询历史场次和视频")
    public R<List<HistoryBatchNumberVideoListVo>> historyBatchNumberVideoList(
            @Parameter(description = "视频id", required = true) String videoId,
            @Parameter(description = "类型 0截图 1看板 2正常", required = true) Integer dataType,
            @Parameter(description = "视频上传状态 0：未上传 1：已上传") Integer uploadStatus,
            @Parameter(description = "条数", required = true) Integer limit){
        if (ObjectUtil.isEmpty(uploadStatus)) uploadStatus = 0;
        return anchorVideoLogic.historyBatchNumberVideoList(videoId, dataType, uploadStatus, limit);
    }

    @GetMapping("/currentFreeVersion")
    @Operation(summary = "查询当前版的资源")
    public R<CurrentFreeVersionVo> currentFreeVersion(){
        return packageLogic.currentFreeVersion();
    }

    @GetMapping("/videoBarrageData")
    @Operation(summary = "查询视频对应的弹幕标注")
    public R<BarrageDataVo> videoBarrageData(@Parameter(description = "视频id", required = true) String videoId){
        return sensitiveWordsLogic.videoBarrageData(videoId);
    }
}
