package com.jiuyu.replay.api.logic.ai.utils;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.jiuyu.replay.api.logic.ai.SentenceMark;
import com.jiuyu.replay.api.logic.ai.dto.AudioaAlysesStatisticsDto;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.words.bll.HistoryParagraphBll;
import com.jiuyu.replay.words.bo.AskRequestBo;
import com.jiuyu.replay.words.bo.oceanEngine.OceanEngineProcessBo;
import com.jiuyu.replay.words.vo.HistoryParagraphInfoVo;
import com.jiuyu.replay.words.vo.oceanEngineData.JuliangStatisticsVo;
import com.jiuyu.replay.words.vo.viewing.VideoDataViewingConfuseInfoVo;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/4/18 下午3:42
 */
@Component
public class CommonSentenceUtils {

    public static String setAskScreenshotQuestion(AskRequestBo askRequestBo, HistoryParagraphBll historyParagraphBll, SentenceMark mark) {
        String str = "";
        if (ObjectUtil.isNotEmpty(askRequestBo)){
            if (askRequestBo.getUseModelWay() == 0) {
                // 有上下文缓存
                if (StrUtil.isNotEmpty(askRequestBo.getParagraphCode()) && ObjectUtil.isEmpty(askRequestBo.getParagraphContent()))
                {
                    if (ObjectUtil.isEmpty(askRequestBo.getParagraphCode()) || "0".equals(askRequestBo.getParagraphCode()))
                    {
                        str = "\n以上问题针对全场直播相关数据全文。";
                    }
                    else
                    {
                        R<HistoryParagraphInfoVo> byCode = historyParagraphBll.getByCode(askRequestBo.getType(), askRequestBo.getSourceId(), askRequestBo.getSourceType(), askRequestBo.getParagraphCode());
                        RRException.isNotEmpty(byCode.getData(), "获取历史段落失败", 7005);
                        String content = byCode.getData().getContent();
                        RRException.isNotEmpty(content, "获取历史段落失败", 7005);
                        str = StrUtil.format("\n本次回答内容不用考虑全文，除非提问中明确你需要涉及全文相关内容做分析，否则只需要根据以下内容进行回答：\n{}", content);
                    }
                }
                else
                {
                    if (StrUtil.isNotEmpty(askRequestBo.getParagraphContent()))
                    {
                        str = StrUtil.format("\n本次回答内容不用考虑全文，除非提问中明确你需要涉及全文相关内容做分析，否则只需要根据以下内容进行回答：\n{}", askRequestBo.getParagraphContent());
                    }
                    else
                    {
                        str = "\n以上问题针对全场直播相关数据全文。";
                    }
                }
            } else if (askRequestBo.getUseModelWay() == 1) {
                // 没有上下文缓存
                if (StrUtil.isEmpty(askRequestBo.getParagraphContent()))
                {
                    if (ObjectUtil.isNotEmpty(askRequestBo.getParagraphCode()) && !"0".equals(askRequestBo.getParagraphCode()))
                    {
                        R<HistoryParagraphInfoVo> byCode = historyParagraphBll.getByCode(askRequestBo.getType(), askRequestBo.getSourceId(), askRequestBo.getSourceType(), askRequestBo.getParagraphCode());
                        RRException.isNotEmpty(byCode.getData(), "获取历史段落失败", 7005);
                        String content = byCode.getData().getContent();
                        str = StrUtil.format("\n针对本次问题回答的范围为以下内容：\n{}", content);
                    }else {
                        str = StrUtil.format("\n针对本次问题回答的范围为以下内容：\n{}", mark.getContent());
                    }
                }
                else
                {
                    str = StrUtil.format("\n针对本次问题回答的范围为以下内容：\n{}", askRequestBo.getParagraphContent());
                }
            }
        }
        return  str;
    }


