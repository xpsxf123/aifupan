package com.jiuyu.replay.api.logic.third.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.alibaba.excel.EasyExcel;
import com.jiuyu.replay.api.logic.third.TableStoreLogic;
import com.jiuyu.replay.common.constant.AiEnums;
import com.jiuyu.replay.common.constant.AnchorVideoEnums;
import com.jiuyu.replay.common.utils.CommonUtils;
import com.jiuyu.replay.common.utils.DataUtils;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.common.utils.ReplayFileUtils;
import com.jiuyu.replay.common.vo.DanMuExportVo;
import com.jiuyu.replay.generic.bo.ai.AiMessageBo;
import com.jiuyu.replay.generic.bo.ai.AiModelBo;
import com.jiuyu.replay.generic.bo.third.AiTokenUseRecordBo;
import com.jiuyu.replay.generic.bo.third.QueryDanMuBo;
import com.jiuyu.replay.generic.enums.words.VideoPlatformEnum;
import com.jiuyu.replay.generic.feign.words.BlessBagFeign;
import com.jiuyu.replay.generic.utils.ResultUtil;
import com.jiuyu.replay.generic.vo.ai.AiModelInfoVo;
import com.jiuyu.replay.generic.vo.ai.AiReturnDataVo;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.order.OrderInfoVo;
import com.jiuyu.replay.generic.vo.order.UserPropertyTypeInfoVo;
import com.jiuyu.replay.generic.vo.third.AiTokenUseRecordInfoVo;
import com.jiuyu.replay.generic.vo.words.*;
import com.jiuyu.replay.generic.vo.words.video.VideoDanMuExportDetailVo;
import com.jiuyu.replay.order.bll.AiTokenUseRecordBll;
import com.jiuyu.replay.order.bll.OrderBll;
import com.jiuyu.replay.order.bll.UserPropertyBll;
import com.jiuyu.replay.power.bll.TenantBll;
import com.jiuyu.replay.power.utils.GlobalObject;
import com.jiuyu.replay.power.vo.TenantInfoVo;
import com.jiuyu.replay.power.vo.UserCacheVo;
import com.jiuyu.replay.ai.factory.ModelFactoryUtils;
import com.jiuyu.replay.ai.model.AiModel;
import com.jiuyu.replay.third.bll.AiModelBll;
import com.jiuyu.replay.third.bll.TableStoreBll;
import com.jiuyu.replay.third.bo.*;
import com.jiuyu.replay.third.tablestore.entity.BarrageBo;
import com.jiuyu.replay.third.vo.DanMuVo;
import com.jiuyu.replay.third.vo.QueryDanMuVo;
import com.jiuyu.replay.third.vo.QueryOtherDanMuVo;
import com.jiuyu.replay.words.bll.*;
import com.jiuyu.replay.words.bo.video.AnchorVideoDetailBo;
import com.jiuyu.replay.words.producer.AnchorUrlProducer;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/1/14 下午2:29
 */
@Component
public class TableStoreLogicImpl implements TableStoreLogic {

    private static final Logger log = LoggerFactory.getLogger(TableStoreLogicImpl.class);
    @Resource
    private TableStoreBll tableStoreBll;
    @Resource
    private SocketCollectMessageBll socketCollectMessageBll;
    @Resource
    private AnchorVideoBll anchorVideoBll;
    @Resource
    private TenantBll tenantBll;
    @Resource
    private UserPropertyBll userPropertyBll;
    @Resource
    private OrderBll orderBll;
    @Resource
    private TradeBll tradeBll;
    @Resource
    private CueWordsBll cueWordsBll;
    @Resource
    private AiModelBll aiModelBll;
    @Resource
    private AiTokenUseRecordBll aiTokenUseRecordBll;
    @Resource
    private BlessBagFeign blessBagFeign;
    @Resource
    private AnchorVideoDetailBll anchorVideoDetailBll;
    @Autowired
    private AnchorUrlProducer anchorUrlProducer;

