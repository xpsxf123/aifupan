package com.jiuyu.replay.api.logic.ai.impl;

import cn.hutool.core.util.StrUtil;
import com.jiuyu.replay.api.logic.ai.utils.CommonSentenceUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.words.bo.AskRequestBo;
import com.jiuyu.replay.words.vo.file.UploadFileInfoVo;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author ：lujie
 * description：数据截图的数据来源-文件的数据截图
 * date ：2025/4/18 下午3:47
 */
@Component
@Scope("prototype")
public class ScreenshotFileSentenceImpl extends FileSentenceImpl {

    @Override
    public void init(String sourceId, Integer askType, List<Long> videoTimeOneList, List<Long> videoTimeTwoList) {
        super.init(sourceId, sourceType, askType, videoTimeOneList, videoTimeTwoList);

        R<UploadFileInfoVo> uploadFileInfoVoR = uploadFileBll.infoByFileId(sourceId);
        RRException.isNotEmpty(uploadFileInfoVoR, "获取文件信息失败");
        uploadFile = uploadFileInfoVoR.getData();
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
