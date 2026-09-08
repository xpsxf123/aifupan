package com.jiuyu.replay.api.logic.ai.impl;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.jiuyu.replay.api.logic.ai.dto.AudioaAlysesStatisticsDto;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.generic.utils.ResultUtil;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.words.AnalysisResultVo;
import com.jiuyu.replay.generic.vo.words.SentenceMarkVo;
import com.jiuyu.replay.generic.vo.words.VideoContentVo;
import com.jiuyu.replay.words.bll.AnchorVideoDetailBll;
import com.jiuyu.replay.words.bll.UploadFileBll;
import com.jiuyu.replay.words.bo.AskRequestBo;
import com.jiuyu.replay.words.vo.file.UploadFileInfoVo;
import com.jiuyu.replay.words.vo.video.AnchorVideoFileAllVo;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/**
 * @author ：lujie
 * @description：文件的数据
 * @date ：2025/3/23 下午3:59
 */
@Component
@Scope("prototype")
public class FileSentenceImpl extends DefaultSentenceMarkImpl {

    @Resource
    public UploadFileBll uploadFileBll;
    @Resource
    private AnchorVideoDetailBll anchorVideoDetailBll;

    private AnalysisResultVo dto = null;
    public String content = null;
    public String textParamsContent = null;
    public UploadFileInfoVo uploadFile;

    @Override
    public void init(String sourceId, Integer askType, List<Long> videoTimeOneList, List<Long> videoTimeTwoList) {
        super.init(sourceId, sourceType, askType, videoTimeOneList, videoTimeTwoList);
        this.sourceType = 1;
        dto = (AnalysisResultVo) super.getMarkDto();

        R<UploadFileInfoVo> uploadFileInfoVoR = uploadFileBll.infoByFileId(sourceId);
        RRException.isNotEmpty(uploadFileInfoVoR, "获取文件信息失败");
        uploadFile = uploadFileInfoVoR.getData();
    }