    @Override
    public R<String> uploadDanMuData(UploadDanMuDataBo danMuData, MultipartFile file) throws Exception {
        RRException.isNotEmpty(file, "上传文件不能为空");
        RRException.isNotEmpty(danMuData.getSecUid(), "主播id不能为空");
        RRException.isNotEmpty(danMuData.getVideoId(), "视频id不能为空");
        RRException.isNotEmpty(danMuData.getBatchNumber(), "场次号不能为空");
        UploadLocalDanMuDataBo data = BeanUtil.copyProperties(danMuData, UploadLocalDanMuDataBo.class);
        UserCacheVo user = GlobalObject.getLocalUser();
        RRException.isNotEmpty(user, "用户未登录");
        data.setUserId(user.getId());
        data.setTenantId(user.getActiveTenantId());

        R<AnchorVideoInfoVo> anchorVideoInfoVoR = anchorVideoBll.infoByVideoId(danMuData.getVideoId());
        AnchorVideoInfoVo video = null;
        if (anchorVideoInfoVoR.getCode() == 0 && ObjectUtil.isNotEmpty(anchorVideoInfoVoR.getData())) {
            video = anchorVideoInfoVoR.getData();
            R<TradeInfoVo> info = tradeBll.info(video.getTradeId());
            if (info.getCode() == 0 && ObjectUtil.isNotEmpty(info.getData())) {
                data.setTradeName(info.getData().getName());
                data.setTradeId(info.getData().getId());
            }
        }

        List<BarrageBo> list = parsingWebsocketFile(data, file);
        if (ObjectUtil.isEmpty(list)) return R.ok("上传成功");

        // 非正常拉取的视频（巨量拉取），弹幕昵称为"用户"的脱敏为"匿名用户"
        if (video != null && ObjectUtil.notEqual(video.getDataSource(), AnchorVideoEnums.dataSource.NORMAL.getCode())) {
            list.forEach(item -> {
                if ("用户".equals(item.getNickName())) {
                    item.setNickName("匿名用户");
                }
            });
        }
        data.setAiModelCode("analysis-doubao1.5-pro-32k-250115");
        // 设置重要的弹幕
//        setImportant(data, list);

        // 设置是否是福袋中的口令
        setIsBlessBag(data, list);

        // 设置弹幕数量
        socketCollectMessageBll.updateTotalBarrageNumByVideoId(danMuData.getBatchNumber().toString(), data.getUserId(), data.getVideoId(), list.size());

        return tableStoreBll.uploadDanMuData(data, list);
    }

    /**
     * 设置是否是福袋中的口令
     *
     * @param list
     */
    private void setIsBlessBag(UploadLocalDanMuDataBo danMuData, List<BarrageBo> list) {
        List<BlessBagInfoVo> blessBagList = blessBagFeign.listByVideoId(danMuData.getVideoId());
        if (ObjectUtil.isEmpty(blessBagList)) return;

        List<String> blessBagStrList = blessBagList.stream().map(item -> {
                    List<JSONObject> mapList = JSONUtil.toList(item.getConditions(), JSONObject.class);
                    return mapList.stream()
                            .filter(v -> v.getInt("type", 0) == 3)
                            .map(json -> json.getStr("content"))
                            .toList();
                })
                .flatMap(List::stream)
                .distinct()
                .toList();

        if (ObjectUtil.isNotEmpty(blessBagStrList)) {
            list.forEach(item -> item.setIsBlessBag(blessBagStrList.contains(item.getContent())));
        }
    }

