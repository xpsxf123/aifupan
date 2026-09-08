package com.jiuyu.replay.words.api;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.jiuyu.replay.common.constant.AiEnums;
import com.jiuyu.replay.common.constant.Constant;
import com.jiuyu.replay.common.constant.WordsEnum;
import com.jiuyu.replay.generic.bo.words.CueWordsListBo;
import com.jiuyu.replay.generic.feign.words.CueWordsFeign;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.words.*;
import com.jiuyu.replay.generic.bo.words.cue.CueWordsPageBo;
import com.jiuyu.replay.generic.bo.words.cue.CueWordsQueryBo;
import com.jiuyu.replay.words.producer.*;
import com.jiuyu.replay.generic.vo.words.BasicSettingsVo;
import com.jiuyu.replay.words.vo.SyncContrastInfoVo;
import com.jiuyu.replay.words.vo.file.UploadFileInfoVo;
import lombok.AllArgsConstructor;
import org.checkerframework.checker.units.qual.A;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/5/30 上午11:33
 */
@Component
@AllArgsConstructor
public class CueWordsApi implements CueWordsFeign {

    private final CueWordsProducer cueWordsProducer;
    private final TradeProducer tradeProducer;
    private final AnchorVideoProducer anchorVideoProducer;
    private final UploadFileProducer uploadFileProducer;
    private final BasicSettingsProducer basicSettingsProducer;
    private final SyncContrastProducer syncContrastProducer;

    @Override
    public List<CueWordsInfoVo> listByIds(List<Long> ids) {
        return cueWordsProducer.listByIds(ids);
    }

    @Override
    public CueWordsInfoVo getById(Long id) {
        return cueWordsProducer.info(id);
    }

    @Override
    public PageUtils<CueWordsListVo> queryPage(CueWordsListBo cueWordsListBo) {
        return cueWordsProducer.queryPage(cueWordsListBo);
    }

    @Override
    public CueWordsInfoVo getHtmlPrompt(Long tradeId, String sourceId, Integer sourceType) {
        List<Long> tradeIds = new ArrayList<>();
        List<TradeInfoVo> vos = tradeProducer.listParentsByTradeId(tradeId, Constant.GeneralEnum.GENERAL_YES.getCode());
        if (ObjectUtil.isNotEmpty(vos)) {
            tradeIds = vos.stream().map(TradeInfoVo::getId).sorted(Comparator.reverseOrder()).toList();
        }

        CueWordsListBo bo = new CueWordsListBo();
        bo.setTradeIds(tradeIds);
        bo.setLimit(1);
        bo.setPage(1);
        bo.setCueType(AiEnums.askType.AI_HTML_PROMPT.getCode());
        bo.setSourceId(sourceId);
        bo.setSourceType(sourceType);
        PageUtils<CueWordsListVo> listP = cueWordsProducer.queryPage(bo);
        if (ObjectUtil.isEmpty(listP) || ObjectUtil.isEmpty(listP.getList())) {
            return null;
        }

        return BeanUtil.copyProperties(listP.getList().get(0), CueWordsInfoVo.class);
    }

    @Override
    public PageUtils<CueWordsListVo> pageCueWords(CueWordsPageBo pageBo) {
        setTradeCueWordsParams(pageBo);
        return cueWordsProducer.pageCueWords(pageBo);
    }

    @Override
    public CueWordsInfoVo getCueWordByTradeAndType(CueWordsQueryBo bo) {
        if (bo == null || bo.getCueType() == null) {
            throw new IllegalArgumentException("CueWordsQueryBo.cueType 必填");
        }
        // 1. 构造候选行业链：子→父优先 + 末尾兜底 trade_id=1
        List<Long> tradeIds = new ArrayList<>();
        if (ObjectUtil.isNotEmpty(bo.getTradeId())) {
            List<TradeInfoVo> tradeVos = tradeProducer.listParentsByTradeId(
                    bo.getTradeId(), Constant.GeneralEnum.GENERAL_NO.getCode());
            if (ObjectUtil.isNotEmpty(tradeVos)) {
                tradeVos.stream()
                        .map(TradeInfoVo::getId)
                        .sorted(Comparator.reverseOrder())
                        .forEach(tradeIds::add);
            }
        }
        if (!tradeIds.contains(1L)) {
            tradeIds.add(1L);
        }

        // 2. 一次 IN 查询取候选行业的全部有效提示词
        //    SQL 已按 tenant_id DESC, sort ASC 排序：同行业层级租户定制赢通用，sort 最小赢
        List<CueWordsInfoVo> all = cueWordsProducer.listByTradeIdsAndQuery(tradeIds, bo);
        if (ObjectUtil.isEmpty(all)) {
            return null;
        }

        // 3. 按行业分组取每组首条（SQL 已排序，首条即胜者；保留遇见顺序）
        Map<Long, CueWordsInfoVo> byTradeId = all.stream()
                .collect(Collectors.toMap(
                        CueWordsInfoVo::getTradeId,
                        java.util.function.Function.identity(),
                        (a, b) -> a));

        // 4. 按 tradeIds 顺序找第一个命中（子→父 → 兜底 1L）
        for (Long t : tradeIds) {
            CueWordsInfoVo hit = byTradeId.get(t);
            if (hit != null) {
                return hit;
            }
        }
        return null;
    }

