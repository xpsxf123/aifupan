package com.jiuyu.replay.words.api;

import com.jiuyu.replay.generic.bo.words.GenerateVideoContentBo;
import com.jiuyu.replay.generic.feign.words.VideoContentFeign;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bll.AnchorVideoDetailBll;
import com.jiuyu.replay.words.bll.SensitiveWordsBll;
import com.jiuyu.replay.words.vo.AnalysisResultAllVo;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/6/12 下午2:51
 */
@Service
@AllArgsConstructor
public class VideoContentApi implements VideoContentFeign {

    private final SensitiveWordsBll sensitiveWordsBll;
    private final AnchorVideoDetailBll anchorVideoDetailBll;


    @Override
    public void generateVideoContent(GenerateVideoContentBo bo) {

        anchorVideoDetailBll.generateVideoContentStatus(bo);
    }
}
