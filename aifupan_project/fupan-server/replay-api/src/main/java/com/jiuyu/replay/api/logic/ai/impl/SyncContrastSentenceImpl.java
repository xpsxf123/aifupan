package com.jiuyu.replay.api.logic.ai.impl;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import com.jiuyu.replay.api.logic.ai.SentenceMark;
import com.jiuyu.replay.api.logic.ai.factory.AiFactoryUtils;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bll.SyncContrastBll;
import com.jiuyu.replay.words.bo.AskRequestBo;
import com.jiuyu.replay.words.vo.SyncContrastInfoVo;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author ：lujie
 * @description：对比分析的数据
 * 要传一个额外的参数
 * singleMaxNum：每一个视频为多少字
 * @date ：2025/3/23 下午3:59
 */
@Component
@Scope("prototype")
public class SyncContrastSentenceImpl extends DefaultSentenceMarkImpl{

    @Resource
    private SyncContrastBll syncContrastBll;

    public String content = null;
    public String textParamsContent = null;
    public SyncContrastInfoVo syncContrast;
    public SentenceMark sentenceMark1;
    public SentenceMark sentenceMark2;

    @Override
    public void init(String sourceId, Integer askType, List<Long> videoTimeOneList, List<Long> videoTimeTwoList){
        super.init(sourceId, sourceType, askType, videoTimeOneList, videoTimeTwoList);
        this.sourceType = 2;

        R<SyncContrastInfoVo> syncContrastInfoVoR = syncContrastBll.infoByContrastId(sourceId);
        if(syncContrastInfoVoR.getCode() == 0 && syncContrastInfoVoR.getData() != null) {
            syncContrast = syncContrastInfoVoR.getData();
            if (ObjectUtil.isNotEmpty(syncContrast.getVideoOneId())){
                // 视频
                sentenceMark1 = AiFactoryUtils.getSentenceMark(0, askType);
                sentenceMark1.init(syncContrast.getVideoOneId(), askType, videoTimeOneList, null);
                sentenceMark2 = AiFactoryUtils.getSentenceMark(0, askType);
                sentenceMark2.init(syncContrast.getVideoTwoId(), askType, videoTimeTwoList, null);
            }else{
                // 文件
                sentenceMark1 = AiFactoryUtils.getSentenceMark(1, askType);
                sentenceMark1.init(syncContrast.getFileOneId(), askType, videoTimeOneList, null);
                sentenceMark2 = AiFactoryUtils.getSentenceMark(1, askType);
                sentenceMark2.init(syncContrast.getFileTwoId(), askType, videoTimeTwoList, null);
            }
        }
    }

    @Override
    public void setOtherParams(Map<String, Object> otherParams) {
        super.setOtherParams(otherParams);
        if (otherParams.containsKey("singleMaxNum")){
            if (otherParams.get("singleMaxNum") instanceof Integer){
                int singleMaxNum = (int) otherParams.get("singleMaxNum");
                if (singleMaxNum == 0 || singleMaxNum < -1){
                    RRException.create("提示词超出字数限制");
                }
                HashMap<String, Object> otherParams1 = new HashMap<>();
                Object o = otherParams.get("singleMaxNum");
                RRException.isNotEmpty(o, "singleMaxNum不能为空");
                int maxNum = singleMaxNum / 2;
                otherParams1.put("singleMaxNum", maxNum);
                sentenceMark1.setOtherParams(otherParams1);
                sentenceMark2.setOtherParams(otherParams1);
            }
            else {
                throw new RRException("singleMaxNum参数错误");
            }
        }
    }

    @Override
    public String getContent() {
        if (content != null) return this.content;
        Object o = otherParams.get("singleMaxNum");
        RRException.isNotEmpty(o, "singleMaxNum不能为空");
        int singleMaxNum = Integer.parseInt(o.toString());
        int maxNum = singleMaxNum / 2;
        if (ObjectUtil.isNotEmpty(sentenceMark1) && ObjectUtil.isNotEmpty(sentenceMark2)){
            String content1 = "视频1:\n" + sentenceMark1.getContent();
            String content2 = "视频2:\n" + sentenceMark2.getContent();
            if(singleMaxNum != -1 && content1.length() > maxNum) content1 = content1.substring(0, maxNum);
            if(singleMaxNum != -1 && content2.length() > maxNum) content2 = content2.substring(0, maxNum);
            content = StrUtil.format("{}\n\n\n{}", content1, content2);
            return content;
        }
        RRException.create(7005, "获取内容为空");
        return "";
    }

