package com.jiuyu.replay.words.bll;

import com.jiuyu.replay.generic.bo.words.video.SaveSliceCorrelationDataBo;
import com.jiuyu.replay.generic.bo.words.video.VideoSliceBo;
import com.jiuyu.replay.generic.enums.words.SliceSourceTypeEnum;
import com.jiuyu.replay.generic.feign.power.UserFeign;
import com.jiuyu.replay.generic.utils.ResultUtil;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.power.UserCacheVo;
import com.jiuyu.replay.generic.vo.video.VideoSliceVo;
import com.jiuyu.replay.generic.vo.words.AnchorVideoInfoVo;
import com.jiuyu.replay.words.bo.oceanEngine.OceanEngineProcessBo;
import com.jiuyu.replay.words.constant.Constant;
import com.jiuyu.replay.words.enums.DataViewingSourceTypeEnum;
import com.jiuyu.replay.words.producer.SocketCollectMessageProducer;
import com.jiuyu.replay.words.producer.UploadFileProducer;
import com.jiuyu.replay.words.producer.VideoDataViewingConfuseProducer;
import com.jiuyu.replay.words.rse.AnchorVideoRse;
import com.jiuyu.replay.words.rse.VideoDataViewingParagraphRse;
import com.jiuyu.replay.words.rse.VideoSliceRse;
import com.jiuyu.replay.words.vo.file.UploadFileInfoVo;
import com.jiuyu.replay.words.vo.viewing.VideoDataViewingConfuseInfoVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

@Component
public class VideoSliceBll {

    @Resource
    private VideoSliceRse videoSliceRse;
    @Resource
    private UserFeign userFeign;
    @Resource
    private AnchorVideoRse anchorVideoRse;
    @Resource
    private SocketCollectMessageProducer socketCollectMessageProducer;
    @Resource
    private VideoDataViewingConfuseProducer videoDataViewingConfuseProducer;
    @Resource
    private UploadFileProducer uploadFileProducer;
    @Resource
    private VideoDataViewingParagraphRse videoDataViewingParagraphRse;

    /**
     * 保存视频切片
     * @param videoSliceBo 视频切片参数
     * @return
     */
    public R<String> saveVideoSlice(VideoSliceBo videoSliceBo) {

        UserCacheVo userCacheVo = ResultUtil.getResult(userFeign.getLocalUser());
        if(userCacheVo == null) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "没有权限访问");
        }
        videoSliceBo.setUserId(userCacheVo.getId());
        videoSliceBo.setTenantId(userCacheVo.getActiveTenantId());

        if(Objects.equals(videoSliceBo.getSourceType(), SliceSourceTypeEnum.VIDEO_SLICE.getCode())) {
            AnchorVideoInfoVo anchorVideoInfoVo = this.anchorVideoRse.infoUserVideoByVideoId(videoSliceBo.getSourceId(), userCacheVo.getId());
            if(anchorVideoInfoVo == null) {
                return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "切片所属视频信息不存在");
            }
        }else if(Objects.equals(videoSliceBo.getSourceType(), SliceSourceTypeEnum.FILE_SLICE.getCode())) {
            UploadFileInfoVo uploadFileInfoVo = this.uploadFileProducer.clientGetFileByFileId(videoSliceBo.getSourceId(), videoSliceBo.getUserId(), videoSliceBo.getTenantId());
            if(uploadFileInfoVo == null) {
                return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "切片所属文件信息不存在");
            }
        }


        this.videoSliceRse.saveVideoSlice(videoSliceBo);
        return R.ok();
    }

    /**
     * 保存切片相关数据（websocket、数据看板等）
     * @param saveSliceCorrelationDataBo 参数
     * @return
     */
    public R<String> saveSliceCorrelationData(SaveSliceCorrelationDataBo saveSliceCorrelationDataBo) {

        UserCacheVo userCacheVo = ResultUtil.getResult(userFeign.getLocalUser());
        if(userCacheVo == null) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "没有权限访问");
        }
        saveSliceCorrelationDataBo.setUserId(userCacheVo.getId());
        saveSliceCorrelationDataBo.setTenantId(userCacheVo.getActiveTenantId());

        // 拷贝websocket数据
        this.socketCollectMessageProducer.copyVideoDataToSlice(saveSliceCorrelationDataBo.getSourceVideoId(), saveSliceCorrelationDataBo.getSliceVideoId(),
                saveSliceCorrelationDataBo.getSliceStartNaturalTime(), saveSliceCorrelationDataBo.getSliceEndNaturalTime(), userCacheVo.getActiveTenantId());

        // 拷贝数据看板数据
        VideoDataViewingConfuseInfoVo dataViewingConfuseInfoVo = this.videoDataViewingConfuseProducer.copyDataViewingToSlice(saveSliceCorrelationDataBo);

        // 保存本段录制视频的看板数据
        if(dataViewingConfuseInfoVo != null && dataViewingConfuseInfoVo.getDataSourceType().equals(DataViewingSourceTypeEnum.JULIANGBAIYING.getType())
            && StringUtils.hasLength(dataViewingConfuseInfoVo.getOssPath())) {
            // 获取过程实时数据
            List<OceanEngineProcessBo> realTimeDataList = videoDataViewingConfuseProducer.getOceanEngineFileData(dataViewingConfuseInfoVo.getVideoId(), dataViewingConfuseInfoVo.getOssPath());
            // 保存切片视频本段录制的看板数据
            this.videoDataViewingParagraphRse.saveSliceVideoParagraphData(saveSliceCorrelationDataBo, realTimeDataList, dataViewingConfuseInfoVo);
        }

        return R.ok();
    }

    /**
     * 修改视频切片
     *
     * @param videoSliceBo 视频切片参数
     *
     * @return
     */
    public R<String> updateVideoSlice(VideoSliceBo videoSliceBo) {

        this.videoSliceRse.updateVideoSlice(videoSliceBo);

        return R.ok();
    }

    public VideoSliceVo getVideoSliceBySourceId(String sourceId, Integer sourceType) {
        return this.videoSliceRse.getVideoSliceBySourceId(sourceId, sourceType);
    }
}
