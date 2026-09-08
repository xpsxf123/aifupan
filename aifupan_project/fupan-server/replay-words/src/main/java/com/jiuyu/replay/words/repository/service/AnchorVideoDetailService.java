package com.jiuyu.replay.words.repository.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jiuyu.replay.generic.vo.words.AnchorVideoDetailVo;
import com.jiuyu.replay.generic.vo.words.anchor.AnchorUrlTradeVo;
import com.jiuyu.replay.words.entity.AnchorVideoDetailEntity;
import com.jiuyu.replay.generic.vo.words.VideoContentVo;

import java.util.List;


/**
 * 视频的详情
 *
 * @author lyw
 * @email 1798883178@qq.com
 * @date 2025-05-27 15:30:33
 */
public interface AnchorVideoDetailService extends IService<AnchorVideoDetailEntity> {


    List<VideoContentVo> selVideoContents(String videoId, Integer type,Integer sourceType,Long userId,Long tenantId);

    void inserto(AnchorVideoDetailEntity anchorVideoDetailEntity);

    void saveVideoContents(List<VideoContentVo> voList,Long userId,Long tenantId,String videoId,Integer sourceType, Integer type);


    /**
     * 获取视频已分析完成的需要生成的自然/优化原文的数据
     * @param limit
     * @return
     */
    List<AnchorVideoDetailVo> selectVideoDetailData( Integer limit);

    /**
     * 根据secUid集合获取主播行业
     *
     * @param secUids secUid集合
     * @return 行业
     */
    List<AnchorUrlTradeVo> tradeListBySecUids(List<String> secUids);
}

