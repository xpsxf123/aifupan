package com.jiuyu.replay.words.controller;

import com.jiuyu.replay.generic.bo.words.ShareVideoCloudBo;
import com.jiuyu.replay.generic.bo.words.video.SubVideoListBo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.words.AnchorVideoInfoVo;
import com.jiuyu.replay.generic.vo.words.UserServiceDurationVo;
import com.jiuyu.replay.generic.vo.words.video.SubUserVideoListVo;
import com.jiuyu.replay.generic.vo.words.video.SubUserYesterdayNotesVideoListVo;
import com.jiuyu.replay.words.bll.AnchorVideoBll;
import com.jiuyu.replay.words.bo.video.ClientVideoListBo;
import com.jiuyu.replay.words.bo.video.GetVideoByNameBo;
import com.jiuyu.replay.words.bo.video.UpdateVideoCloudRenameBo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("anchorVideoControllerNew")
@RequestMapping("replay/anchorVideo")
@Tag(name = "主播视频")
public class AnchorVideoController {

    @Resource
    private AnchorVideoBll anchorVideoBll;


    /**
     * 分析视频到云空间
     *
     * @param shareVideoCloudBo 视频信息
     * @return 分享地址
     */
    @PostMapping("/shareVideoToCloud")
    @Operation(summary = "分析视频到云空间")
    public R<String> shareVideoToCloud(@Parameter(description = "视频信息", required = true) @RequestBody ShareVideoCloudBo shareVideoCloudBo) {

        return anchorVideoBll.shareVideoToCloud(shareVideoCloudBo);
    }

    /**
     * 获取子账号昨日录制视频列表
     * 
     * @param subVideoListBo 查询参数
     * @return 录制视频列表
     */
    @PostMapping("/getSubUserYesterdayVideoList")
    @Operation(summary = "获取子账号昨日录制视频列表")
    public R<PageUtils<SubUserVideoListVo>> getSubUserYesterdayVideoList(@RequestBody SubVideoListBo subVideoListBo) {

        return anchorVideoBll.getSubUserYesterdayVideoList(subVideoListBo);
    }

    /**
     * 获取子账号昨日有小结数据的视频列表
     *
     * @param subVideoListBo 查询参数
     * @return 录制视频列表
     */
    @PostMapping("/getSubUserYesterdayNotesVideoList")
    @Operation(summary = "获取子账号昨日有小结数据的视频列表")
    public R<PageUtils<SubUserYesterdayNotesVideoListVo>> getSubUserYesterdayNotesVideoList(@RequestBody SubVideoListBo subVideoListBo) {

        return anchorVideoBll.getSubUserYesterdayNotesVideoList(subVideoListBo);
    }

    /**
     * 批量将视频的本地视频删除标识改为已删除
     *
     * @param videoIds 视频videoId集合
     * @return 录制视频列表
     */
    @PostMapping("/deleteLocalVideoByIds")
    @Operation(summary = "批量将视频的本地视频删除标识改为已删除")
    public R<String> deleteLocalVideoByIds(@RequestBody List<String> videoIds) {

        return anchorVideoBll.deleteLocalVideoByIds(videoIds);
    }

    /**
     * 获取子账号视频列表
     * @param clientVideoListBo 查询参数
     * @return
     */
    @PostMapping("/getSubClientVideoList")
    @Operation(summary = "获取子账号视频列表")
    public R<PageUtils<SubUserVideoListVo>> getSubClientVideoList(@RequestBody ClientVideoListBo clientVideoListBo) {

        return anchorVideoBll.getSubClientVideoList(clientVideoListBo);
    }

    /**
     * 获取用户的服务时长和录制视频数
     * @return
     */
    @GetMapping("/getUserServiceDuration")
    @Operation(summary = "获取用户的服务时长和录制视频数")
    public R<UserServiceDurationVo> getUserServiceDuration() {

        return anchorVideoBll.getUserServiceDuration();
    }

    /**
     * 更新视频的云空间重命名
     *
     * @param updateVideoCloudRenameBo 重命名参数
     * @return 操作结果
     */
    @PostMapping("/updateCloudRename")
    @Operation(summary = "更新视频的云空间重命名")
    public R<String> updateCloudRename(@RequestBody UpdateVideoCloudRenameBo updateVideoCloudRenameBo) {

        return anchorVideoBll.updateCloudRename(updateVideoCloudRenameBo.getVideoId(), updateVideoCloudRenameBo.getCloudRename());
    }

    /**
     * 根据视频名称获取视频信息
     * @param getVideoByNameBo 视频名称入参
     * @return 视频信息
     */
    @PostMapping("/getByVideoName")
    @Operation(summary = "根据视频名称获取视频信息")
    public R<AnchorVideoInfoVo> getByVideoName(@RequestBody GetVideoByNameBo getVideoByNameBo) {
        return anchorVideoBll.getByVideoName(getVideoByNameBo.getVideoName());
    }

    /**
     * 推送治理视频
     *
     * @param videoId 视频id
     * @return 操作结果
     */
    @GetMapping("/pushGovernanceVideo")
    @Operation(summary = "推送治理视频")
    public R<String> pushGovernanceVideo(String videoId) {
        anchorVideoBll.pushGovernanceVideo(videoId);
        return R.ok();
    }

}
