package com.jiuyu.replay.api.logic.ai.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson2.JSONObject;
import com.jiuyu.replay.api.logic.ai.SentenceMark;
import com.jiuyu.replay.common.constant.RedisCacheKey;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.common.utils.ReplayFileUtils;
import com.jiuyu.replay.generic.feign.system.DictDataFeign;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.common.code.StatusCode;
import com.jiuyu.replay.generic.vo.system.DictDataListVo;
import com.jiuyu.replay.generic.vo.third.AiTempTokenVo;
import com.jiuyu.replay.generic.vo.words.AnalysisResultVo;
import com.jiuyu.replay.generic.vo.words.TradeInfoVo;
import com.jiuyu.replay.third.bll.AiModelBll;
import com.jiuyu.replay.words.bll.*;
import com.jiuyu.replay.words.bo.AskRequestBo;
import com.jiuyu.replay.words.vo.DataScreenshotListVo;
import com.jiuyu.replay.words.vo.HistoryParagraphInfoVo;
import com.jiuyu.replay.words.vo.OnlineNumInfoVo;
import com.jiuyu.replay.words.vo.viewing.VideoDataViewingConfuseInfoVo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/3/24 下午3:08
 */
@Component
@Scope("prototype")
@Slf4j
public class DefaultSentenceMarkImpl implements SentenceMark {
    public String sourceId;
    public int sourceType = 0;
    public int askType = 0;
    public List<Long> videoTimeOneList;
    public List<Long> videoTimeTwoList;
    public Map<String, Object> otherParams = new HashMap<>();
    public String dataScreenshot;
    public String board;
    private TradeInfoVo trade;

    @Resource
    public SensitiveWordsBll sensitiveWordsBll;
    @Resource
    public SocketCollectMessageBll socketCollectMessageBll;
    @Resource
    public DataScreenshotBll dataScreenshotBll;
    @Resource
    public TradeBll tradeBll;
    @Resource
    public HistoryParagraphBll historyParagraphBll;
    @Resource
    public VideoDataViewingBll videoDataViewingBll;
    @Resource
    public DictDataFeign dictDataFeign;
    @Resource
    public AiModelBll aiModelBll;

    @Override
    public void init(String sourceId, Integer askType, List<Long> videoTimeOneList, List<Long> videoTimeTwoList) {
        this.sourceId = sourceId;
        this.videoTimeOneList = videoTimeOneList;
        this.videoTimeTwoList = videoTimeTwoList;
    }

    public void init(String sourceId, int sourceType, Integer askType, List<Long> videoTimeOneList, List<Long> videoTimeTwoList) {
        this.sourceId = sourceId;
        this.sourceType = sourceType;
        this.askType = askType;
        this.videoTimeOneList = videoTimeOneList;
        this.videoTimeTwoList = videoTimeTwoList;
    }

    @Override
    public void setOtherParams(Map<String, Object> otherParams) {
        // 校验singleMaxNum字段
        if (otherParams.containsKey("singleMaxNum")){
            if (otherParams.get("singleMaxNum") instanceof Integer){
                int singleMaxNum = (int) otherParams.get("singleMaxNum");
                if (singleMaxNum == 0 || singleMaxNum < -1){
                    RRException.create("提示词超出字数限制");
                }
            }
            else {
                throw new RRException("singleMaxNum参数错误");
            }
        }

        this.otherParams.putAll(otherParams);
    }

    @Override
    public String getContextRedisKey(AskRequestBo askRequestBo) {
        Map<String, Object> otherObj = askRequestBo.getOtherObj();
        int questionContent = otherObj == null ? 0 : NumberUtil.parseInt(otherObj.getOrDefault("questionContent", 0).toString(), 0);

        AiTempTokenVo analysisTempToken = aiModelBll.getAnalysisTempToken(askRequestBo.getAiModel(), null);
        String code = analysisTempToken == null ? askRequestBo.getAiModel() + "" : ObjUtil.defaultIfEmpty(analysisTempToken.getModelCode(), askRequestBo.getAiModel() + "");

        List<String> systemPromptKeys = askRequestBo.getSystemPromptKeys();

        String stringBuffer = askRequestBo.getSourceId() +
                askRequestBo.getSourceType() +
                askRequestBo.getType() +
                code +
                questionContent +
                JSONUtil.toJsonStr(ObjectUtil.defaultIfNull(askRequestBo.getVideoTimeOneList(), new ArrayList<>())) +
                JSONUtil.toJsonStr(ObjectUtil.defaultIfNull(askRequestBo.getVideoTimeTwoList(), new ArrayList<>())) +
                (CollUtil.isEmpty(systemPromptKeys) ? "" : String.join(",", systemPromptKeys));
                ;
        String key = DigestUtil.md5Hex(stringBuffer);
        return RedisCacheKey.getRedisKey(RedisCacheKey.aiVideoContextIdNewCacheKey, askRequestBo.getUserId(), key);
    }