    /**
     * 设置重要弹幕字段
     *
     * @param danMuData
     * @param list
     */
    private void setImportant(UploadLocalDanMuDataBo danMuData, List<BarrageBo> list) {
        try {
            if (ObjectUtil.isEmpty(list)) return;
            // 获取提示词
            R<CueWordsInfoVo> cueWordsInfoVoR = cueWordsBll.importantCueWordsByTradeId(danMuData.getTradeId());
            if (cueWordsInfoVoR.getCode() == 0 && ObjectUtil.isNotEmpty(cueWordsInfoVoR.getData())) {

                CueWordsInfoVo data = cueWordsInfoVoR.getData();


                // 处理占位符
                danMuData.setProblem("提问：" + CommonUtils.placeholderHandle(data.getProblem(), Map.of("trade", danMuData.getTradeName())));

                // 重要弹幕提示词的要求
                danMuData.setProblem(danMuData.getProblem() + "\n" + importantAdditionalStr());

                // 获取ai模型配置
                R<AiModelInfoVo> aiModelR = aiModelBll.getByCode(danMuData.getAiModelCode());
                if (aiModelR.getCode() != 0 || aiModelR.getData() == null) {
                    log.info("没有找到对应的ai模型配置，code={}", danMuData.getAiModelCode());
                    return;
                }
                AiModelInfoVo aiModel = aiModelR.getData();
                AiModelBo modelConfig = BeanUtil.copyProperties(aiModel, AiModelBo.class);

                // 获取ai模型
                AiModel aiModel1 = ModelFactoryUtils.getAiModel(aiModel.getResourceType());
                if (aiModel1 == null) return;

                AiMessageBo params = new AiMessageBo();
                params.setSystem(List.of(Map.of("text", "你是一个顶尖弹幕分析专家")));

                // 获取弹幕提示词，有多条问多次
                List<String> problemList = separateBarrageList(danMuData, modelConfig.getWordsNum(), list);

                if (ObjectUtil.isNotEmpty(problemList)) {
                    for (String problem : problemList) {
                        params.setUser(List.of(Map.of("text", problem)));
                        AiReturnDataVo aiReturnDataVo = aiModel1.chatCompletion(modelConfig, params, 0L);
                        log.info("获取的重要弹幕videoId = {} ,回答：{}", danMuData.getVideoId(), aiReturnDataVo.getContent());

                        // 记录aiToken消耗量
                        AiTokenUseRecordBo aiTokenUseRecordBo = AiTokenUseRecordBo.builder(aiReturnDataVo, modelConfig.getModelName());
                        aiTokenUseRecordBo.setTenantId(danMuData.getTenantId());
                        aiTokenUseRecordBo.setUserId(danMuData.getUserId());
                        aiTokenUseRecordBo.setModelName(modelConfig.getModelName());
                        aiTokenUseRecordBo.setUseSourceType(AiEnums.useSourceType.VIDEO.getCode());
                        aiTokenUseRecordBo.setUseSourceId(danMuData.getVideoId());
                        aiTokenUseRecordBo.setAssistantType(AiEnums.askType.IMPORTANT_SCREENSHOT.getCode());
                        R<AiTokenUseRecordInfoVo> save = aiTokenUseRecordBll.save(aiTokenUseRecordBo);

                        // 设置重要弹幕
                        setImportantValue(aiReturnDataVo.getContent(), list);
                    }
                }
            } else {
                log.info("行业ID：{}, 行业名称：{}，没有获取到提示词", danMuData.getTenantId(), danMuData.getTradeName());
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.info("获取重要弹幕报错：{}", e.getMessage());
        }
    }

    /**
     * 设置重要弹幕字段的值
     *
     * @param value ai返回的回答
     * @param list  所有的弹幕数据
     */
    private void setImportantValue(String value, List<BarrageBo> list) {
        if (ObjectUtil.isNotEmpty(value)) {
            String[] split = value.split(",");
            if (ObjectUtil.isNotEmpty(split)) {
                for (String v : split) {
                    if (ObjectUtil.isNotEmpty(v)) {
                        Integer i = NumberUtil.parseInt(v.trim(), -1);
                        if (i != -1 && i < list.size()) {
                            list.get(i).setImportant(1L);
                        }
                    }
                }
            }
        }
    }

    /**
     * 分割弹幕助手的提示词，以便于分多次去ai提问
     *
     * @param danMuData
     * @param maxProblemLength
     * @param list
     * @return
     */
    private List<String> separateBarrageList(UploadLocalDanMuDataBo danMuData, int maxProblemLength, List<BarrageBo> list) {
        ArrayList<String> result = new ArrayList<>();

        danMuData.setProblem(danMuData.getProblem() + "\n弹幕数据:\n");

        if (maxProblemLength == 0) maxProblemLength = -1;

        int maxOldLength = maxProblemLength == -1 ? -1 : maxProblemLength - danMuData.getProblem().length();
        int surplusLength = maxOldLength;
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            BarrageBo barrageBo = list.get(i);
            String format = StrUtil.format("{}:{}\n", i, barrageBo.getContent());

            if (maxOldLength != -1) {
                int temp = surplusLength - format.length();
                if (temp > 0) {
                    surplusLength = temp;
                } else {
                    result.add(danMuData.getProblem() + str);
                    str = new StringBuilder();
                    surplusLength = maxOldLength - format.length();
                }
            }
            str.append(format);
            if (i == list.size() - 1 && ObjectUtil.isNotEmpty(str.toString())) {
                result.add(danMuData.getProblem() + str);
            }
        }
        return result;
    }

