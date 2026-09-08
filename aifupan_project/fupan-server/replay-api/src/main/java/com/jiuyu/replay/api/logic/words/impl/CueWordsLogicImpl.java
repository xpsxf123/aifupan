package com.jiuyu.replay.api.logic.words.impl;

import cn.hutool.core.util.ObjectUtil;
import com.jiuyu.replay.api.logic.words.CueWordsLogic;
import com.jiuyu.replay.common.constant.Constant;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.words.CueWordsInfoVo;
import com.jiuyu.replay.generic.vo.words.CueWordsListVo;
import com.jiuyu.replay.generic.vo.words.TradeInfoVo;
import com.jiuyu.replay.words.bll.AnchorVideoBll;
import com.jiuyu.replay.words.bll.*;
import com.jiuyu.replay.generic.bo.words.CueWordsBo;
import com.jiuyu.replay.generic.bo.words.CueWordsListBo;
import com.jiuyu.replay.words.vo.*;
import com.jiuyu.replay.words.vo.file.UploadFileInfoVo;
import com.jiuyu.replay.generic.vo.words.AnchorVideoInfoVo;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;


/**
 * 提示词
 *
 * @author DearSil
 * @email 2437495924@qq.com
 * @date 2025-02-20 15:23:28
 */
@Service
public class CueWordsLogicImpl implements CueWordsLogic {

    @Resource
    private CueWordsBll cueWordsBll;

    @Resource
    private AnchorVideoBll videoBll;

    @Resource
    private TradeBll tradeBll;

    @Resource
    private SyncContrastBll syncContrastBll;

    @Autowired
    private UploadFileBll fileBll;

    @Override
    public R<PageUtils<CueWordsListVo>>  queryPage(CueWordsListBo cueWordsListBo) {
        //构建tradeid数据
        getTradesIdsByParam(cueWordsListBo);
        return cueWordsBll.queryPage(cueWordsListBo);
    }

    private CueWordsListBo getTradesIdsByParam(CueWordsListBo cueWordsListBo) {

        //如果没有传入sourceId就直接跳出
        if (ObjectUtil.isEmpty(cueWordsListBo.getSourceId())) {
            return cueWordsListBo;
        }

        //构建默认的行业信息
        Long tradeId = 1L;

        //如果当前类型为视频，则从文件数据获取信息
        if (com.jiuyu.replay.words.constant.Constant.CueSourceTypeEnum.VIDEO.getCode().equals(cueWordsListBo.getSourceType())) {
            R<AnchorVideoInfoVo> anchorVideoInfoVoR = videoBll.infoByVideoId(cueWordsListBo.getSourceId());
            if (ObjectUtil.isNotEmpty(anchorVideoInfoVoR) && ObjectUtil.isNotEmpty(anchorVideoInfoVoR.getData())) {
                tradeId = anchorVideoInfoVoR.getData().getTradeId();
            }
        }else if (com.jiuyu.replay.words.constant.Constant.CueSourceTypeEnum.FILE.getCode().equals(cueWordsListBo.getSourceType())) {
            R<UploadFileInfoVo> uploadFileInfoVoR = fileBll.infoByFileId(cueWordsListBo.getSourceId());
            if (ObjectUtil.isNotEmpty(uploadFileInfoVoR) && ObjectUtil.isNotEmpty(uploadFileInfoVoR.getData())) {
                tradeId = uploadFileInfoVoR.getData().getTradeId();
            }
        }else if (com.jiuyu.replay.words.constant.Constant.CueSourceTypeEnum.CONTRAST.getCode().equals(cueWordsListBo.getSourceType())) {
            R<SyncContrastInfoVo> syncContrastInfoVoR = syncContrastBll.infoByContrastId(cueWordsListBo.getSourceId());
            if (ObjectUtil.isNotEmpty(syncContrastInfoVoR) && ObjectUtil.isNotEmpty(syncContrastInfoVoR.getData())) {
                SyncContrastInfoVo data = syncContrastInfoVoR.getData();
                CueWordsListBo cueWordsListBo1 = new CueWordsListBo();
                if (ObjectUtil.isNotEmpty(data.getVideoOneId())){
                    cueWordsListBo1.setSourceId(data.getVideoOneId());
                    cueWordsListBo1.setSourceType(com.jiuyu.replay.words.constant.Constant.CueSourceTypeEnum.VIDEO.getCode());
                    CueWordsListBo tradesIdsByParam = getTradesIdsByParam(cueWordsListBo1);
                    cueWordsListBo.setTradeIds(tradesIdsByParam.getTradeIds());
                    return cueWordsListBo;
                }else if (ObjectUtil.isNotEmpty(data.getFileOneId())){
                    cueWordsListBo1.setSourceId(data.getFileOneId());
                    cueWordsListBo1.setSourceType(com.jiuyu.replay.words.constant.Constant.CueSourceTypeEnum.FILE.getCode());
                    CueWordsListBo tradesIdsByParam = getTradesIdsByParam(cueWordsListBo1);
                    cueWordsListBo.setTradeIds(tradesIdsByParam.getTradeIds());
                    return cueWordsListBo;
                }else{
                    return cueWordsListBo;
                }
            }
        }

        //获取当前行业信息包含所有的父级行业
        R<List<TradeInfoVo>> listR = tradeBll.listTradeIdsByTradeId(tradeId, Constant.GeneralEnum.GENERAL_NO.getCode());
        if (ObjectUtil.isNotEmpty(listR) && ObjectUtil.isNotEmpty(listR.getData())) {
            cueWordsListBo.setTradeIds(listR.getData().stream().map(TradeInfoVo::getId).sorted(Comparator.reverseOrder()).collect(Collectors.toList()));
        }

//      2.22 需求变更，不需要获取父级数据 根据视频数据获取所有父级行业数据
//        //根据videoId获取videoList
//        List<AnchorVideoInfoVo> videoList = getVideoListRByCueBo(cueWordsListBo);
//
//        if (ObjectUtil.isNotEmpty(videoList)) {
//            cueWordsListBo.setTradeIds(videoList.stream().map(AnchorVideoInfoVo::getTradeId).collect(Collectors.toList()));
//        }


//        R<List<TradeInfoVo>> tradeVoList = tradeBll.listTradeIdsByTradeIds(tradeIds, Constant.GeneralEnum.GENERAL_YES.getCode());

//        List<Long> trades = tradeVoList.getData().stream()
//                .map(TradeInfoVo::getId)
//                .collect(Collectors.toList());

        return cueWordsListBo;
    }