    /**
     * 设置参数
     * scope、applyTo、accountType、syncScene
     *
     * @param pageBo 查询参数
     */
    public void setTradeCueWordsParams(CueWordsPageBo pageBo) {
        // 范围 0:全文，1:段落
        pageBo.setScope(0);

        // 获取行业和账号类型
        setTradeIdAndAccountType(pageBo);
        if (ObjectUtil.isEmpty(pageBo.getTradeId())) {
            pageBo.setTradeId(1L);
            pageBo.setCustomizeTradeId(1L);
        } else {
            pageBo.setCustomizeTradeId(pageBo.getTradeId());
        }

        // 提示词用于：0：单个分析，1：对比分析
        if (ObjectUtil.equals(pageBo.getSourceType(), WordsEnum.sourceType.SYNC_CONTRAST.getCode())) {
            pageBo.setAccountType(null);
            pageBo.setCustomizeAccountType(null);
            pageBo.setApplyTo(1);
        } else {
            pageBo.setSyncScene(null);
            pageBo.setApplyTo(0);
        }
        // 违规提示词不用区分账号归属
        if (ObjectUtil.equals(pageBo.getCueType(), AiEnums.askType.VIOLATION.getCode())) {
            pageBo.setAccountType(null);
            pageBo.setCustomizeAccountType(null);
        }

        // 获取行业层级
        List<TradeInfoVo> tradeVos = tradeProducer.listParentsByTradeId(pageBo.getTradeId(), Constant.GeneralEnum.GENERAL_YES.getCode());

        // 校验行业是否有对应的提示词
        // 当前账号为自有账号的时候，调用自有账号提示词
        // 当前账号为同行业账号的时候，
        //  A、如果有同行业提示词，调用同行业提示词
        //  B、当前没有同行业提示词，调用自有账号提示词
        if (!tradeVos.isEmpty()) {
            // 获取各个行业_账号类型对应的提示词数量
            Map<Long, Map<String, Integer>> tenantCountMap = cueWordsProducer.countByTenantParamsBatch(tradeVos.stream().map(TradeVo::getId).toList(), pageBo);
            // 通用提示词
            Map<String, Integer> stringIntegerMap = tenantCountMap.getOrDefault(0L, Map.of());
            // 定制提示词
            Map<String, Integer> customizeTradeMap = Map.of();
            if (ObjectUtil.isNotEmpty(pageBo.getTenantId())) {
                customizeTradeMap = tenantCountMap.getOrDefault(pageBo.getTenantId(), Map.of());
            }
            // 查询方式不一样，需要分别处理，
            // accountType 不为空时，优先查询 accountType 对应的提示词
            // accountType 为空时，查询 tradeId 对应的提示词,不查询accountType的提示词
            if (ObjectUtil.isNotEmpty(pageBo.getAccountType())) {
                pageBo.setCustomizeAccountType(pageBo.getAccountType());
                List<Integer> accountTypeList = ObjectUtil.equals(pageBo.getAccountType(), WordsEnum.accountType.OWN.getCode()) ?
                        List.of(WordsEnum.accountType.OWN.getCode()) : List.of(WordsEnum.accountType.PEER.getCode(), WordsEnum.accountType.OWN.getCode());
                AtomicBoolean universalFlag = new AtomicBoolean(true);
                AtomicBoolean customizeFlag = new AtomicBoolean(true);
                for (Integer account : accountTypeList) {
                    if (universalFlag.get()) {
                        for (TradeInfoVo tradeVo : tradeVos) {
                            long count = stringIntegerMap.getOrDefault(StrUtil.format("{}_{}", tradeVo.getId(), account), 0);
                            if (count > 0) {
                                pageBo.setAccountType(account);
                                pageBo.setTradeId(tradeVo.getId());
                                universalFlag.set(false);
                                break;
                            }
                        }
                    }

                    if (customizeFlag.get()) {
                        for (TradeInfoVo tradeVo : tradeVos) {
                            Integer customizeCount = customizeTradeMap.getOrDefault(StrUtil.format("{}_{}", tradeVo.getId(), account), 0);
                            if (customizeCount > 0) {
                                pageBo.setCustomizeTradeId(tradeVo.getId());
                                pageBo.setCustomizeAccountType(account);
                                customizeFlag.set(false);
                                break;
                            }
                        }
                    }
                }
            } else {
                Map<String, Integer> stringIntegerMapNew = new HashMap<>();
                stringIntegerMap.forEach((key, value) -> {
                    String[] keyArray = key.split("_");
                    // 累加到对应trade_id的计数中
                    stringIntegerMapNew.merge(keyArray[0], value, Integer::sum);
                });
                for (TradeInfoVo tradeVo : tradeVos) {
                    long count = stringIntegerMapNew.getOrDefault(StrUtil.format("{}", tradeVo.getId()), 0);
                    if (count > 0) {
                        pageBo.setTradeId(tradeVo.getId());
                        break;
                    }
                }
                Map<String, Integer> customizeTradeMapNew = new HashMap<>();
                customizeTradeMap.forEach((key, value) -> {
                    String[] keyArray = key.split("_");
                    // 累加到对应trade_id的计数中
                    customizeTradeMapNew.merge(keyArray[0], value, Integer::sum);
                });
                for (TradeInfoVo tradeVo : tradeVos) {
                    long count = customizeTradeMapNew.getOrDefault(StrUtil.format("{}", tradeVo.getId()), 0);
                    if (count > 0) {
                        pageBo.setCustomizeTradeId(tradeVo.getId());
                        break;
                    }
                }
            }


            if (ObjectUtil.isEmpty(pageBo.getTradeId())) {
                pageBo.setTradeId(1L);
                pageBo.setAccountType(null);
            }
            if (ObjectUtil.isEmpty(pageBo.getCustomizeTradeId())) {
                pageBo.setCustomizeTradeId(1L);
                pageBo.setCustomizeAccountType(null);
            }
        }
    }

