package com.jiuyu.replay.api.logic.ai.impl;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.words.AnchorUrlInfoVo;
import com.jiuyu.replay.generic.vo.words.AnchorVideoInfoVo;
import com.jiuyu.replay.words.bll.AnchorUrlBll;
import com.jiuyu.replay.words.bll.AnchorVideoBll;
import com.jiuyu.replay.generic.vo.words.AnalysisResultVo;
import com.jiuyu.replay.words.vo.OnlineNumInfoVo;
import com.jiuyu.replay.generic.vo.words.SentenceMarkVo;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/3/23 下午4:02
 */
@Component
@Scope("prototype")
public class VideoSentenceOldImpl extends DefaultSentenceMarkImpl {

    @Resource
    private AnchorVideoBll anchorVideoBll;
    @Resource
    private AnchorUrlBll anchorUrlBll;

    public AnalysisResultVo dto = null;
    public String content = null;
    public String textParamsContent = null;
    public AnchorVideoInfoVo anchorVideo;
    public List<OnlineNumInfoVo> onlineNumList = null;

    @Override
    public void init(String sourceId, Integer askType, List<Long> videoTimeOneList, List<Long> videoTimeTwoList) {
        super.init(sourceId, sourceType, askType, videoTimeOneList, videoTimeTwoList);
        super.init(sourceId, sourceType, askType, videoTimeOneList, videoTimeTwoList);
        this.sourceType = 0;
        dto = (AnalysisResultVo) super.getMarkDto();
        // 获取视频信息
        getAnchorVideoInfoVo();

        // 获取在线人数
        getOnlineNumList();
    }

    /**
     * 获取视频信息
     *
     * @return
     */
    protected AnchorVideoInfoVo getAnchorVideoInfoVo() {
        if (anchorVideo == null) {
            R<AnchorVideoInfoVo> anchorVideoInfoVoR = anchorVideoBll.GetByVideoId(sourceId);
            RRException.isNotEmpty(anchorVideoInfoVoR.getData(), "视频不存在");
            AnchorVideoInfoVo data = anchorVideoInfoVoR.getData();
            R<AnchorUrlInfoVo> info = anchorUrlBll.infoBySecUidOne(data.getSecUid());
            RRException.isNotEmpty(info.getData(), "用户信息获取失败");
            data.setAnchorInfo(info.getData());
            this.anchorVideo = data;
            return data;
        } else {
            return anchorVideo;
        }
    }

    /**
     * 获取在线人数
     *
     * @return
     */
    private List<OnlineNumInfoVo> getOnlineNumList() {
        if (onlineNumList != null) return onlineNumList;
        this.onlineNumList = super.getOnlineNumList(anchorVideo.getBatchNumber().toString(), anchorVideo.getUserId(), anchorVideo.getVideoId());
        return onlineNumList;
    }