    /**
     * 根据时间获取巨量对应的数据
     *
     * @param data 巨量数据
     * @param list 音频分析统计数据列表
     * @return 巨量统计数据映射
     */
    public static Map<Long, JuliangStatisticsVo> getJuliangData(VideoDataViewingConfuseInfoVo data, List<AudioaAlysesStatisticsDto> list) {
        if (ObjectUtil.isEmpty(list) || ObjectUtil.isEmpty(data)) {
            return new HashMap<>();
        }

        if (ObjectUtil.isEmpty(data) || ObjectUtil.isEmpty(data.getOceanEngineProcessList())) {
            return new HashMap<>();
        }

        List<OceanEngineProcessBo> juliangList = new ArrayList<>(data.getOceanEngineProcessList());

        // 对juliangList排序，gatherTimeStamp字段升序
        juliangList.sort(Comparator.comparing(OceanEngineProcessBo::getGatherTimeStamp));

        int tempJ = 0;
        List<JuliangStatisticsVo> result = new ArrayList<>();

        // 遍历list的数据
        for (int i = 0; i < list.size(); i++) {
            AudioaAlysesStatisticsDto audioaAlysesDto = list.get(i);
            List<OceanEngineProcessBo> temp = new ArrayList<>();

            for (int j = tempJ; j < juliangList.size(); j++) {
                OceanEngineProcessBo item = juliangList.get(j);

                if (item != null) {
                    if (item.getGatherTimeStamp() >= audioaAlysesDto.getEndVideoTime()) {
                        break;
                    } else if (item.getGatherTimeStamp() > audioaAlysesDto.getStartVideoTime()
                            && item.getGatherTimeStamp() < audioaAlysesDto.getEndVideoTime()) {
                        temp.add(item);
                    }
                }
            }

            OceanEngineProcessBo max = null;
            OceanEngineProcessBo min = null;

            if (!temp.isEmpty()) {
                // 获取最大的值
                temp.sort((a, b) -> Long.compare(b.getGatherTimeStamp(), a.getGatherTimeStamp()));
                max = temp.get(0);
                min = temp.get(temp.size() - 1);
            } else {
                if (i > 0) {
                    JuliangStatisticsVo tempDto = result.get(i - 1);
                    if (tempDto != null) {
                        max = new OceanEngineProcessBo();
                        max.setPayComboCnt(ObjectUtil.defaultIfNull(tempDto.getPayComboCntMax(), 0));
                        max.setPayAmt(ObjectUtil.defaultIfNull(tempDto.getPayAmtMax(), 0));
                        max.setFansClubJoinUcnt(ObjectUtil.defaultIfNull(tempDto.getFansClubJoinUcntMax(), 0));
                        max.setFollowAnchorUcnt(ObjectUtil.defaultIfNull(tempDto.getFollowAnchorUcntMax(), 0));

                        min = new OceanEngineProcessBo();
                        min.setPayComboCnt(ObjectUtil.defaultIfNull(tempDto.getPayComboCntMax(), 0));
                        min.setPayAmt(ObjectUtil.defaultIfNull(tempDto.getPayAmtMax(), 0));
                        min.setFansClubJoinUcnt(ObjectUtil.defaultIfNull(tempDto.getFansClubJoinUcntMax(), 0));
                        min.setFollowAnchorUcnt(ObjectUtil.defaultIfNull(tempDto.getFollowAnchorUcntMax(), 0));
                    }
                }
                if (max == null) {
                    max = new OceanEngineProcessBo();
                }
                if (min == null) {
                    min = new OceanEngineProcessBo();
                }
            }

            JuliangStatisticsVo dto = new JuliangStatisticsVo();
            dto.setStartVideoTime(audioaAlysesDto.getStartVideoTime());
            dto.setPayComboCntMin(ObjectUtil.defaultIfNull(min.getPayComboCnt(), 0));
            dto.setPayComboCntMax(ObjectUtil.defaultIfNull(max.getPayComboCnt(), 0));
            dto.setPayAmtMin(ObjectUtil.defaultIfNull(min.getPayAmt(), 0));
            dto.setPayAmtMax(ObjectUtil.defaultIfNull(max.getPayAmt(), 0));
            dto.setFansClubJoinUcntMin(ObjectUtil.defaultIfNull(min.getFansClubJoinUcnt(), 0));
            dto.setFansClubJoinUcntMax(ObjectUtil.defaultIfNull(max.getFansClubJoinUcnt(), 0));
            dto.setFollowAnchorUcntMin(ObjectUtil.defaultIfNull(min.getFollowAnchorUcnt(), 0));
            dto.setFollowAnchorUcntMax(ObjectUtil.defaultIfNull(max.getFollowAnchorUcnt(), 0));

            result.add(dto);
        }

        // 计算范围值
        if (!result.isEmpty()) {
            for (int i = 0; i < result.size(); i++) {
                if (i != result.size() - 1) {
                    result.get(i).setPayComboCntMax(result.get(i + 1).getPayComboCntMin());
                    result.get(i).setPayAmtMax(result.get(i + 1).getPayAmtMin());
                    result.get(i).setFansClubJoinUcntMax(result.get(i + 1).getFansClubJoinUcntMin());
                    result.get(i).setFollowAnchorUcntMax(result.get(i + 1).getFollowAnchorUcntMin());
                }

                JuliangStatisticsVo item = result.get(i);
                item.setRangePayComboCnt(item.getPayComboCntMax() - item.getPayComboCntMin());
                item.setRangePayAmt(item.getPayAmtMax() - item.getPayAmtMin());
                item.setRangeFansClubJoinUcnt(item.getFansClubJoinUcntMax() - item.getFansClubJoinUcntMin());
                item.setRangeFollowAnchorUcnt(item.getFollowAnchorUcntMax() - item.getFollowAnchorUcntMin());
            }
        }

        if (!result.isEmpty()) {
            return result.stream().collect(Collectors.toMap(
                    JuliangStatisticsVo::getStartVideoTime,
                    Function.identity(),
                    (a, b) -> b
            ));
        }

        return new HashMap<>();
    }
}