    /**
     * 获取对应的行业id
     *
     * @param pageBo 查询参数
     * @return 行业id
     */
    private void setTradeIdAndAccountType(CueWordsPageBo pageBo) {
        if (ObjectUtil.isEmpty(pageBo.getSourceId())) {
            return;
        }
        //如果当前类型为视频，则从文件数据获取信息
        if (ObjectUtil.equals(WordsEnum.sourceType.VIDEO.getCode(), pageBo.getSourceType())) {
            AnchorVideoInfoVo anchorVideo = anchorVideoProducer.getByVideoId(pageBo.getSourceId());
            if (ObjectUtil.isNotEmpty(anchorVideo)) {
                pageBo.setTradeId(anchorVideo.getTradeId());
            }
            // 设置账号归属类型
            BasicSettingsVo basicSettingsVo = basicSettingsProducer.getBySourceUser(anchorVideo.getSecUid(), WordsEnum.basicSettingsType.ANCHOR.getCode(), anchorVideo.getUserId(), anchorVideo.getTenantId());
            if (ObjectUtil.isNotEmpty(basicSettingsVo)) {
                pageBo.setAccountType(basicSettingsVo.getAccountType());
            }
        } else if (ObjectUtil.equals(WordsEnum.sourceType.FILE.getCode(), pageBo.getSourceType())) {
            UploadFileInfoVo uploadFile = uploadFileProducer.getByFileId(pageBo.getSourceId());
            if (ObjectUtil.isNotEmpty(uploadFile)) {
                pageBo.setTradeId(uploadFile.getTradeId());
                // 设置账号归属类型
                BasicSettingsVo basicSettingsVo = basicSettingsProducer.getBySourceUser(pageBo.getSourceId(), WordsEnum.basicSettingsType.FILE.getCode(), uploadFile.getUserId(), uploadFile.getTenantId());
                if (ObjectUtil.isNotEmpty(basicSettingsVo)) {
                    pageBo.setAccountType(basicSettingsVo.getAccountType());
                }
            }
        } else if (ObjectUtil.equals(WordsEnum.sourceType.SYNC_CONTRAST.getCode(), pageBo.getSourceType())) {
            SyncContrastInfoVo syncContrast = syncContrastProducer.infoByContrastId(pageBo.getSourceId());
            if (ObjectUtil.isNotEmpty(syncContrast)) {
                CueWordsPageBo wordsPageBo = new CueWordsPageBo();
                if (ObjectUtil.isNotEmpty(syncContrast.getVideoOneId())) {
                    wordsPageBo.setSourceId(syncContrast.getVideoOneId());
                    wordsPageBo.setSourceType(WordsEnum.sourceType.VIDEO.getCode());
                } else if (ObjectUtil.isNotEmpty(syncContrast.getFileOneId())) {
                    wordsPageBo.setSourceId(syncContrast.getFileOneId());
                    wordsPageBo.setSourceType(WordsEnum.sourceType.FILE.getCode());
                }
                setTradeIdAndAccountType(wordsPageBo);
                pageBo.setTradeId(wordsPageBo.getTradeId());
                pageBo.setSyncScene(syncContrast.getSyncScene());
//                pageBo.setAccountType(wordsPageBo.getAccountType());
            }
        }
    }
}
