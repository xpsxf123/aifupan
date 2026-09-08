package com.jiuyu.replay.api.logic.ai.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONUtil;
import com.jiuyu.replay.common.constant.RedisCacheKey;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.third.bll.TableStoreBll;
import com.jiuyu.replay.generic.bo.third.QueryDanMuBo;
import com.jiuyu.replay.third.vo.DanMuVo;
import com.jiuyu.replay.third.vo.QueryDanMuVo;
import com.jiuyu.replay.words.bo.AskRequestBo;
import com.jiuyu.replay.words.vo.HistoryParagraphInfoVo;
import com.jiuyu.replay.generic.vo.words.AnchorVideoInfoVo;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Scope;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author ：lujie
 * @description：弹幕数据
 * @date ：2025/4/17 下午4:47
 */
@Component
@Scope("prototype")
public class BarrageSentenceImpl extends VideoSentenceImpl{

    private static final Integer askType = 2;
    // 设置弹幕缓存过期时间-2小时
    private static final long barrageTimout = 2 * 60 * 60;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private TableStoreBll tableStoreBll;

    @Override
    public void init(String sourceId, Integer askType, List<Long> videoTimeOneList, List<Long> videoTimeTwoList) {
        super.init(sourceId, sourceType, askType, videoTimeOneList, videoTimeTwoList);

        super.getAnchorVideoInfoVo();
    }