    /**
     * 重要弹幕提示词的要求
     *
     * @return
     */
    public String importantAdditionalStr() {
        List<String> list = new ArrayList<>();
        list.add("筛选出重要序号");
        list.add("只要序号，不要有额外的说明");
        list.add("序号和序号之间有逗号分隔");
        StringBuilder str = new StringBuilder("要求：");
        for (int i = 0; i < list.size(); i++) {
            str.append(StrUtil.format("{}.{}。", i + 1, list.get(i)));
        }
        return str.toString();
    }

    /**
     * 解析弹幕文件
     *
     * @param danMuData
     * @param file
     * @return
     */
    private List<BarrageBo> parsingWebsocketFile(UploadLocalDanMuDataBo danMuData, MultipartFile file) {
        List<BarrageBo> result = new ArrayList<>();
        if (file != null) {
            // 从file中获取弹幕数据
            List<String> list = ReplayFileUtils.readTxtFileContentFromZip(file);
            // 把弹幕数据封装成BarrageBo
            Map<Long, BarrageBo> msgIdMap = new LinkedHashMap<>();
            list.stream()
                    .filter(s -> ObjectUtil.isNotEmpty(s) && s.contains(">>>"))
                    .forEach(s -> {
                        String[] split = s.split(">>>");
                        BarrageBo barrageBo = new BarrageBo();
                        barrageBo.setTenantId(danMuData.getTenantId());
                        barrageBo.setUserId(danMuData.getUserId());
                        barrageBo.setSecUid(danMuData.getSecUid());
                        barrageBo.setBatchNumber(NumberUtil.parseLong(danMuData.getBatchNumber(), 0L));
                        barrageBo.setVideoId(danMuData.getVideoId());
                        BarrageBo danmu = JSONUtil.toBean(split[1], BarrageBo.class);
                        barrageBo.setNickName(danmu.getNickName());
                        barrageBo.setIsNew(false);
                        barrageBo.setLevel(danmu.getLevel());
                        barrageBo.setFansLevelCurrent(danmu.getFansLevel());
                        barrageBo.setFansLevelMin(0L);
                        barrageBo.setFansLevelMax(0L);
                        barrageBo.setContent(danmu.getContent());
                        barrageBo.setMsgId(danmu.getMsgId());
                        barrageBo.setRecordDate(DateUtil.parse(split[0]).getTime());
                        barrageBo.setImportant(0L);
                        msgIdMap.put(danmu.getMsgId(), barrageBo);
                    });
            result = new ArrayList<>(msgIdMap.values());
        }
        if (ObjectUtil.isNotEmpty(result)) {
            result.sort(Comparator.comparing(BarrageBo::getRecordDate));
        }
        return result;
    }