    @Override
    public String getContent() {
        if (content != null) return this.content;
        long allStartTime = anchorVideo.getStartTime().getTime();
        RRException.isNotEmpty(allStartTime, "视频的开始时间获取失败");
        List<Map<String, Object>> list = new ArrayList<>();
        int onlineNumIndex = 0;
        for (int i = 0; i < dto.getSentenceMarkVos().size(); i++) {
            Date startTime = null;
            Date endTime = null;
            String renShu = null, textStart = null, textEnd = null, content = null;
            SentenceMarkVo item = dto.getSentenceMarkVos().get(i);
            Map<String, Object> temp = new HashMap<>();
            if (ObjectUtil.isNotEmpty(item)) {
                content = ObjectUtil.defaultIfEmpty(item.getContent(), "");
                if (ObjectUtil.isNotEmpty(item.getItems())) {
                    long startTempTime = ObjectUtil.defaultIfNull(item.getItems().get(0).getStartTime(), 0L);
                    long endTempTime = ObjectUtil.defaultIfNull(item.getItems().get(item.getItems().size() - 1).getEndTime(), 0L);
                    startTime = new Date(allStartTime + startTempTime);
                    endTime = new Date(allStartTime + endTempTime);
                    textStart = DateUtil.format(new DateTime(startTempTime).offset(DateField.HOUR, -8), "HH:mm:ss");
                    textEnd = DateUtil.format(new DateTime(endTempTime).offset(DateField.HOUR, -8), "HH:mm:ss");
                    temp.put("startParagraphTime", startTempTime);
                    temp.put("endParagraphTime", endTempTime);
                }
            }
            if (startTime == null || endTime == null) continue;
            // 获取在线人数
            if (ObjectUtil.isNotEmpty(this.onlineNumList)) {
                for (int j = onlineNumIndex; j < this.onlineNumList.size(); j++) {
                    OnlineNumInfoVo onlineNum = this.onlineNumList.get(j);
                    if (ObjectUtil.isNotEmpty(onlineNum.getRecordDate())) {
                        Date dateTime = DateUtil.parse(onlineNum.getRecordDate());
                        if (dateTime.compareTo(startTime) < 0) continue;
                        if (dateTime.compareTo(endTime) > 0) break;
                        if (dateTime.compareTo(startTime) >= 0 && dateTime.compareTo(endTime) <= 0) {
                            renShu = onlineNum.getPeopleNum();
                        }
                    }
                    onlineNumIndex = j;
                }
            }
            temp.put("startTime", startTime);
            temp.put("endTime", endTime);
            temp.put("textStart", textStart);
            temp.put("textEnd", textEnd);
            temp.put("renShu", renShu);
            temp.put("content", content);
            list.add(temp);
        }
        Integer numIndex = (Integer) otherParams.get("singleMaxNum");
        StringBuilder str = new StringBuilder();
        if (ObjectUtil.isNotEmpty(list)) {
            StringBuilder temp = new StringBuilder();
            for (int i = 0; i < list.size(); i++) {
                temp.setLength(0);
                Map<String, Object> item = list.get(i);
                String renShu;
                if (i == 0) {
                    renShu = MapUtil.getStr(item, "renShu", "0");
                } else {
                    renShu = MapUtil.getStr(list.get(i - 1), "renShu", "0");
                }
                Long startParagraphTime = (Long) item.getOrDefault("startParagraphTime", 0);
                Long endParagraphTime = (Long) item.getOrDefault("endParagraphTime", 0);
                if (ObjectUtil.isNotEmpty(videoTimeOneList) && videoTimeOneList.size() > 1) {
                    long start = videoTimeOneList.get(0);
                    long end = videoTimeOneList.get(1);
                    // 判断当前段落是否在指定的时间范围内
                    if (!(startParagraphTime >= start && endParagraphTime <= end)) {
                        continue;
                    }
                }

                temp.append(i == 0 ? "" : "\n\n");
                temp.append(StrUtil.format("本段开始时间：{}", MapUtil.getStr(item, "textStart", "")));
                temp.append(StrUtil.format("\n本段结束时间：{}", MapUtil.getStr(item, "textEnd", "")));
                temp.append(StrUtil.format("\n本段在线人数：{}", renShu));
                // 获取上一段的在线人数
                int oldRenShu = i == 0 ? 0 : NumberUtil.parseInt(MapUtil.getStr(list.get(i - 1), "renShu", "0"), 0);
                // 获取当前段的在线人数
                int currentValue = NumberUtil.parseInt(renShu, 0);
                String strName = currentValue >= oldRenShu ? "增加" : "减少";
                int currentInt = i == 0 ? 0 : Math.abs(currentValue - oldRenShu);
                temp.append(StrUtil.format("\n本段在线人数{}：{}人", strName, currentInt));
                temp.append(StrUtil.format("\n本段内容："));

                String tempContent = (String) item.getOrDefault("content", "");
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

    @Override
    public String getTextParamsContent() {
        if (textParamsContent != null) return textParamsContent;

        String anchorName = this.anchorVideo.getAnchorInfo().getAnchorName();
        // 获取视频的结束时间
        String str = "";
        str += StrUtil.format("\n文本内的参数说明：");
        str += StrUtil.format("\n本段开始时间：指的是转译成的文字段落开始时对应的视频播放时间。");
        str += StrUtil.format("\n本段结束时间：指的是转译成的文字段落结束时对应的视频播放时间。");
        str += StrUtil.format("\n本段在线人数：指的是转译成的文字段落开始时对应的直播间在线人数。");
        str += StrUtil.format("\n从第二段开始直到最后一段，每一段的本段自然时间是上一段的自然结束时间。");

        Long startTime = videoTimeOneList.get(0);
        Long endTime = videoTimeOneList.get(1);
        String startStr = DateUtil.format(new DateTime(startTime).offset(DateField.HOUR, -8), "HH:mm:ss");
        String endStr = DateUtil.format(new DateTime(endTime).offset(DateField.HOUR, -8), "HH:mm:ss");

        str += StrUtil.format("\n本场直播的视频时间是{}到{}，共{}秒的直播 \n\n", startStr, endStr, (endTime - startTime) / 1000);
        str += "以下是由#{platform}账号";
        if (StrUtil.isNotEmpty(anchorName)) str += StrUtil.format("昵称为{}的", anchorName);
        str += StrUtil.format("直播间录屏成视频后转译成文字的直播全文脚本，从第一段到最后一段，每一段的内容都是连续的。");
        textParamsContent = str;
        return textParamsContent;
    }

    @Override
    public Long getTradeId() {
        return anchorVideo.getTradeId();
    }

    @Override
    public String getAnchorName() {
        return anchorVideo.getAnchorInfo().getAnchorName();
    }
}
