package com.jiuyu.replay.api.logic.ai.impl;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author ：lujie
 * @description：对比分析的数据-数据截图
 * 要传一个额外的参数
 * singleMaxNum：每一个视频为多少字
 * @date ：2025/3/23 下午3:59
 */
@Component
@Scope("prototype")
public class ScreenshotContrastSentenceImpl extends SyncContrastSentenceImpl{

    @Override
    public void init(String sourceId, Integer askType, List<Long> videoTimeOneList, List<Long> videoTimeTwoList){
        super.init(sourceId, askType, videoTimeOneList, videoTimeTwoList);
    }


    @Override
    public String getTextParamsContent() {
        return "";
    }

    @Override
    public String getAllContent() {
        return getContent();
    }
}
