package com.jiuyu.replay.ai.controller;

import com.jiuyu.replay.ai.bll.SceneSliceBll;
import com.jiuyu.replay.ai.bo.SaveSceneSliceBo;
import com.jiuyu.replay.ai.bo.SceneSliceSignUploadUrlBo;
import com.jiuyu.replay.ai.vo.SceneSliceStatusVo;
import com.jiuyu.replay.generic.vo.common.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 场景切片 Controller
 *
 * @author lj
 * @date 2026-07-06
 */
@RestController
@CrossOrigin
@AllArgsConstructor
@RequestMapping("replay/ai/sceneSlice")
@Tag(name = "场景切片")
public class SceneSliceController {

    private final SceneSliceBll sceneSliceBll;

    /**
     * 获取场景切片状态
     * <p>仅查询是否已有完成记录；已完成则返回AI分析结果，
     * 未完成则返回OSS上传URL和截取秒数（不创建数据库记录）。</p>
     */
    @PostMapping("/getStatus")
    @Operation(summary = "获取场景切片状态",
            description = "检查视频的场景切片状态。已完成返回AI结果，未完成返回OSS上传URL和截取秒数")
    public R<SceneSliceStatusVo> getStatus(
            @Validated @RequestBody SceneSliceSignUploadUrlBo bo) {
        return sceneSliceBll.getStatus(bo.getVideoId());
    }

    /**
     * 保存场景切片记录
     * <p>客户端上传截图到OSS后调用，创建记录并异步触发AI分析。</p>
     */
    @PostMapping("/save")
    @Operation(summary = "保存场景切片记录",
            description = "客户端上传截图到OSS后调用，创建数据库记录并触发AI分析")
    public R<String> save(
            @Validated @RequestBody SaveSceneSliceBo bo) {
        return sceneSliceBll.save(bo);
    }
}
