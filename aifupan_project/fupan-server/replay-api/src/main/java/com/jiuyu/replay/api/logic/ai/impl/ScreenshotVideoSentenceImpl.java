package com.jiuyu.replay.api.logic.ai.impl;

import cn.hutool.core.util.StrUtil;
import com.jiuyu.replay.api.logic.ai.utils.CommonSentenceUtils;
import com.jiuyu.replay.words.bo.AskRequestBo;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author ：lujie
 * @description：数据截图的数据来源-视频的数据截图
 * @date ：2025/4/18 下午3:47
 */
@Component
@Scope("prototype")
public class ScreenshotVideoSentenceImpl extends VideoSentenceImpl{

    @Override
    public void init(String sourceId, Integer askType, List<Long> videoTimeOneList, List<Long> videoTimeTwoList) {
        super.init(sourceId, sourceType, askType, videoTimeOneList, videoTimeTwoList);

        super.getAnchorVideoInfoVo();
    }


    @Override
    public String getContent() {
        if (content != null) return this.content;
        this.content = getDataScreenshot();
        return this.content;
    }

    @Override
    public String getTextParamsContent() {
        return "";
    }

    @Override
    public String getAllContent() {
        return StrUtil.format("全场直播相关数据全文：\n{}", getContent());
    }

    @Override
    public String setAskQuestion(AskRequestBo askRequestBo) {
        return CommonSentenceUtils.setAskScreenshotQuestion(askRequestBo, historyParagraphBll, this);
    }





}