    @Override
    public R<QueryDanMuVo> queryDanMuData(QueryDanMuBo queryDanMuBo) {
        // 参数校验
        RRException.isNotEmpty(queryDanMuBo, "查询参数不能够为空");
        RRException.isNotEmpty(queryDanMuBo.getBatchNumber(), "场次号不能够为空");
        RRException.isNotEmpty(queryDanMuBo.getVideoId(), "视频id不能为空");
        if (queryDanMuBo.getQueryType() == 2) {
            RRException.isNotEmpty(queryDanMuBo.getRecordDate(), "上下查询时，记录时间不能为空");
        } else if (queryDanMuBo.getQueryType() == 3) {
            RRException.isNotEmpty(queryDanMuBo.getNickName(), "昵称查询时昵称不能为空");
            queryDanMuBo.setRecordDate(null);
        } else if (queryDanMuBo.getQueryType() == 4) {
            queryDanMuBo.setQueryType(1);
            queryDanMuBo.setRecordDate(null);
            if (ObjectUtil.isNotEmpty(queryDanMuBo.getStartTime())) {
                queryDanMuBo.setRecordDate(queryDanMuBo.getStartTime());
            }
        }

        R<AnchorVideoInfoVo> anchorVideoInfoVoR = anchorVideoBll.GetByVideoId(queryDanMuBo.getVideoId());
        AnchorVideoInfoVo videoInfoVo = anchorVideoInfoVoR.getData();
        RRException.isTrue(ObjectUtil.isNotEmpty(anchorVideoInfoVoR) && ObjectUtil.isNotEmpty(videoInfoVo), "视频查询失败");

        if(!videoInfoVo.getPlatformType().equals(VideoPlatformEnum.DOUYIN.getCode())) {
            // 只有抖音平台的视频才有弹幕
            return R.ok();
        }

        if (ObjectUtil.isEmpty(queryDanMuBo.getTenantId())) {
            queryDanMuBo.setTenantId(videoInfoVo.getTenantId());
        }

        if (ObjectUtil.isEmpty(queryDanMuBo.getUserId())) {
            queryDanMuBo.setUserId(videoInfoVo.getUserId());
        }

        R<QueryDanMuVo> queryDanMuVoR = tableStoreBll.queryDanMuSearchData(queryDanMuBo);

        // 是否显示其他场次弹幕
        // 数据脱敏
        // 获取用户资产中的弹幕显示数量
        // 填充【在之前是否显示升级提示】字段
        if (queryDanMuVoR.getCode() == 0 && ObjectUtil.isNotEmpty(queryDanMuVoR.getData())) {
            QueryDanMuVo data = queryDanMuVoR.getData();
            // 获取视频的用户资产
            R<TenantInfoVo> info = tenantBll.info(anchorVideoInfoVoR.getData().getTenantId());
            if (info.getCode() == 0 && ObjectUtil.isNotEmpty(info.getData())) {
                TenantInfoVo tenantInfoVo = info.getData();
                List<UserPropertyTypeInfoVo> userPropertyList = userPropertyBll.getUserProperty(tenantInfoVo.getUserId());

                // 数据脱敏
                // 获取用户资产中的弹幕显示数量
                // 填充【在之前是否显示升级提示】字段
                UserPropertyTypeInfoVo barrageCount = userPropertyList.stream()
                        .filter(item -> item.getCommodityTypeCode().equals("barrageNum"))
                        .findFirst().orElse(null);
                long barrageNum;
                if (ObjectUtil.isNotEmpty(barrageCount)) {
                    barrageNum = barrageCount.getTotalQuantity();
                } else {
                    barrageNum = 0L;
                }
                data.getList().forEach(item -> {
                    // 数据脱敏
                    if (ObjectUtil.isNotEmpty(item.getNickName()) && item.getSort() > barrageNum) {
                        item.setNickName(item.getNickName().replaceAll(".", "*"));
                    }
                    // 填充【在之前是否显示升级提示】字段
                    if (item.getSort() == barrageNum + 1) {
                        item.setShowUpgradeTips(true);
                    }
                    // 是否脱敏
                    item.setIsDesensitization(false);
                    if (item.getSort() > barrageNum) {
                        item.setIsDesensitization(true);
                    }

                    // 新的用户判断，粉丝团等级要等于0，并且第一次发言
                    if (item.getIsNew()) {
                        item.setIsNew(item.getFansLevelMin() == 0);
                    }
                });
            }

            // 获取重要弹幕状态
            AnchorVideoDetailBo bo = new AnchorVideoDetailBo();
            bo.setVideoId(queryDanMuBo.getVideoId());
            R<AnchorVideoDetailInfoVo> andSave = anchorVideoDetailBll.getAndSave(bo);
            if (andSave.getCode() == 0 && ObjectUtil.isNotEmpty(andSave.getData())) {
                data.setImportantBarrageStatus(ObjectUtil.defaultIfNull(andSave.getData().getImportantBarrageStatus(), 0));
            }
        }
        return queryDanMuVoR;
    }

    @Override
    public R<VideoDanMuExportDetailVo> queryDanMuCount(QueryDanMuBo queryDanMuBo) {
        // 参数校验
        RRException.isNotEmpty(queryDanMuBo, "查询参数不能够为空");
        RRException.isNotEmpty(queryDanMuBo.getBatchNumber(), "场次号不能够为空");
        RRException.isNotEmpty(queryDanMuBo.getVideoId(), "视频id不能为空");
        AnchorVideoInfoVo videoInfoVo = ResultUtil.getResult(anchorVideoBll.GetByVideoId(queryDanMuBo.getVideoId()));
        RRException.isTrue(ObjectUtil.isNotEmpty(videoInfoVo), "视频查询失败");

        VideoDanMuExportDetailVo res = new VideoDanMuExportDetailVo();
        if (!videoInfoVo.getPlatformType().equals(VideoPlatformEnum.DOUYIN.getCode())) {
            // 只有抖音平台的视频才有弹幕
            return R.ok(res);
        }
        queryDanMuBo.setTenantId(videoInfoVo.getTenantId());
        queryDanMuBo.setUserId(videoInfoVo.getUserId());
        queryDanMuBo.setRecordDate(null);
        Long count = tableStoreBll.queryDanMuSearchCount(queryDanMuBo);
        res.setTotalCount(count);

        // 处理视频名
        String name = "";
        String videoName = videoInfoVo.getVideoName();
        if (StrUtil.isNotBlank(videoName)) {
            int af = videoName.indexOf("AF_");
            String sub = (af == -1 ? videoName : videoName.substring(af));
            String[] split = sub.split("_");
            name = split.length > 0 ? split[0] : sub;
        }

        int important = 0;
        if (ObjectUtil.isNotEmpty(queryDanMuBo.getImportant())) {
            important = queryDanMuBo.getImportant();
        }
        String fileName = StrUtil.format("{}_{}_{}.xlsx",
                DateUtil.format(new Date(), "yyyyMMddHHmmss"),
                name,
                important == 0 ? "所有弹幕" : "重要弹幕");
        res.setFileName(FileUtil.cleanInvalid(fileName));
        return R.ok(res);
    }

