package com.jiuyu.replay.api.logic.ai.impl;

import cn.hutool.core.util.StrUtil;
import com.jiuyu.replay.api.logic.ai.utils.CommonSentenceUtils;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.words.bo.AskRequestBo;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author ：lujie
 * description：数据看板的数据来源-文件的数据截图
 * date ：2025/4/18 下午5:27
 */
@Component
@Scope("prototype")
public class ViewingConfuseSentenceImpl extends VideoSentenceImpl{

    @Override
    public void init(String sourceId, Integer askType, List<Long> videoTimeOneList, List<Long> videoTimeTwoList) {
        super.init(sourceId, sourceType, askType, videoTimeOneList, videoTimeTwoList);

        super.getAnchorVideoInfoVo();
    }


    @Override
    public String getContent() {
        if (content != null) return this.content;
        String board1 = getBoard();
        RRException.isNotEmpty(board1, "看板数据为空不能发起ai提问");
        this.content = board1;
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