    @Override
    public Object getMarkDto() {
        String downloadUrl = sensitiveWordsBll.getAnalysisDownloadUrl(sourceType, sourceId);
        String content = "";
        if(StrUtil.isNotEmpty(downloadUrl)) {
            content = ReplayFileUtils.getFileContentByZipDownloadUrl(downloadUrl);
        }

        RRException.isNotEmpty(content, "获取全文失败");

        // 封装在线复盘信息
        return JSONObject.parseObject(content, AnalysisResultVo.class);
    }

    @Override
    public String getContent() {
        return "";
    }

    @Override
    public String getTextParamsContent() {
        return "";
    }

    @Override
    public String getAllContent() {
        return StrUtil.format("{}\n\n{}", getTextParamsContent(), getContent());
    }

    @Override
    public Long getTradeId() {
        return null;
    }

    @Override
    public String getTradeName() {
        if (ObjectUtil.isNotEmpty(trade)) return trade.getName();
        if (ObjectUtil.isNotEmpty(getTradeId())){
            R<TradeInfoVo> info = tradeBll.info(getTradeId());
            if (info.getCode() == 0 && ObjectUtil.isNotEmpty(info.getData())){
                this.trade = info.getData();
            }
        }
        return ObjectUtil.isNotEmpty(trade) ? trade.getName() : "";
    }

    @Override
    public String getAnchorName() {
        return "";
    }

    @Override
    public String getDataScreenshot() {
        if (ObjectUtil.isNotEmpty(dataScreenshot)) return dataScreenshot;
        StringBuilder str = new StringBuilder();
        List<DataScreenshotListVo> list = dataScreenshotList();
        if (ObjectUtil.isNotEmpty(list)){
            for (int i = 0; i < list.size(); i++) {
                DataScreenshotListVo dataScreenshot = list.get(i);
                str.append(StrUtil.format("数据{}、\n{}\n", i + 1, dataScreenshot.getAiContent()));
            }
        }
        this.dataScreenshot = str.toString();
        return str.toString();
    }

    /**
     * 获取数据截图
     * @return
     */
    protected List<DataScreenshotListVo> dataScreenshotList(){
        R<List<DataScreenshotListVo>> listR = dataScreenshotBll.getExistDataScreenshotList(sourceType, sourceId);
        if (listR.getCode() == 0 && ObjectUtil.isNotEmpty(listR.getData())){
            return listR.getData().stream().filter(item -> ObjectUtil.isNotEmpty(item.getAiContent())).toList();
        }
        return new ArrayList<>();
    }

    @Override
    public String getBoard() {
        if (ObjectUtil.isNotEmpty(board)) return board;
        String str = "";
        R<VideoDataViewingConfuseInfoVo> videoDataR = videoDataViewingBll.infoByVideoIdPriorityParagraph(sourceId);
        if (videoDataR.getCode() == StatusCode.SUCCESS.getCode() && videoDataR.getData() != null) {
            VideoDataViewingConfuseInfoVo data = videoDataR.getData();
//            String temp = "";
//            if (ObjectUtil.isNotEmpty(data.getTotalWatchNum())) temp += StrUtil.format("总观看人次:{}  ", data.getTotalWatchNum());
//            if (ObjectUtil.isNotEmpty(data.getAverageOnlineNum())) temp += StrUtil.format("平均在线人数:{}  ", data.getAverageOnlineNum());
//            if (ObjectUtil.isNotEmpty(data.getAverageResidenceTime())) temp += StrUtil.format("平均停留时间:{}秒  ", data.getAverageResidenceTime());
//            if (ObjectUtil.isNotEmpty(data.getIncrementFollowerCount())) temp += StrUtil.format("新增粉丝数:{}  ", data.getIncrementFollowerCount());
//            if (ObjectUtil.isNotEmpty(data.getConvertFanRate())) temp += StrUtil.format("粉丝转化率:{}  ", data.getConvertFanRate());
//            if (ObjectUtil.isNotEmpty(data.getInteractionPercent())) temp += StrUtil.format("互动率:{}  ", data.getInteractionPercent());

//            if (ObjectUtil.isNotEmpty(data.getVolumeStart())) temp += StrUtil.format("销售额:{}~{}元  ", data.getVolumeStart(), data.getVolumeEnd());
//            if (ObjectUtil.isNotEmpty(data.getPurchaseCountStart())) temp += StrUtil.format("销量:{}~{}  ", data.getPurchaseCountStart(), data.getPurchaseCountEnd());
//            if (ObjectUtil.isNotEmpty(data.getCustomerUnitPriceStart())) temp += StrUtil.format("客单价:{}~{}元  ", data.getCustomerUnitPriceStart(), data.getPurchaseCountEnd());
//            if (ObjectUtil.isNotEmpty(data.getUvValueStart())) temp += StrUtil.format("uv价值:{}~{}  ", data.getUvValueStart(), data.getUvValueEnd());
//            if (ObjectUtil.isNotEmpty(data.getGoodsConvertRateStart())) temp += StrUtil.format("带货转换率:{}~{}  ", data.getGoodsConvertRateStart(), data.getGoodsConvertRateEnd());
            str += data.formatStrForAi();
        }
        if (ObjectUtil.isNotEmpty(str)) {
            this.board = "看板数据：\n" + str;
        }
        return this.board;
    }

