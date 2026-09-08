package com.jiuyu.replay.api.logic.ai.impl;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.jiuyu.replay.api.logic.ai.dto.AudioaAlysesStatisticsDto;
import com.jiuyu.replay.api.logic.ai.utils.CommonSentenceUtils;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.generic.feign.third.TableStoreFeign;
import com.jiuyu.replay.generic.utils.ResultUtil;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.words.*;
import com.jiuyu.replay.words.bll.AnchorUrlBll;
import com.jiuyu.replay.words.bll.AnchorVideoBll;
import com.jiuyu.replay.words.bll.AnchorVideoDetailBll;
import com.jiuyu.replay.words.bo.AskRequestBo;
import com.jiuyu.replay.words.vo.OnlineNumInfoVo;
import com.jiuyu.replay.words.vo.oceanEngineData.JuliangStatisticsVo;
import com.jiuyu.replay.words.vo.video.AnchorVideoFileAllVo;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/3/23 下午4:02
 */
@Component
@Scope("prototype")
public class VideoSentenceImpl extends DefaultSentenceMarkImpl {

    @Resource
    private AnchorVideoBll anchorVideoBll;
    @Resource
    private AnchorUrlBll anchorUrlBll;
    @Resource
    private AnchorVideoDetailBll anchorVideoDetailBll;
    @Resource
    private TableStoreFeign tableStoreFeign;

    public AnalysisResultVo dto = null;
    public String content = null;
    public String textParamsContent = null;
    public AnchorVideoInfoVo anchorVideo;
    public List<OnlineNumInfoVo> onlineNumList = null;
    public List<Map<String, Object>> barrageDataList = null;

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