    @Override
    public String getTextParamsContent() {
        if (textParamsContent != null) return textParamsContent;

        Map<String, Object> dataDisplay = (Map<String, Object>) otherParams.get("aiAssistantDisplay");
        if (ObjectUtil.isEmpty(dataDisplay)) dataDisplay = new HashMap<>();
        boolean natureTimeFlag = ((Integer) dataDisplay.getOrDefault("natureTime", 1)) == 1;
        boolean startTimeFlag = ((Integer) dataDisplay.getOrDefault("startTime", 1)) == 1;
        boolean onlineNumFlag = ((Integer) dataDisplay.getOrDefault("onlineNum", 1)) == 1;
        boolean analysisCharFlag = ((Integer) dataDisplay.getOrDefault("analysisChar", 1)) == 1;
        boolean barrageNumFlag = ((Integer) dataDisplay.getOrDefault("barrageNum", 0)) == 1;
        boolean dealNumFlag = ((Integer) dataDisplay.getOrDefault("dealNum", 0)) == 1;
        boolean interactionRateFlag = ((Integer) dataDisplay.getOrDefault("interactionRate", 0)) == 1;
        boolean dealRateFlag = ((Integer) dataDisplay.getOrDefault("dealRate", 0)) == 1;
        boolean salesFlag = ((Integer) dataDisplay.getOrDefault("sales", 0)) == 1;
        boolean uvFlag = ((Integer) dataDisplay.getOrDefault("uv", 0)) == 1;

        String str = "";
        if (ObjectUtil.isNotEmpty(videoTimeOneList) || ObjectUtil.isNotEmpty(videoTimeTwoList)) {
            StringBuilder descSb = new StringBuilder();
            if (natureTimeFlag) {
                descSb.append(StrUtil.format("\n本段自然时间：指的是转译成的文字段落开始时对应的视频播放时间。"));
            }
            if (startTimeFlag) {
                descSb.append(StrUtil.format("\n本段开始时间：指的是转译成的文字段落开始时对应的视频播放时间。"));
                descSb.append(StrUtil.format("\n本段结束时间：指的是转译成的文字段落结束时对应的视频播放时间。"));
            }
            if (onlineNumFlag) {
                descSb.append(StrUtil.format("\n本段在线人数：指的是转译成的文字段落开始时对应的直播间在线人数。"));
            }
            if (analysisCharFlag) {
                descSb.append(StrUtil.format("\n本段语速：指的是1分钟内说的字数。"));
            }
            if (barrageNumFlag) {
                descSb.append(StrUtil.format("\n本段发弹幕条数：指的是转译成的文字段落开始时间到结束时间内直播间发送弹幕数量。"));
            }
            if (dealNumFlag) {
                descSb.append(StrUtil.format("\n本段成交人数：指的是转译成的文字段落开始时间到结束时间内商品的成交人数。"));
            }
            if (interactionRateFlag) {
                descSb.append(StrUtil.format("\n本段互动率：指的是转译成的文字段落开始时间到结束时间内弹幕数量/在线人数得出的互动率。"));
            }
            if (dealRateFlag) {
                descSb.append(StrUtil.format("\n本段成交率：指的是转译成的文字段落开始时间到结束时间内成交人数/在线人数得出的成交率。"));
            }
            if (salesFlag) {
                descSb.append(StrUtil.format("\n本段销售额：指的是转译成的文字段落开始时间到结束时间内的销售额。"));
            }
            if (uvFlag) {
                descSb.append(StrUtil.format("\n本段UV价值：指的是转译成的文字段落开始时间到结束时间内销售额/在线人数得出的UV价值。"));
            }
            if (natureTimeFlag) {
                descSb.append(StrUtil.format("\n从第二段开始直到最后一段，每一段的本段自然时间是上一段的自然结束时间。"));
            }
            if (descSb.length() > 0) {
                str += StrUtil.format("\n文本内的参数说明：") + descSb.toString();
            }
            if (ObjectUtil.isNotEmpty(videoTimeOneList)){
                long startTime = videoTimeOneList.get(0);
                long endTime = videoTimeOneList.get(1);
                String startStr = DateUtil.format(new DateTime(startTime).offset(DateField.HOUR, -8), "HH:mm:ss");
                String endStr = DateUtil.format(new DateTime(endTime).offset(DateField.HOUR, -8), "HH:mm:ss");
                str += StrUtil.format("\n视频1的时间是{}到{}，共{}秒的直播 \n\n", startStr, endStr, (endTime  - startTime) / 1000);
            }
            if (ObjectUtil.isNotEmpty(videoTimeTwoList)){
                long start2 = videoTimeTwoList.get(0);
                long end2 = videoTimeTwoList.get(1);
                String startStr2 = DateUtil.format(new DateTime(start2).offset(DateField.HOUR, -8), "HH:mm:ss");
                String sendStr2 = DateUtil.format(new DateTime(end2).offset(DateField.HOUR, -8), "HH:mm:ss");
                str += StrUtil.format("\n视频2的时间是{}到{}，共{}秒的直播 \n\n", startStr2, sendStr2, (end2  - start2) / 1000);
            }
        }
        str += StrUtil.format("\n\n以下是由两个直播间录屏成视频后转译成文字的直播全文脚本，从第一段到最后一段，每一段的内容都是连续的。");
        textParamsContent = str;
        return textParamsContent;

    }


