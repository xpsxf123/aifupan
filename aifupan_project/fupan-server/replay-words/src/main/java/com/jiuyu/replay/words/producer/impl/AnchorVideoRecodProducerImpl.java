package com.jiuyu.replay.words.producer.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.vo.words.AiAnalysisSensitiveRelaInfoVo;
import com.jiuyu.replay.generic.vo.words.AnchorVideoVO;
import com.jiuyu.replay.words.bo.AnchorVideoBo;
import com.jiuyu.replay.words.entity.*;
import com.jiuyu.replay.words.producer.AnchorVideoRecodProducer;
import com.jiuyu.replay.words.repository.service.*;
import com.jiuyu.replay.words.vo.*;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * @author tisheng
 * @date 2024/9/11
 * @apinNote
 */
@Service
public class AnchorVideoRecodProducerImpl implements AnchorVideoRecodProducer {

    @Resource
    private AnchorVideoRecodService anchorVideoRecodService;
    @Resource
    private TradeService tradeService;
    @Resource
    private AnchorUrlService anchorUrlService;
    @Resource
    AudioAnalysisService audioAnalysisService;
    @Resource
    AnchorVideoService anchorVideoService;
    @Resource
    VideoAnalysisRecordService videoRecordService;
    @Resource
    private AiAnalysisSensitiveRelaService aiAnalysisSensitiveRelaService;

    /**
     * 获取分析记录
     *
     * @param anchorVideoVO
     * @return
     */
    @Override
    public R<PageUtils<AnchorVideoVO>> list(AnchorVideoBo anchorVideoVO) {

        Page page2 = new Page(anchorVideoVO.getPage(), anchorVideoVO.getLimit());
        IPage<AnchorVideoVO> pageList = anchorVideoService.pageList(page2, anchorVideoVO);

        PageUtils<AnchorVideoVO> pageUtils = new PageUtils<>(pageList.getRecords(), (int) pageList.getTotal(), anchorVideoVO.getLimit(), anchorVideoVO.getPage());
        List<AnchorVideoVO> records = pageList.getRecords();
        if (records != null && !records.isEmpty()) {
            //行业
            List<Long> tradeIds = records.stream().map(AnchorVideoVO::getTradeId).toList();
            List<TradeEntity> tradelist = tradeService.list(new QueryWrapper<TradeEntity>().in("id", tradeIds));
            //视频信息
            List<String> videoIds = records.stream().map(AnchorVideoVO::getVideoId).toList();
            //主播信息
            List<String> secUids = records.stream().map(AnchorVideoVO::getSecUid).toList();
            List<AnchorUrlEntity> secUidList = anchorUrlService.list(new QueryWrapper<AnchorUrlEntity>().in("sec_uid", secUids));

            // 获取未在词库的词语列表
            List<AiAnalysisSensitiveRelaEntity> analysisSensitiveRelaEntities = this.aiAnalysisSensitiveRelaService.list(new QueryWrapper<AiAnalysisSensitiveRelaEntity>().in("uuid", videoIds).eq("record_type", 0));


            //判断是否分析完成
//            List<AnchorVideoEntity> statusList = anchorVideoService.list(new QueryWrapper<AnchorVideoEntity>().eq("analysis_status", 2));
//            List<String> analysisIds = statusList.stream().map(AnchorVideoEntity::getVideoId).toList();

//            QueryWrapper<AudioAnalysisEntity> entityQueryWrapper = new QueryWrapper<>();
//            entityQueryWrapper.in("video_id", analysisIds).select("version","video_id");
//            entityQueryWrapper.ne("version", 0);
//            List<AudioAnalysisEntity> analysisEntities = this.audioAnalysisService.list(entityQueryWrapper);

            List<AnchorVideoVO> list = records.stream().map(item -> {
                AnchorVideoVO vo = new AnchorVideoVO();
                BeanUtils.copyProperties(item, vo);
                //设置分析次数
                List<VideoAnalysisRecordEntity> recordEntities = this.videoRecordService.list(new QueryWrapper<VideoAnalysisRecordEntity>().eq("video_id",item.getVideoId()).select("version"));
                if (recordEntities != null && recordEntities.size() > 0) {
                    vo.setAnalysisStatusVersion(recordEntities.size());
                }else{
                    vo.setAnalysisStatusVersion(0);
                }

                //填充主播名称
                List<AnchorUrlEntity> list2 = secUidList.stream().filter(entity -> entity.getSecUid().equals(item.getSecUid())).toList();
                if (list2 != null && list2.size() > 0) {
                    vo.setAnchorName(list2.get(0).getAnchorName());
                }
                //行业名称
                List<TradeEntity> list1 = tradelist.stream().filter(entity -> entity.getId().equals(item.getTradeId())).toList();
                if (list1 != null && list1.size() > 0) {
                    vo.setTradeName(list1.get(0).getName());
                }
//                //视频大小，单位：B
//                List<AnchorVideoEntity> list3 = anchorVideoList.stream().filter(entity -> entity.getVideoId().equals(item.getVideoId())).toList();
//                if (list3 != null && list3.size() > 0) {
//                    vo.setVedioSizie(list3.get(0).getVedioSizie());
//                }
                // 未在词库的词语
                List<AiAnalysisSensitiveRelaInfoVo> notMarkWordList = new LinkedList<>();
                if(analysisSensitiveRelaEntities != null && analysisSensitiveRelaEntities.size() > 0) {
                    for (AiAnalysisSensitiveRelaEntity analysisSensitiveRelaEntity : analysisSensitiveRelaEntities) {
                        if(analysisSensitiveRelaEntity.getUuid().equals(vo.getVideoId())) {
                            AiAnalysisSensitiveRelaInfoVo aiAnalysisSensitiveRelaInfoVo = new AiAnalysisSensitiveRelaInfoVo();
                            BeanUtils.copyProperties(analysisSensitiveRelaEntity, aiAnalysisSensitiveRelaInfoVo);
                            notMarkWordList.add(aiAnalysisSensitiveRelaInfoVo);
                        }
                    }
                }
                vo.setNotMarkWordList(notMarkWordList);
                return vo;
            }).toList();
            pageUtils.setList(list);
        }
        return R.ok(pageUtils);
    }