    @Override
    public R<List<QueryOtherDanMuVo>> queryOtherDanMuData(QueryOtherDanMuBo danMu) throws Exception {
        // 参数校验
        RRException.isNotEmpty(danMu, "查询参数不能够为空");
        R<AnchorVideoInfoVo> anchorVideoInfoVoR = anchorVideoBll.GetByVideoId(danMu.getVideoId());

        RRException.isTrue(ObjectUtil.isNotEmpty(anchorVideoInfoVoR) && ObjectUtil.isNotEmpty(anchorVideoInfoVoR.getData()), "视频查询失败");
        if (ObjectUtil.isEmpty(danMu.getTenantId())) {
            danMu.setTenantId(anchorVideoInfoVoR.getData().getTenantId());
        }

        if (ObjectUtil.isEmpty(danMu.getUserId())) {
            danMu.setUserId(anchorVideoInfoVoR.getData().getUserId());
        }
//        danMu.setBatchNumber(anchorVideoInfoVoR.getData().getBatchNumber().toString()); // 以场次维度
        danMu.setRecordDate(anchorVideoInfoVoR.getData().getStartTime().getTime());
        // 获取场次的时间
        R<List<QueryOtherDanMuVo>> resultR = tableStoreBll.queryOtherSearchData(danMu);
        if (resultR.getCode() == 0 && ObjectUtil.isNotEmpty(resultR.getData())) {
            List<QueryOtherDanMuVo> data = resultR.getData().stream().filter(item -> !item.getBatchNumber().equals(danMu.getVideoId())).toList(); // 以视频维度
//            List<QueryOtherDanMuVo> data = resultR.getData().stream().filter(item -> !item.getBatchNumber().equals(danMu.getBatchNumber())).toList(); // 以场次维度
            if (ObjectUtil.isNotEmpty(data)) {
                List<String> list = data.stream().map(QueryOtherDanMuVo::getBatchNumber).distinct().toList(); // 以视频维度
//                List<Long> list = data.stream().map(item -> NumberUtil.parseLong(item.getBatchNumber(), 0L)).distinct().toList(); // 以场次维度
                R<List<AnchorVideoInfoVo>> listR = anchorVideoBll.listByVideoIds(list);
                if (listR.getCode() == 0 && ObjectUtil.isNotEmpty(listR.getData())) {
                    DataUtils.setFieldNameById(data, "batchNumber", "batchNumberDate", listR.getData(), "videoId", "startTime"); // 以视频维度
//                    DataUtils.setFieldNameById(data, "batchNumber", "batchNumberDate", listR.getData(), "batchNumber", "startTime"); // 以场次维度
                }

                // 筛选data的数据，过滤batchNumberDate字段为null的数据，并按照batchNumberDate字段进行倒序排序
                data = data.stream()
                        .filter(item -> ObjectUtil.isNotEmpty(item.getBatchNumberDate()))
                        .sorted(Comparator.comparing(QueryOtherDanMuVo::getBatchNumberDate).reversed())
                        .toList();

                // 数据脱敏
                // 获取用户资产中的弹幕显示数量
                R<TenantInfoVo> info = tenantBll.info(anchorVideoInfoVoR.getData().getTenantId());
                if (info.getCode() == 0 && ObjectUtil.isNotEmpty(info.getData())) {
                    TenantInfoVo tenantInfoVo = info.getData();
                    List<UserPropertyTypeInfoVo> userPropertyList = userPropertyBll.getUserProperty(tenantInfoVo.getUserId());

                    // 数据脱敏
                    UserPropertyTypeInfoVo barrageCount = userPropertyList.stream()
                            .filter(item -> item.getCommodityTypeCode().equals("barrageNum"))
                            .findFirst().orElse(null);
                    int barrageNum;
                    if (ObjectUtil.isNotEmpty(barrageCount)) {
                        barrageNum = barrageCount.getTotalQuantity().intValue();
                    } else {
                        barrageNum = 0;
                    }

                    // 数据脱敏
                    data.forEach(item -> item.getList().forEach(value -> {
                        if (ObjectUtil.isNotEmpty(value.getNickName()) && value.getSort() > barrageNum) {
                            value.setNickName(value.getNickName().replaceAll(".", "*"));
                        }
                    }));
                }
            }
            resultR.setData(data);
        }

        return resultR;
    }