    @Override
    public String getTradeName() {
        String str = "";
        if (ObjectUtil.isNotEmpty(sentenceMark1)){
            str += "视频1的行业为" + sentenceMark1.getTradeName();
        }
        if (ObjectUtil.isNotEmpty(sentenceMark2)){
            str += "视频2的行业为" + sentenceMark2.getTradeName();
        }
        return str;
    }

    @Override
    public String getDataScreenshot() {
        if (ObjectUtil.isNotEmpty(dataScreenshot)) return dataScreenshot;
        StringBuilder str = new StringBuilder();
        if (sentenceMark1 != null){
            str.append("视频1相关的数据：\n").append(sentenceMark1.getDataScreenshot());
        }
        if (sentenceMark2 != null){
            if (StrUtil.isNotEmpty(str.toString())) str.append("\n\n");
            str.append("视频2相关的数据：\n").append(sentenceMark2.getDataScreenshot());
        }
        this.dataScreenshot = str.toString();
        return this.dataScreenshot;
    }

    @Override
    public String getBoard() {
        if (ObjectUtil.isNotEmpty(board)) return board;
        StringBuilder str = new StringBuilder();
        if (sentenceMark1 != null) {
            str.append("视频1相关的数据：\n").append(sentenceMark1.getBoard());
        }
        if (sentenceMark2 != null) {
            if (StrUtil.isNotEmpty(str.toString())) str.append("\n\n");
            str.append("视频2相关的数据：\n").append(sentenceMark2.getBoard());
        }
        this.board = str.toString();
        return this.board;
    }

    @Override
    public String getPlatform() {
        String platform1 = "抖音";
        if (ObjectUtil.isNotEmpty(sentenceMark1)) {
            platform1 = sentenceMark1.getPlatform();
        }
        String platform2 = "抖音";
        if (ObjectUtil.isNotEmpty(sentenceMark2)) {
            platform2 = sentenceMark2.getPlatform();
        }
        if (platform1.equals(platform2)) {
            return "两个视频的平台都是" + platform1;
        } else {
            return StrUtil.format("视频1的平台为{},视频2的平台为{}", platform1, platform2);
        }
    }

    @Override
    public void setSpeed(AskRequestBo askRequestBo) {
        if (ObjectUtil.isEmpty(askRequestBo.getOtherObj())) {
            return;
        }
        if (sentenceMark1 != null) {
            Integer speed = sentenceMark1.getSpeed(askRequestBo);
            Object backgroundConfig = askRequestBo.getOtherObj().getOrDefault("backgroundConfigOne", null);
            // 判断是否是map<string,object>类型的
            if (backgroundConfig instanceof Map) {
                Map<String, Object> backgroundConfigMap = (Map<String, Object>) backgroundConfig;
                backgroundConfigMap.put("speechRate", speed);
            }
        }
        if (sentenceMark2 != null) {
            Integer speed = sentenceMark2.getSpeed(askRequestBo);
            Object backgroundConfig = askRequestBo.getOtherObj().getOrDefault("backgroundConfigTwo", null);
            if (backgroundConfig == null) {
                Object backgroundConfigTwo = askRequestBo.getOtherObj().getOrDefault("backgroundConfigOne", null);
                if (backgroundConfigTwo != null) {
                    if (backgroundConfigTwo instanceof Map) {
                        backgroundConfig = JSONObject.parseObject(JSONObject.toJSONString(backgroundConfigTwo), Map.class);
                        askRequestBo.getOtherObj().put("backgroundConfigTwo", backgroundConfig);
                    }
                }
            }

            backgroundConfig = askRequestBo.getOtherObj().getOrDefault("backgroundConfigTwo", null);
            // 判断是否是map<string,object>类型的
            if (backgroundConfig instanceof Map) {
                Map<String, Object> backgroundConfigMap = (Map<String, Object>) backgroundConfig;
                backgroundConfigMap.put("speechRate", speed);
            }
        }
    }
}
