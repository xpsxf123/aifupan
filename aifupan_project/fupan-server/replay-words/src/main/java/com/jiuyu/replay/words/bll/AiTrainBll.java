package com.jiuyu.replay.words.bll;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.words.TradeListVo;
import com.jiuyu.replay.words.bo.AiTrainBo;
import com.jiuyu.replay.words.bo.AiTrainListBo;
import com.jiuyu.replay.words.constant.Constant;
import com.jiuyu.replay.words.producer.AiTrainProducer;
import com.jiuyu.replay.words.producer.AnchorVideoProducer;
import com.jiuyu.replay.words.producer.TradeProducer;
import com.jiuyu.replay.words.vo.*;
import com.jiuyu.replay.generic.vo.words.AnchorVideoInfoVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * AI训练
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-02-20 18:47:39
 */
@Component
public class AiTrainBll {

    @Resource
    private AiTrainProducer aiTrainProducer;
    @Resource
    private AnchorVideoProducer anchorVideoProducer;
    @Resource
    private TradeProducer tradeProducer;


    /**
     * AI训练列表
     * @param aiTrainListBo AI训练列表查询参数
     * @return
     */
    public R<PageUtils<AiTrainListVo>> queryPage(AiTrainListBo aiTrainListBo) {

        PageUtils<AiTrainListVo> pageUtils = aiTrainProducer.queryPage(aiTrainListBo);

        List<AiTrainListVo> records = pageUtils.getList();
        if(records != null && records.size() > 0) {

            List<String> videoIds = records.stream().map(AiTrainVo::getVideoId).toList();
            List<AnchorVideoInfoVo> videoInfoVos = anchorVideoProducer.listByVideoIds(videoIds);

            Set<Long> tradeIds = records.stream().map(AiTrainVo::getTradeId).collect(Collectors.toSet());
            List<TradeListVo> tradeListVos = tradeProducer.listByIds(tradeIds);

            for (AiTrainListVo aiTrainVo : records) {
                // 封装视频名称
                if(videoInfoVos != null && videoInfoVos.size() > 0) {
                    for (AnchorVideoInfoVo anchorVideoInfoVo : videoInfoVos) {
                        if(anchorVideoInfoVo.getVideoId().equals(aiTrainVo.getVideoId())) {
                            aiTrainVo.setVideoName(anchorVideoInfoVo.getVideoName());
                            break;
                        }
                    }
                }
                // 封装行业名称
                if(tradeListVos != null && tradeListVos.size() > 0) {
                    for (TradeListVo tradeListVo : tradeListVos) {
                        if(tradeListVo.getId().equals(aiTrainVo.getTradeId())) {
                            aiTrainVo.setTradeName(tradeListVo.getName());
                            break;
                        }
                    }
                }
            }

        }

        return R.ok("获取成功", pageUtils);
    }

    /**
    * AI训练信息
    * @param id AI训练id
    * @return
    */
    public R<AiTrainInfoVo> info(Long id) {

        AiTrainInfoVo aiTrainInfoVo = aiTrainProducer.info(id);
        return R.ok("获取成功", aiTrainInfoVo);
    }

    /**
     * 新增AI训练
     * @param aiTrainBo AI训练对象
     * @return
     */
    public R<String> save(AiTrainBo aiTrainBo) {

        AiTrainInfoVo aiTrainInfoVo = this.aiTrainProducer.getByVideoId(aiTrainBo.getVideoId());
        if(aiTrainInfoVo != null) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "每条视频只能训练一次");
        }

        AnchorVideoInfoVo videoInfoVo = this.anchorVideoProducer.getByVideoId(aiTrainBo.getVideoId());
        if(videoInfoVo == null || !videoInfoVo.getUserId().equals(aiTrainBo.getUserId())) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "数据不存在或没有权限");
        }
        aiTrainBo.setTradeId(videoInfoVo.getTradeId());
        aiTrainProducer.save(aiTrainBo);

        return R.ok("添加成功");
    }

    /**
     * 修改AI训练
     * @param aiTrainBo AI训练对象
     * @return
     */
    public R<String> update(AiTrainBo aiTrainBo) {

        aiTrainProducer.update(aiTrainBo);
        return R.ok("修改成功");
    }

    /**
     * 删除AI训练
     * @param id AI训练id
     * @return
     */
    public R<String> delete(Long id) {

        aiTrainProducer.deleteById(id);
        return R.ok("删除成功");
    }

    /**
     * 根据视频id获取AI训练信息
     * @param videoId 视频id
     * @return
     */
    public R<AiTrainInfoVo> infoByVideoId(String videoId) {
        AiTrainInfoVo aiTrainInfoVo = this.aiTrainProducer.getByVideoId(videoId);

        return R.ok(aiTrainInfoVo);
    }

    /**
     * 后台完成训练
     * @param id AI训练id
     * @return
     */
    public R<String> completeTrain(Long id) {
        this.aiTrainProducer.completeTrain(id);

        return R.ok("操作成功");
    }
}