    public String minuteParagraphContent() {
        List<AudioaAlysesStatisticsDto> list = new ArrayList<>();
        for (int i = 0; i < dto.getSentenceMarkVos().size(); i++) {
            Date startTime = null;
            Date endTime = null;
            String textStart = null, textEnd = null, content = null;
            Long startTempTime = null, endTempTime = null;
            SentenceMarkVo item = dto.getSentenceMarkVos().get(i);
            if (ObjectUtil.isNotEmpty(item)) {
                content = ObjectUtil.defaultIfEmpty(item.getContent(), "");
                if (ObjectUtil.isNotEmpty(item.getItems())) {
                    startTempTime = ObjectUtil.defaultIfNull(item.getItems().get(0).getStartTime(), 0L);
                    endTempTime = ObjectUtil.defaultIfNull(item.getItems().get(item.getItems().size() - 1).getEndTime(), 0L);
                    textStart = DateUtil.format(new DateTime(startTempTime).offset(DateField.HOUR, -8), "HH:mm:ss");
                    textEnd = DateUtil.format(new DateTime(endTempTime).offset(DateField.HOUR, -8), "HH:mm:ss");
                }
            }
            AudioaAlysesStatisticsDto audioa = new AudioaAlysesStatisticsDto();
            audioa.setStartTime(startTime);
            audioa.setEndTime(endTime);
            audioa.setTextStart(textStart);
            audioa.setTextEnd(textEnd);
            audioa.setContent(content);
            audioa.setStartTempTime(startTempTime);
            audioa.setEndTempTime(endTempTime);
            list.add(audioa);
        }

        StringBuilder str = new StringBuilder();
        Integer numIndex = (Integer) otherParams.get("singleMaxNum");
        if (ObjectUtil.isNotEmpty(list)) {
            int tempDuration = NumberUtil.parseInt(this.uploadFile.getFileDuration(), 0);
            StringBuilder temp = new StringBuilder();
            Map<String, Object> dataDisplay = (Map<String, Object>) otherParams.get("aiAssistantDisplay");
            if (ObjectUtil.isEmpty(dataDisplay)) dataDisplay = new HashMap<>();

            // 开始时间
            boolean startTimeFlag = ((Integer) dataDisplay.getOrDefault("startTime", 1)) == 1;
            // 自然时间-单前无用
            boolean natureTimeFlag = ((Integer) dataDisplay.getOrDefault("natureTime", 1)) == 1;
            // 在线人数
            boolean onlineNumFlag = ((Integer) dataDisplay.getOrDefault("onlineNum", 1)) == 1;
            // 弹幕人数
            boolean barrageNumFlag = ((Integer) dataDisplay.getOrDefault("barrageNum", 0)) == 1;
            // 成交数量-单前无用
            boolean dealNumFlag = ((Integer) dataDisplay.getOrDefault("dealNum", 0)) == 1;
            // 成交数量-单前无用
            boolean analysisCharFlag = ((Integer) dataDisplay.getOrDefault("analysisChar", 1)) == 1;
            for (int i = 0; i < list.size(); i++) {
                temp.setLength(0);
                AudioaAlysesStatisticsDto item = list.get(i);
                if (tempDuration > 0) {
                    Long startParagraphTime = item.getStartTempTime();
                    Long endParagraphTime = item.getEndTempTime();
                    if (startParagraphTime != null && endParagraphTime != null
                            && ObjectUtil.isNotEmpty(videoTimeOneList) && videoTimeOneList.size() > 1) {
                        long start = videoTimeOneList.get(0);
                        long end = videoTimeOneList.get(1);
                        // 判断当前段落是否在指定的时间范围内
                        if (!(startParagraphTime >= start && endParagraphTime <= end)) {
                            continue;
                        }
                    }
                }
                temp.append(i == 0 ? "" : "\n\n");
                // 语速
                if (analysisCharFlag && item.getContent() != null
                        && item.getStartTempTime() != null && item.getEndTempTime() != null) {
                    String content = item.getContent()
                            .replaceAll("，", "")
                            .replaceAll("。", "")
                            .replaceAll("、", "")
                            .replaceAll("？", "");
                    double num = content.length() / ((double) (item.getEndTempTime() - item.getStartTempTime()) / 1000) * 60;
                    temp.append(StrUtil.format("\n本段语速：{}字/分钟", (int) num));
                }
                if (startTimeFlag && StrUtil.isNotEmpty(item.getTextStart())) {
                    temp.append(StrUtil.format("本段开始时间：{}", item.getTextStart()));
                }
                if (startTimeFlag && StrUtil.isNotEmpty(item.getTextEnd())) {
                    temp.append(StrUtil.format("\n本段结束时间：{}", item.getTextEnd()));
                }
                temp.append(StrUtil.format("\n本段内容："));

                String tempContent = item.getContent();
                if (numIndex != -1 && numIndex - str.length() - temp.length() - tempContent.length() < 0) {
                    int tempI = numIndex - str.length() - temp.length();
                    if (tempI > 0) {
                        if (tempContent.length() > tempI) {
                            tempContent = tempContent.substring(0, tempI);
                        }
                    } else {
                        break;
                    }
                }
                temp.append(tempContent);
                if (numIndex != -1 && numIndex - str.length() - temp.length() < 0) {
                    break;
                }

                str.append(temp);
            }
        }
        this.content = str.toString();
        return this.content;
    }

    public String textContent(int type, String name) {
        AnchorVideoFileAllVo anchorVideoFile = ResultUtil.getResult(anchorVideoDetailBll.getVideoContent(sourceId, type, 1));
        StringBuilder str = new StringBuilder();
        str.append("\n");
        if (anchorVideoFile != null && anchorVideoFile.getContentStatus() == 2 && ObjectUtil.isNotEmpty(anchorVideoFile.getVideoFileContentList())) {
            long i = 0;
            int num = (Integer) otherParams.get("singleMaxNum");
            long startI = videoTimeOneList == null ? 0 : videoTimeOneList.get(0);
            long endI = videoTimeOneList == null ? 999999999 : videoTimeOneList.get(1);
            for (int j = 0; j < anchorVideoFile.getVideoFileContentList().size(); j++) {
                VideoContentVo item = anchorVideoFile.getVideoFileContentList().get(j);
                if (ObjectUtil.isNotEmpty(item.getContentList())) {
                    for (int index = 0; index < item.getContentList().size(); index++) {
                        if (i >= startI && i < endI) {
                            String temp = item.getContentList().get(index) + "\n";
                            int tempNum = str.length() + temp.length();
                            if (num != -1 && num - tempNum <= 0) {
                                break;
                            }
                            str.append(temp);
                        }
                        i++;
                    }

                }
            }
        } else {
            RRException.create(7005, StrUtil.format("未生成{}，请您先生成。", name));
        }
        this.content = str.toString();
        return this.content;
    }

