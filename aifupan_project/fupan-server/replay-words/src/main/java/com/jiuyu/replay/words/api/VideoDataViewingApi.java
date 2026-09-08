package com.jiuyu.replay.words.api;

import com.jiuyu.replay.common.constant.AnchorVideoEnums;
import com.jiuyu.replay.generic.feign.words.VideoDataViewingFeign;
import com.jiuyu.replay.words.enums.DataViewingSourceTypeEnum;
import com.jiuyu.replay.words.producer.VideoDataViewingConfuseProducer;
import com.jiuyu.replay.words.rse.VideoDataViewingParagraphRse;
import com.jiuyu.replay.words.vo.viewing.VideoDataViewingConfuseInfoVo;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * @author ：lujie
 * @description：
 * @date ：2026/2/9 18:03
 */
@Service
@AllArgsConstructor
public class VideoDataViewingApi implements VideoDataViewingFeign {

    private VideoDataViewingConfuseProducer videoDataViewingConfuseProducer;
    private VideoDataViewingParagraphRse videoDataViewingParagraphRse;


    @Override
    public boolean hasBoard(String sourceId) {
        VideoDataViewingConfuseInfoVo videoDataViewingConfuseInfoVo = this.videoDataViewingConfuseProducer.infoByVideoIdAndUser(sourceId, null, null);
        if (videoDataViewingConfuseInfoVo != null) {
            if (Objects.equals(videoDataViewingConfuseInfoVo.getDataSourceType(), DataViewingSourceTypeEnum.JULIANGBAIYING.getType())) {
                // 获取本段视频看板数据
                VideoDataViewingConfuseInfoVo paragraphDataViewingInfoVo = this.videoDataViewingParagraphRse.infoByVideoId(sourceId);
                if (paragraphDataViewingInfoVo != null) {
                    videoDataViewingConfuseInfoVo = paragraphDataViewingInfoVo;
                }
            }
        }
        if (videoDataViewingConfuseInfoVo != null && videoDataViewingConfuseInfoVo.getDataStatus() != null) {
            int val = videoDataViewingConfuseInfoVo.getDataStatus() == AnchorVideoEnums.videoDataViewingStatus.SUCCESS.getCode()
                    || videoDataViewingConfuseInfoVo.getDataStatus() == AnchorVideoEnums.videoDataViewingStatus.SALES_DATA_IS_BEING_SUMMARIZED.getCode() ? 1 : 0;
            return val == 1;
        }
        return false;
    }
}