    @Override
    public String getContent() {
        if (content != null) return this.content;
        StringBuilder str = new StringBuilder();
        List<DanMuVo> danMuVos = barrageBoList();
        RRException.isNotEmpty(danMuVos, "弹幕数据为空不能发起ai提问");

        if (ObjectUtil.isNotEmpty(danMuVos)){
            int num = -1;
            if (otherParams.containsKey("singleMaxNum")) num = (int) otherParams.get("singleMaxNum");
            long startTime = anchorVideo.getStartTime().getTime();
            Map<String, Object> dataDisplay = (Map<String, Object>) otherParams.get("dataDisplay");
            if (ObjectUtil.isEmpty(dataDisplay)) dataDisplay = new HashMap<>();

            boolean dateTime = ((Integer) dataDisplay.getOrDefault("dateTime", 0)) == 1;
            boolean nickName = ((Integer) dataDisplay.getOrDefault("nickName", 0)) == 1;
            boolean level = ((Integer) dataDisplay.getOrDefault("level", 0)) == 1;
            boolean fansLevel = ((Integer) dataDisplay.getOrDefault("fansLevel", 0)) == 1;
            boolean isNew = ((Integer) dataDisplay.getOrDefault("isNew", 0)) == 1;

            for (DanMuVo danMuVo : danMuVos) {
                long time1 = danMuVo.getRecordDate() - startTime - TimeUnit.HOURS.toMillis(8);
                String temp = "";
                if (dateTime)
                {
                    temp += StrUtil.format("{} ", DateUtil.format(new Date(time1), "HH:mm:ss"));
                }
                if (nickName)
                {
                    temp += StrUtil.format("{}", danMuVo.getNickName());
                }

                if (level || fansLevel || isNew)
                {
                    List<String> tempStr = new ArrayList<>();
                    if (isNew)
                    {
                        String newStr = danMuVo.getIsNew() != null && danMuVo.getFansLevelMin() == 0 ? "新" : "";
                        if (ObjectUtil.isNotEmpty(newStr))
                        {
                            tempStr.add(newStr);
                        }
                    }
                    if (level)
                    {
                        tempStr.add(StrUtil.format("UL:{}", danMuVo.getLevel()));
                    }
                    if (fansLevel && ObjectUtil.defaultIfNull(danMuVo.getFansLevelCurrent(), 0L) > 0)
                    {
                        tempStr.add(StrUtil.format("FL:{}", danMuVo.getFansLevelCurrent()));
                    }
                    if(!tempStr.isEmpty())
                    {
                        temp += "(";
                        temp += StrUtil.join("，", tempStr);
                        temp += ")";
                    }
                }

                if (ObjectUtil.isNotEmpty(temp))
                {
                    temp += "：";
                }
                temp += danMuVo.getContent() + "\n";
                int tempNum = str.length() + temp.length();
                if (num != -1 && num - tempNum <= 0){
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
        Map<String, Object> dataDisplay = (Map<String, Object>) otherParams.get("dataDisplay");
        if (ObjectUtil.isEmpty(dataDisplay)) dataDisplay = new HashMap<>();

        boolean dateTime = ((Integer) dataDisplay.getOrDefault("dateTime", 0)) == 1;
        boolean nickName = ((Integer) dataDisplay.getOrDefault("nickName", 0)) == 1;
        boolean level = ((Integer) dataDisplay.getOrDefault("level", 0)) == 1;
        boolean fansLevel = ((Integer) dataDisplay.getOrDefault("fansLevel", 0)) == 1;
        boolean isNew = ((Integer) dataDisplay.getOrDefault("isNew", 0)) == 1;

        String str = "";
        str += StrUtil.format("\n文本内的参数说明：");
        str += StrUtil.format("\n一条完整的弹幕为一行，数据格式分为：");
        List<String> temps = new ArrayList<>();
        if (dateTime) temps.add("弹幕发送时间(HH:mm:ss)");
        if (nickName) temps.add("用户昵称");
        if (isNew) temps.add("新");
        if (level) temps.add("UL");
        if (fansLevel) temps.add("FL");
        temps.add("弹幕内容");
        str += StrUtil.join("、", temps);

        if (level || fansLevel)
        {
            str += "\n";
            temps = new ArrayList<>();
            if (level) temps.add("UL是用户等级的简称");
            if (fansLevel) temps.add("FL是用户粉丝团的简称");
            str += StrUtil.join(",", temps);
        }

        if (isNew) str += StrUtil.format("\n弹幕格式中如果有“新”字则是新用户，没有就是老用户");
        if (nickName) str += StrUtil.format("\n用户昵称是否隐藏判断：金***、建***、青***这种类型的昵称都是隐藏的，其他格式的都是不隐藏的");
        if (nickName && level) str += StrUtil.format("\n判断是否为用一个用户的方式：1、昵称没有隐藏就以\"用户昵称\"区分。2、昵称已经隐藏就以\"用户昵称+UL\"区分");
        textParamsContent = str;
        return textParamsContent;
    }

    private String getBarrageFormatHeader() {
        Map<String, Object> dataDisplay = (Map<String, Object>) otherParams.get("dataDisplay");
        if (ObjectUtil.isEmpty(dataDisplay)) dataDisplay = new HashMap<>();

        boolean dateTime = ((Integer) dataDisplay.getOrDefault("dateTime", 0)) == 1;
        boolean nickName = ((Integer) dataDisplay.getOrDefault("nickName", 0)) == 1;
        boolean level = ((Integer) dataDisplay.getOrDefault("level", 0)) == 1;
        boolean fansLevel = ((Integer) dataDisplay.getOrDefault("fansLevel", 0)) == 1;
        boolean isNew = ((Integer) dataDisplay.getOrDefault("isNew", 0)) == 1;

        StringBuilder sb = new StringBuilder("\n（格式：");
        if (dateTime) {
            sb.append("HH:mm:ss ");
        }
        if (nickName) {
            sb.append("用户昵称");
        }
        if (isNew || level || fansLevel) {
            sb.append("(");
            List<String> attrs = new ArrayList<>();
            if (isNew) attrs.add("新");
            if (level) attrs.add("UL");
            if (fansLevel) attrs.add("FL");
            sb.append(StrUtil.join("/", attrs));
            sb.append(")");
        }
        sb.append("：弹幕内容）");
        return sb.toString();
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
                        str = "\n以上问题针对直播的全部弹幕。";
                    }
                    else
                    {
                        R<HistoryParagraphInfoVo> byCode = historyParagraphBll.getByCode(askRequestBo.getType(), askRequestBo.getSourceId(), askRequestBo.getSourceType(), askRequestBo.getParagraphCode());
                        RRException.isNotEmpty(byCode.getData(), "获取历史段落失败", 7005);
                        String content = byCode.getData().getContent();
                        RRException.isNotEmpty(content, "获取历史段落失败", 7005);
                        str = StrUtil.format("\n本次回答内容不用考虑全部弹幕，除非提问中明确需要涉及全部弹幕相关内容做分析，否则只需要根据以下的弹幕内容进行回答：\n{}", content);
                    }
                }
                else
                {
                    if (StrUtil.isNotEmpty(askRequestBo.getParagraphContent()))
                    {
                        str = StrUtil.format("\n本次回答内容不用考虑全部弹幕，除非提问中明确需要涉及全部弹幕相关内容做分析，否则只需要根据以下的弹幕内容进行回答：\n{}", askRequestBo.getParagraphContent());
                    }
                    else
                    {
                        str = "\n以上问题针对直播的全部弹幕。";
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
                        str = StrUtil.format("\n针对本次问题回答的范围为以下弹幕内容：{}\n{}", getBarrageFormatHeader(), content);
                    }else {
                        str = StrUtil.format("\n{}\n\n针对本次问题回答的范围为以下弹幕内容：{}\n{}", this.getTextParamsContent(), getBarrageFormatHeader(), this.getContent());
                    }
                }
                else
                {
                    str = StrUtil.format("\n针对本次问题回答的范围为以下弹幕内容：{}\n{}", getBarrageFormatHeader(), askRequestBo.getParagraphContent());
                }
            }
        }
        return str;
    }

    private QueryDanMuBo getQueryDanMuBo(){
        QueryDanMuBo queryDanMuBo = BeanUtil.copyProperties(otherParams, QueryDanMuBo.class);
        queryDanMuBo.setLimit(-1);
        queryDanMuBo.setPage(1);
        queryDanMuBo.setQueryType(1);
        queryDanMuBo.setTenantId(getTenantId());
        queryDanMuBo.setUserId(getUserId());
        queryDanMuBo.setBatchNumber(getBatchNumber());
        queryDanMuBo.setVideoId(getVideoId());
        queryDanMuBo.setStartTime(getStartTime() + videoTimeOneList.get(0));
        queryDanMuBo.setEndTime(getStartTime() + videoTimeOneList.get(1));
        if (ObjectUtil.isEmpty(queryDanMuBo.getImportant())) {
            queryDanMuBo.setImportant(0);
        }
        return queryDanMuBo;
    }

    /**
     * 获取对应时间内的弹幕数据
     * @return
     */
    public List<DanMuVo> barrageBoList(){
        QueryDanMuBo queryDanMuBo = getQueryDanMuBo();

        String key = getBarrageRedisKey(queryDanMuBo);
        Object tempObj = redisTemplate.opsForValue().getAndExpire(key, barrageTimout, TimeUnit.SECONDS);
        if (ObjectUtil.isNotEmpty(tempObj)){
            return JSONUtil.toList((String) tempObj, DanMuVo.class);
        }

        R<QueryDanMuVo> queryDanMuVoR = tableStoreBll.queryDanMuSearchData(queryDanMuBo);
        List<DanMuVo> list = new ArrayList<>();
        if (queryDanMuVoR.getCode() == 0 && ObjectUtil.isNotEmpty(queryDanMuVoR.getData()) && ObjectUtil.isNotEmpty(queryDanMuVoR.getData().getList())){
            list = queryDanMuVoR.getData().getList();
        }

        redisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(list), barrageTimout, TimeUnit.SECONDS);
        return  queryDanMuVoR.getData().getList();
    }

    /**
     * 获取弹幕redis key
     * @return
     */
    private String getBarrageRedisKey(QueryDanMuBo queryDanMuBo){
        String stringBuffer = sourceId + sourceType + askType +
                JSONUtil.toJsonStr(queryDanMuBo);
        ;
        String key = DigestUtil.md5Hex(stringBuffer);

        return RedisCacheKey.getRedisKey(RedisCacheKey.barrageRedisKey, getUserId(), key);
    }

    /**
     * 获取用户id
     * @return
     */
    private Long getUserId(){
        return super.getAnchorVideoInfoVo().getUserId();
    }

    /**
     * 获取租户id
     * @return
     */
    private Long getTenantId(){
        return super.getAnchorVideoInfoVo().getTenantId();
    }

    /**
     * 获取批次号
     * @return
     */
    private String getBatchNumber(){
        return super.getAnchorVideoInfoVo().getBatchNumber();
    }

    /**
     * 获取视频id
     * @return
     */
    private String getVideoId() {
        return super.getAnchorVideoInfoVo().getVideoId();
    }

    /**
     * 获取视频开始时间
     * @return
     */
    private Long getStartTime(){
        AnchorVideoInfoVo anchorVideoInfoVo = super.getAnchorVideoInfoVo();
        RRException.isNotEmpty(anchorVideoInfoVo.getStartTime(), "获取视频开始时间失败");
        return anchorVideoInfoVo.getStartTime().getTime();
    }
}