    @Override
    public String getContent() {
        if (content != null) return this.content;
        Integer questionContent = (Integer) otherParams.getOrDefault("questionContent", 0);
        questionContent = questionContent == null ? 0 : questionContent;
        if (questionContent == 0) {
            return minuteParagraphContent();
        } else if (questionContent == 1) {
            return textContent(1, "自然原文");
        } else if (questionContent == 2) {
            return textContent(2, "优化原文");
        } else {
            RRException.create(7005, "questionContent未知");
        }

        return "";

    }

    @Override
    public String getTextParamsContent() {
        if (textParamsContent != null) return textParamsContent;

        Map<String, Object> dataDisplay = (Map<String, Object>) otherParams.get("aiAssistantDisplay");
        if (ObjectUtil.isEmpty(dataDisplay)) dataDisplay = new HashMap<>();
        boolean analysisCharFlag = ((Integer) dataDisplay.getOrDefault("analysisChar", 1)) == 1;
        boolean startTimeFlag = ((Integer) dataDisplay.getOrDefault("startTime", 1)) == 1;

        Integer questionContent = (Integer) otherParams.getOrDefault("questionContent", 0);
        String str = "";
        if (ObjectUtil.isNotEmpty(videoTimeOneList) && questionContent == 0) {
            StringBuilder descSb = new StringBuilder();
            if (analysisCharFlag) {
                descSb.append(StrUtil.format("\n本段语速：指的是1分钟内说的字数。"));
            }
            if (startTimeFlag) {
                descSb.append(StrUtil.format("\n本段开始时间：指的是转译成的文字段落开始时对应的视频播放时间。"));
                descSb.append(StrUtil.format("\n本段结束时间：指的是转译成的文字段落结束时对应的视频播放时间。"));
            }
            if (descSb.length() > 0) {
                str += StrUtil.format("\n文本内的参数说明：") + descSb.toString();
            }

            Long startTime = videoTimeOneList.get(0);
            Long endTime = videoTimeOneList.get(1);
            String startStr = DateUtil.format(new DateTime(startTime).offset(DateField.HOUR, -8), "HH:mm:ss");
            String endStr = DateUtil.format(new DateTime(endTime).offset(DateField.HOUR, -8), "HH:mm:ss");

            str += StrUtil.format("\n本场直播的视频时间是{}到{}，共{}秒的直播 \n\n", startStr, endStr, (endTime - startTime) / 1000);
        }
        str += StrUtil.format("以下是#{platform}平台某直播账号直播间录屏转写的完整文字脚本，内容为从第一段到最后一段的连续全文。");
        textParamsContent = str;
        return textParamsContent;
    }

    @Override
    public Long getTradeId() {
        return uploadFile.getTradeId();
    }

    @Override
    public String getPlatformValue() {
        return uploadFile != null ? uploadFile.getPlatformType() : null;
    }

    @Override
    public Integer getSpeed(AskRequestBo askRequestBo) {
        if (dto == null || dto.getSentenceMarkVos() == null || dto.getSentenceMarkVos().isEmpty() || uploadFile == null || uploadFile.getFileDuration() == null || "0".equals(uploadFile.getFileDuration())) {
            return null;
        }
        int contentSum = dto.getSentenceMarkVos().stream().mapToInt(item -> {
            if (ObjectUtil.isEmpty(item.getContent())) {
                return 0;
            }
            // 获取去掉标点的内容
            String content1 = removePunctuation(item.getContent());
            return content1.length();
        }).sum();

        if (contentSum == 0) {
            return 0;
        }

        long time = dto.getSentenceMarkVos().stream().mapToLong(item -> {
            if (ObjectUtil.isEmpty(item.getItems())) {
                return 0;
            }
            long[] array = item.getItems().stream()
                    .flatMapToLong(jtem -> Stream.of(jtem.getStartTime(), jtem.getEndTime()).mapToLong(Long::longValue)).toArray();
            long min = LongStream.of(array).min().orElse(0);
            long max = LongStream.of(array).max().orElse(0);
            return max - min;
        }).sum();

        return (int) (contentSum / (time / 1000.0) * 60.0);
    }

    @Override
    public void setSpeed(AskRequestBo askRequestBo) {

    }
}
