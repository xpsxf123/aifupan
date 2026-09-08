package com.jiuyu.replay.words.controller;


import cn.hutool.core.util.StrUtil;
import com.jiuyu.replay.common.lock.DistributedLock;
import com.jiuyu.replay.common.vo.SignUploadUrlVo;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bll.VideoDataViewingBll;
import com.jiuyu.replay.words.bo.oceanEngine.OceanEngineDataBo;
import com.jiuyu.replay.words.constant.Constant;
import com.jiuyu.replay.words.vo.OceanEngineDataVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotBlank;
import org.apache.ibatis.annotations.Update;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


/**
 * 巨量引擎相关逻辑
 *
 * @author liaoxin
 * @date 2025-06-13
 */
@CrossOrigin
@RestController
@RequestMapping("replay/words/oceanEngineData")
@Tag(name = "巨量百应", description = "巨量百应")
public class OceanEngineDataController {

    @Resource
    private VideoDataViewingBll videoDataViewingBll;
    @Resource
    private DistributedLock distributedLock;

    /**
     * 巨量引擎数据更新
     *
     * @param bo 巨量引擎数据更新业务对象
     * @return R<Long> 更新结果，包含操作状态和相关ID
     */
    @Operation(summary = "巨量数据更新接口", description = "客户端进行巨量数据更新")
    @PostMapping("/updateOceanEngine")
    public R<Long> updateOceanEngine(@RequestBody @Validated(Update.class) OceanEngineDataBo bo) {

        if(StrUtil.isEmpty(bo.getVideoId())) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "视频id不能为空");
        }

        return distributedLock.executeWithLock("create_viewing_lock:" + bo.getVideoId(), () -> videoDataViewingBll.saveOrUpdateOceanEngine(bo));
    }

    /**
     * 巨量数据查询接口
     *
     * @param videoId 视频id
     * @return
     */
    @Operation(summary = "巨量数据查询接口", description = "根据视频ID查询巨量数据")
    @GetMapping("/getOceanEngine")
    @Parameters(value = {@Parameter(name = "videoId", in = ParameterIn.QUERY, description = "视频ID", required = true, schema = @Schema(type = "string"))})
    public R<OceanEngineDataVo> getOceanEngine(@RequestParam @NotBlank(message = "视频ID不能为空") @Schema(description = "视频Id") String videoId) {

        return videoDataViewingBll.getOceanEngineByVideoId(videoId);
    }

    /**
     * 获取巨量数据上传的预签名链接
     *
     * @param videoId 视频ID
     * @return R<SignUploadUrlVo> 预签名链接
     */
    @Operation(summary = "获取巨量数据上传的预签名链接")
    @GetMapping("/getSignUploadUrl")
    public R<SignUploadUrlVo> getSignUploadUrl(@RequestParam String videoId) {

        return videoDataViewingBll.getOceanEngineUploadUrlByVideoId(videoId);
    }


}