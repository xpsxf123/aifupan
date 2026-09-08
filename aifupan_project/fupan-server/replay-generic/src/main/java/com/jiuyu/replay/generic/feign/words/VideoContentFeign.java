package com.jiuyu.replay.generic.feign.words;

import com.jiuyu.replay.generic.bo.words.GenerateVideoContentBo;

/**
 * 自然、优化原文
 */
public interface VideoContentFeign {

    void generateVideoContent(GenerateVideoContentBo bo);
}