    @Override
    public R<Boolean> existsBarrage(UploadLocalDanMuDataBo bo) {
        RRException.isNotEmpty(bo, "上传弹幕数据不能为空");
        RRException.isNotEmpty(bo.getBatchNumber(), "直播场次号不能为空");
        RRException.isNotEmpty(bo.getVideoId(), "视频id不能为空");

        UserCacheVo user = GlobalObject.getLocalUser();
        RRException.isNotEmpty(user, "用户未登录");
        if (ObjectUtil.isEmpty(bo.getTenantId())) {
            bo.setTenantId(user.getActiveTenantId());
        }
        if (ObjectUtil.isEmpty(bo.getUserId())) {
            bo.setUserId(GlobalObject.getLocalUser().getId());
        }
        return tableStoreBll.existsBarrage(bo);
    }

    @Override
    public void queryDanMuExport(HttpServletResponse response, QueryDanMuExport queryDanMuExport) throws IOException {
        UserCacheVo user = GlobalObject.getLocalUser();
        if (user == null) {
            throw new RRException("用户未登录");
        }
        OrderInfoVo order = orderBll.orderByUserId(user.getId());
        if (order == null || order.getLevel() < 20) {
            throw new RRException("请升级到企业版及以上使用");
        }
        queryDanMuExport.setQueryType(1);
        queryDanMuExport.setLimit(-1);
        queryDanMuExport.setRecordDate(null);
        QueryDanMuVo danMuVo = ResultUtil.getResult(queryDanMuData(queryDanMuExport));
        if (danMuVo == null || ObjectUtil.isEmpty(danMuVo.getList())) {
            throw new RRException("没有弹幕数据无法导出");
        }

        List<DanMuExportVo> list = new ArrayList<>();
        for (DanMuVo muVo : danMuVo.getList()) {
            DanMuExportVo e = new DanMuExportVo();
            e.setUserName(muVo.getNickName());
            e.setDouYinLevel(String.valueOf(ObjectUtil.defaultIfNull(muVo.getLevel(), 0L)));
            e.setFansLevel(ObjectUtil.equal(muVo.getFansLevelMin(), 0L) && ObjectUtil.equal(0L, muVo.getFansLevelMax())
                    ? getFansStartLevel(muVo.getFansLevelMin())
                    : muVo.getFansLevelMin() + "-" + muVo.getFansLevelMax());
            e.setIsNew(muVo.getIsNew() ? "新" : "");
            e.setDanMuDate(DateUtil.format(new Date(muVo.getRecordDate()), "yyyy-MM-dd HH:mm:ss"));
            e.setContent(muVo.getContent());
            list.add(e);
        }

        // 响应头
        response.reset();
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String fileName = queryDanMuExport.getFileName();
        if (!fileName.endsWith(".xlsx")) fileName += ".xlsx";
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment; filename=" + encodedFileName);

        // 写出到响应流
        try {
            EasyExcel.write(response.getOutputStream(), DanMuExportVo.class)
                    .autoCloseStream(false)
                    .sheet("弹幕信息")
                    .doWrite(list);
            response.getOutputStream().flush();
            response.flushBuffer();
            response.getOutputStream().close();
        } catch (Exception e) {
            throw new IOException("导出Excel文件失败", e);
        }
    }

    /**
     * 获取粉丝团等级
     *
     * @param level 等级
     * @return 字符串的等级
     */
    private String getFansStartLevel(Long level) {
        if (level == null || level == 0) {
            return "无";
        }
        return String.valueOf(level);
    }
}