    private List<AnchorVideoInfoVo> getVideoListRByCueBo(CueWordsListBo cueWordsListBo) {

        //这个地方做分流处理
        /*
            如果传过来的sourceType = 0，就是单个视频
            如果传过来的sourceType = 1，就是文件
            根据不同的sourceType ,根据sourceId来获取对应的行业信息
         */
        List<String> vodeoIds = new ArrayList<>();

        //当前为单个视频
        if (Constant.GeneralEnum.GENERAL_NO.getCode() == cueWordsListBo.getSourceType()) {
            vodeoIds.add(cueWordsListBo.getSourceId());
        }

        //当前为多个视频
        if (Constant.GeneralEnum.GENERAL_YES.getCode() == cueWordsListBo.getSourceType()) {

            R<SyncContrastInfoVo> contrastInfo = syncContrastBll.info(Long.valueOf(cueWordsListBo.getSourceId()));

            if (ObjectUtil.isNotEmpty(contrastInfo) && ObjectUtil.isNotEmpty(contrastInfo.getData())) {
                if (ObjectUtil.isNotEmpty(contrastInfo.getData().getVideoOneId())) {
                    vodeoIds.add(contrastInfo.getData().getVideoOneId());
                }
                if (ObjectUtil.isNotEmpty(contrastInfo.getData().getVideoTwoId())) {
                    vodeoIds.add(contrastInfo.getData().getVideoTwoId());
                }
            }
        }

        if (vodeoIds.size() == 0) {
            return null;
        }
        return videoBll.listByVideoIds(vodeoIds).getData();
    }

    @Override
    public R<CueWordsInfoVo> info(Long id) {

        return cueWordsBll.info(id);
    }

    @Override
    public R<String> save(CueWordsBo cueWordsBo) {

        return cueWordsBll.save(cueWordsBo);
    }

    @Override
    public R<String> update(CueWordsBo cueWordsBo) {
        return cueWordsBll.update(cueWordsBo);
    }

    @Override
    public R<String> delete(Long id) {

        return cueWordsBll.delete(id);
    }


}