    /**
     * 保存分析记录
     *
     * @param audioAnalysisVo
     * @return
     */
    @Override
    public R<String> save(List<AudioAnalysisVo> audioAnalysisVo) {

        if (audioAnalysisVo != null && audioAnalysisVo.size() > 0) {
            List<AudioAnalysisEntity> list = new ArrayList<>();

            QueryWrapper<AudioAnalysisEntity> queryWrappe = new QueryWrapper<>();
            queryWrappe.eq("video_id", audioAnalysisVo.get(0).getVideoId());
            queryWrappe.eq("trade_id", audioAnalysisVo.get(0).getTradeId())
                    .orderByDesc("version").last("limit 1");
            AudioAnalysisEntity oldAnalysis = audioAnalysisService.getOne(queryWrappe);

            int version = 0;
            if (oldAnalysis != null) {
                version = oldAnalysis.getVersion() + 1;
            }

            for (AudioAnalysisVo item : audioAnalysisVo) {
                AudioAnalysisEntity audioAnalysisEntity = new AudioAnalysisEntity();
                BeanUtils.copyProperties(item, audioAnalysisEntity);
                audioAnalysisEntity.setId(SnowflakeManager.nextValue());
                audioAnalysisEntity.setVersion(version);
                audioAnalysisEntity.setCreateDate(new Date());
                list.add(audioAnalysisEntity);
            }
            audioAnalysisService.saveBatch(list);

            QueryWrapper<AnchorVideoEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("video_id", audioAnalysisVo.get(0).getVideoId());
            AnchorVideoEntity one = anchorVideoService.getOne(queryWrapper);
            AnchorVideoRecodEntity anchorVideoRecodEntity = new AnchorVideoRecodEntity();
            BeanUtils.copyProperties(one, anchorVideoRecodEntity);
            anchorVideoRecodEntity.setId(SnowflakeManager.nextValue());
            anchorVideoRecodEntity.setCreateDate(new Date());
            anchorVideoRecodService.save(anchorVideoRecodEntity);
        }
        return R.ok("保存成功");
    }

    /**
     * 服务端用户详情查询分析记录
     *
     * @param anchorVideoBo
     * @return
     */
    @Override
    public R<PageUtils<AnchorVideoRecodListVo>> selectVideoRecod(AnchorVideoBo anchorVideoBo) {
        QueryWrapper<AnchorVideoRecodEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", anchorVideoBo.getUserId());

        IPage<AnchorVideoRecodEntity> page = anchorVideoRecodService.page(new Query<AnchorVideoRecodEntity>().getPage(anchorVideoBo.getPage(), anchorVideoBo.getLimit()), queryWrapper);
        PageUtils<AnchorVideoRecodListVo> objectPageUtils = new PageUtils<>(anchorVideoBo.getPage(), anchorVideoBo.getLimit(), page);
        List<AnchorVideoRecodEntity> records = page.getRecords();


        if (records != null && records.size() > 0) {
            List<String> listSerUid = records.stream().map(AnchorVideoRecodEntity::getSecUid).toList();
            QueryWrapper<AnchorUrlEntity> queryWrapper2 = new QueryWrapper<>();
            queryWrapper2.in("sec_uid", listSerUid);
            List<AnchorUrlEntity> list2 = anchorUrlService.list(queryWrapper2);
            List<AnchorVideoRecodListVo> list1 = records.stream().map(item -> {

                QueryWrapper<AnchorVideoEntity> queryWrapper3 = new QueryWrapper<>();
                queryWrapper3.eq("video_id", item.getVideoId());
                List<AnchorVideoEntity> list3 = anchorVideoService.list(queryWrapper3);

                AnchorVideoRecodListVo anchorVideoVO = new AnchorVideoRecodListVo();

                BeanUtils.copyProperties(list3.get(0), anchorVideoVO);
                anchorVideoVO.setAnchorName(list2.stream()
                        .filter(entity -> entity.getSecUid().equals(item.getSecUid()))
                        .findFirst().get().getAnchorName());
                //填充行也
                TradeEntity byId = tradeService.getById(item.getTradeId());
                anchorVideoVO.setTradeName(byId.getName());
                return anchorVideoVO;
            }).toList();
            objectPageUtils.setList(list1);
        }
        return R.ok(objectPageUtils);
    }


}