    public String minuteParagraphContent() {
        long allStartTime = anchorVideo.getStartTime().getTime();
        RRException.isNotEmpty(allStartTime, "视频的开始时间获取失败");
        List<AudioaAlysesStatisticsDto> list = new ArrayList<>();
        int onlineNumIndex = 0;

        Map<String, Object> dataDisplay = (Map<String, Object>) otherParams.get("aiAssistantDisplay");
        if (ObjectUtil.isEmpty(dataDisplay)) dataDisplay = new HashMap<>();

        // 开始时间
        boolean startTimeFlag = ((Integer) dataDisplay.getOrDefault("startTime", 1)) == 1;
        // 自然时间
        boolean natureTimeFlag = ((Integer) dataDisplay.getOrDefault("natureTime", 1)) == 1;
        // 在线人数
        boolean onlineNumFlag = ((Integer) dataDisplay.getOrDefault("onlineNum", 1)) == 1;
        // 弹幕人数
        boolean barrageNumFlag = ((Integer) dataDisplay.getOrDefault("barrageNum", 0)) == 1;
        // 成交数量
        boolean dealNumFlag = ((Integer) dataDisplay.getOrDefault("dealNum", 0)) == 1;
        // 互动率
        boolean interactionRateFlag = ((Integer) dataDisplay.getOrDefault("interactionRate", 0)) == 1;
        // 成交率
        boolean dealRateFlag = ((Integer) dataDisplay.getOrDefault("dealRate", 0)) == 1;
        // 语速
        boolean analysisCharFlag = ((Integer) dataDisplay.getOrDefault("analysisChar", 1)) == 1;
        // 销售额
        boolean salesFlag = ((Integer) dataDisplay.getOrDefault("sales", 0)) == 1;
        // uv价值
        boolean uvFlag = ((Integer) dataDisplay.getOrDefault("uv", 0)) == 1;
        for (int i = 0; i < dto.getSentenceMarkVos().size(); i++) {
            Date startTime = null;
            Date endTime = null;
            String renShu = null, textStart = null, textEnd = null, content = null;
            Long startTempTime = null, endTempTime = null, startVideoTime = null, endVideoTime = null;
            SentenceMarkVo item = dto.getSentenceMarkVos().get(i);
            if (ObjectUtil.isNotEmpty(item)) {
                content = ObjectUtil.defaultIfEmpty(item.getContent(), "");
                if (ObjectUtil.isNotEmpty(item.getItems())) {
                    startTempTime = ObjectUtil.defaultIfNull(item.getItems().get(0).getStartTime(), 0L);
                    endTempTime = ObjectUtil.defaultIfNull(item.getItems().get(item.getItems().size() - 1).getEndTime(), 0L);
                    startTime = new Date(allStartTime + startTempTime);
                    endTime = new Date(allStartTime + endTempTime);
                    textStart = DateUtil.format(new DateTime(startTempTime).offset(DateField.HOUR, -8), "HH:mm:ss");
                    textEnd = DateUtil.format(new DateTime(endTempTime).offset(DateField.HOUR, -8), "HH:mm:ss");
                }
            }
            if (startTime == null || endTime == null) {
                continue;
            }
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

            AudioaAlysesStatisticsDto audioa = new AudioaAlysesStatisticsDto();
            audioa.setStartTempTime(startTempTime);
            audioa.setEndTempTime(endTempTime);
            audioa.setStartVideoTime(startTime.getTime());
            audioa.setEndVideoTime(endTime.getTime());
            audioa.setStartTime(startTime);
            audioa.setEndTime(endTime);
            audioa.setTextStart(textStart);
            audioa.setTextEnd(textEnd);
            audioa.setRenShu(renShu);
            audioa.setContent(content);
            list.add(audioa);
        }
        Integer numIndex = (Integer) otherParams.get("singleMaxNum");
        StringBuilder str = new StringBuilder();
        if (ObjectUtil.isNotEmpty(list)) {
            Map<String, String> barrageMap = new HashMap<>();
            if (barrageNumFlag || interactionRateFlag) {
                List<Map<String, Object>> barrageList = getBarrageList();
                if (ObjectUtil.isNotEmpty(barrageList)) {
                    barrageMap = barrageList.stream()
                            .collect(Collectors.toMap(item -> item.getOrDefault("date", "0").toString(), item -> item.getOrDefault("barrageNum", "0").toString(), (k1, k2) -> k2));
                }
            }
            Map<Long, JuliangStatisticsVo> juliangDataMap = new HashMap<>();
            if (dealNumFlag || dealRateFlag) {
                // 获取巨量的实时数据
                juliangDataMap = CommonSentenceUtils.getJuliangData(videoDataViewingBll.getOceanEngineDetailsByVideoId(sourceId), list);
            }

            StringBuilder temp = new StringBuilder();
            for (int i = 0; i < list.size(); i++) {
                temp.setLength(0);
                AudioaAlysesStatisticsDto item = list.get(i);
                String renShu = ObjectUtil.defaultIfNull(item.getRenShu(), "0");
                Long startParagraphTime = item.getStartTempTime();
                Long endParagraphTime = item.getEndTempTime();
                if (ObjectUtil.isNotEmpty(videoTimeOneList) && videoTimeOneList.size() > 1) {
                    long start = videoTimeOneList.get(0);
                    long end = videoTimeOneList.get(1);
                    // 判断当前段落是否在指定的时间范围内
                    if (!(startParagraphTime >= start && endParagraphTime <= end)) {
                        continue;
                    }
                }

                temp.append(i == 0 ? "" : "\n\n");

                // 段落N时间:（开始时间：XX，结束时间：XX，自然时间：XX）
                StringBuilder timeSection = new StringBuilder();
                if (startTimeFlag) {
                    timeSection.append(StrUtil.format("开始时间：{}", ObjectUtil.defaultIfNull(item.getTextStart(), "")));
                    timeSection.append("，");
                    timeSection.append(StrUtil.format("结束时间：{}", ObjectUtil.defaultIfNull(item.getTextEnd(), "")));
                }
                if (natureTimeFlag && item.getStartTime() != null) {
                    if (timeSection.length() > 0) timeSection.append("，");
                    timeSection.append(StrUtil.format("自然时间：{}", DateUtil.format(item.getStartTime(), "HH:mm:ss")));
                }
                if (timeSection.length() > 0) {
                    temp.append(StrUtil.format("段落{}时间:（{}）\n", i + 1, timeSection));
                }

                // 获取上一段的在线人数
                int oldRenShu = i == 0 ? 0 : NumberUtil.parseInt(ObjectUtil.defaultIfNull(list.get(i - 1).getRenShu(), "0"), 0);
                // 获取当前段的在线人数
                int currentValue = NumberUtil.parseInt(renShu, 0);
                String strName = currentValue >= oldRenShu ? "增加" : "减少";
                int currentInt = i == 0 ? 0 : Math.abs(currentValue - oldRenShu);

                // 段落N数据:（在线人数：XX，比上段增加XX人，语速：XX字/分钟，...）
                StringBuilder dataSection = new StringBuilder();
                if (onlineNumFlag) {
                    dataSection.append(StrUtil.format("在线人数：{}", renShu));
                    dataSection.append("，");
                    dataSection.append(StrUtil.format("比上段{}{}人", strName, currentInt));
                }
                if (analysisCharFlag && item.getContent() != null) {
                    if (dataSection.length() > 0) dataSection.append("，");
                    String content = removePunctuation(item.getContent());
                    double num = content.length() / ((double) (item.getEndTempTime() - item.getStartTempTime()) / 1000) * 60;
                    dataSection.append(StrUtil.format("语速：{}字/分钟", (int) num));
                }
                int barrageNum = 0;
                if (barrageNumFlag || interactionRateFlag) {
                    Date startDateTemp = item.getStartTime();
                    String key = startDateTemp == null ? "0" : DateUtil.formatDateTime(startDateTemp);
                    barrageNum = NumberUtil.parseInt(barrageMap.getOrDefault(key, "0"), 0);
                }
                if (barrageNumFlag) {
                    if (dataSection.length() > 0) dataSection.append("，");
                    dataSection.append(StrUtil.format("弹幕条数：{}条", barrageNum));
                }
                JuliangStatisticsVo tempJuliang = juliangDataMap == null ? null : juliangDataMap.get(item.getStartVideoTime());

                if (dealNumFlag) {
                    if (dataSection.length() > 0) dataSection.append("，");
                    dataSection.append(StrUtil.format("成交人数：{}", (tempJuliang == null ? 0 : tempJuliang.getRangePayComboCnt())));
                }

                if (interactionRateFlag) {
                    int rs = NumberUtil.parseInt(renShu, 0);
                    if (barrageNum > 0 && rs > 0) {
                        if (dataSection.length() > 0) dataSection.append("，");
                        double rela = NumberUtil.round((double) barrageNum / rs * 100, 2).doubleValue();
                        dataSection.append(StrUtil.format("互动率：{}%", rela));
                    }
                }

                if (dealRateFlag) {
                    int rs = NumberUtil.parseInt(renShu, 0);
                    int tempJuliangNum = tempJuliang == null ? 0 : tempJuliang.getRangePayComboCnt();
                    if (tempJuliangNum > 0 && rs > 0) {
                        if (dataSection.length() > 0) dataSection.append("，");
                        double rela = NumberUtil.round((double) tempJuliangNum / rs * 100, 2).doubleValue();
                        dataSection.append(StrUtil.format("成交率：{}%", rela));
                    }
                }

                if (salesFlag) {
                    if (dataSection.length() > 0) dataSection.append("，");
                    dataSection.append(StrUtil.format("销售额：{}", (tempJuliang == null ? 0 : tempJuliang.getRangePayAmt())));
                }

                if (uvFlag) {
                    int rs = NumberUtil.parseInt(renShu, 0);
                    int rangePayAmt = tempJuliang == null ? 0 : tempJuliang.getRangePayAmt();
                    if (rangePayAmt > 0 && rs > 0) {
                        if (dataSection.length() > 0) dataSection.append("，");
                        double rela = NumberUtil.round((double) rangePayAmt / rs, 2).doubleValue();
                        dataSection.append(StrUtil.format("uv价值：{}", rela));
                    }
                }

                if (dataSection.length() > 0) {
                    temp.append(StrUtil.format("段落{}结束数据:（{}）\n", i + 1, dataSection));
                }

                temp.append(StrUtil.format("段落{}内容：", i + 1));

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
        AnchorVideoFileAllVo anchorVideoFile = ResultUtil.getResult(anchorVideoDetailBll.getVideoContent(sourceId, type, 0));
        StringBuilder str = new StringBuilder();
        str.append("\n");
        if (anchorVideoFile != null && anchorVideoFile.getContentStatus() == 2 && ObjectUtil.isNotEmpty(anchorVideoFile.getVideoFileContentList())) {
            long i = 0;
            int num = (Integer) otherParams.get("singleMaxNum");
            long startI = videoTimeOneList.get(0);
            long endI = videoTimeOneList.get(1);
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
        Integer questionContent = (Integer) otherParams.get("questionContent");
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

        String anchorName = this.anchorVideo.getAnchorInfo().getAnchorName();

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
        if (natureTimeFlag) {
            str += StrUtil.format("\n本段自然时间：指的是转译成的文字段落开始时对应的视频播放时间。");
        }
        if (startTimeFlag) {
            str += StrUtil.format("\n本段开始时间：指的是转译成的文字段落开始时对应的视频播放时间。");
            str += StrUtil.format("\n本段结束时间：指的是转译成的文字段落结束时对应的视频播放时间。");
        }
        if (onlineNumFlag) {
            str += StrUtil.format("\n本段在线人数：指的是转译成的文字段落开始时对应的直播间在线人数。");
        }
        if (analysisCharFlag) {
            str += StrUtil.format("\n本段语速：指的是1分钟内说的字数。");
        }
        if (barrageNumFlag) {
            str += StrUtil.format("\n本段发弹幕条数：指的是转译成的文字段落开始时间到结束时间内直播间发送弹幕数量。");
        }
        if (dealNumFlag) {
            str += StrUtil.format("\n本段成交人数：指的是转译成的文字段落开始时间到结束时间内商品的成交人数。");
        }
        if (interactionRateFlag) {
            str += StrUtil.format("\n本段互动率：指的是转译成的文字段落开始时间到结束时间内弹幕数量/在线人数得出的互动率。");
        }
        if (dealRateFlag) {
            str += StrUtil.format("\n本段成交率：指的是转译成的文字段落开始时间到结束时间内成交人数/在线人数得出的成交率。");
        }
        if (salesFlag) {
            str += StrUtil.format("\n本段销售额：指的是转译成的文字段落开始时间到结束时间内的销售额。");
        }
        if (uvFlag) {
            str += StrUtil.format("\n本段UV价值：指的是转译成的文字段落开始时间到结束时间内销售额/在线人数得出的UV价值。");
        }
        if (StrUtil.isNotEmpty(str)) {
            str = StrUtil.format("\n文本内的参数说明：") + str;
        }

        Long startTime = videoTimeOneList.get(0);
        Long endTime = videoTimeOneList.get(1);
        String startStr = DateUtil.format(new DateTime(startTime).offset(DateField.HOUR, -8), "HH:mm:ss");
        String endStr = DateUtil.format(new DateTime(endTime).offset(DateField.HOUR, -8), "HH:mm:ss");

        str += StrUtil.format("\n本场直播的视频时间是{}到{}，共{}秒的直播 \n\n", startStr, endStr, (endTime - startTime) / 1000);

        if (StrUtil.isNotEmpty(anchorName)) {
            str += "以下是#{platform}平台主播「" + anchorName + "」直播间录屏转写的完整文字脚本，内容为从第一段到最后一段的连续全文。";
        } else {
            str += "以下是#{platform}平台某直播账号直播间录屏转写的完整文字脚本，内容为从第一段到最后一段的连续全文。";
        }
        textParamsContent = str;
        return textParamsContent;
    }

    /**
     * 获取弹幕数量列表
     *
     * @return
     */
    public List<Map<String, Object>> getBarrageList() {
        if (this.barrageDataList == null) {
            // 弹幕标注
            this.barrageDataList = tableStoreFeign.getBarrageDataList(this.sourceId, dto.getSentenceMarkVos().stream().map(JSONUtil::toJsonStr).toList());
        }
        return this.barrageDataList;
    }

    @Override
    public Long getTradeId() {
        return anchorVideo.getTradeId();
    }

    @Override
    public String getAnchorName() {
        return anchorVideo.getAnchorInfo().getAnchorName();
    }

    @Override
    public String getPlatformValue() {
        AnchorVideoInfoVo anchorVideoInfoVo = getAnchorVideoInfoVo();
        return anchorVideoInfoVo != null ? anchorVideoInfoVo.getPlatformType() : null;
    }

    @Override
    public Integer getSpeed(AskRequestBo askRequestBo) {
        if (dto == null || dto.getSentenceMarkVos() == null || dto.getSentenceMarkVos().isEmpty() || anchorVideo == null || anchorVideo.getDuration() == null || anchorVideo.getDuration() == 0) {
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
}
