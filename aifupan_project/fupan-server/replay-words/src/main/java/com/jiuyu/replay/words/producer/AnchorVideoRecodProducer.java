package com.jiuyu.replay.words.producer;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bo.AnchorVideoBo;
import com.jiuyu.replay.words.vo.AnchorVideoRecodListVo;
import com.jiuyu.replay.generic.vo.words.AnchorVideoVO;
import com.jiuyu.replay.words.vo.AudioAnalysisVo;

import java.util.List;

/**
 * @author tisheng
 * @date 2024/9/11
 * @apinNote
 */
public interface AnchorVideoRecodProducer {
    /**
     * 获取分析记录
     * @param anchorVideoBo
     * @return
     */
    R<PageUtils<AnchorVideoVO>> list(AnchorVideoBo anchorVideoBo);

    /**
     * 保存分析记录
     * @param AudioAnalysisVo
     * @return
     */
    R<String> save(List<AudioAnalysisVo> AudioAnalysisVo);

    /**
     * 服务端用户详情查询分析记录
     * @param anchorVideoBo
     * @return
     */
    R<PageUtils<AnchorVideoRecodListVo>> selectVideoRecod(AnchorVideoBo anchorVideoBo);

}