    @Override
    public List<String> getAdditionalList() {
        return new ArrayList<>();
    }

    @Override
    public String setAskQuestion(AskRequestBo askRequestBo) {
        String str = "";
        if (ObjectUtil.isNotEmpty(askRequestBo)){
            if (askRequestBo.getUseModelWay() == 0) {
                // 有上下文缓存
                if (StrUtil.isNotEmpty(askRequestBo.getParagraphCode()) && ObjectUtil.isEmpty(askRequestBo.getParagraphContent()))
                {
                    if (ObjectUtil.isEmpty(askRequestBo.getParagraphCode()) || "0".equals(askRequestBo.getParagraphCode()))
                    {
                        str = "\n以上问题针对直播脚本为全文。";
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
                        str = "\n以上问题针对直播脚本为全文。";
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
                        str = StrUtil.format("\n{}\n\n针对本次问题回答的范围为以下内容：\n{}", this.getTextParamsContent(), this.getContent());
                    }
                }
                else
                {
                    str = StrUtil.format("\n针对本次问题回答的范围为以下内容：\n{}", askRequestBo.getParagraphContent());
                }
            }
        }
        return str;
    }

    /**
     * 获取在线人数
     * @return
     */
    public List<OnlineNumInfoVo> getOnlineNumList(String batchNumber, Long userId, String videoId) {
        List<OnlineNumInfoVo> result = new ArrayList<>();
        R<List<OnlineNumInfoVo>> onlineRes = socketCollectMessageBll.getOnlineNumList(batchNumber, userId, videoId);
        if (onlineRes.getCode() == 0 && ObjectUtil.isNotEmpty(onlineRes.getData())){
            result = onlineRes.getData();
        }
        return result;
    }

    @Override
    public String getPlatform() {
        String name = "抖音";
        String platform = getPlatformValue();
        if (ObjectUtil.isNotEmpty(platform)) {
            DictDataListVo replayPlatformType = dictDataFeign.dictDataByValue("replay_platform_type", platform);
            if (replayPlatformType != null) {
                name = replayPlatformType.getLabel();
            }
        }
        return name;
    }

    @Override
    public void setSpeed(AskRequestBo askRequestBo) {
        Integer speed = getSpeed(askRequestBo);
        if (speed == null) return;
        if (ObjectUtil.isNotEmpty(askRequestBo.getOtherObj())) {
            Object backgroundConfig = askRequestBo.getOtherObj().getOrDefault("backgroundConfigOne", null);
            // 判断是否是map<string,object>类型的
            if (backgroundConfig instanceof Map) {
                Map<String, Object> backgroundConfigMap = (Map<String, Object>) backgroundConfig;
                backgroundConfigMap.put("speechRate", speed);
            }
        }
    }

    @Override
    public Integer getSpeed(AskRequestBo askRequestBo) {
        return null;
    }

    public String getPlatformValue() {
        return "1";
    }



    /**
     * 获取去掉标点的内容
     *
     * @param content 内容
     * @return 去掉标点的内容
     */
    public String removePunctuation(String content) {
        if (ObjectUtil.isEmpty(content)) {
            return "";
        }
        return content
                .replaceAll("，", "")
                .replaceAll("。", "")
                .replaceAll("、", "")
                .replaceAll("？", "");

    }

}
