package com.jiuyu.replay.words.bll;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bo.AiAnalysisRecordBo;
import com.jiuyu.replay.words.bo.AiAnalysisRecordListBo;
import com.jiuyu.replay.words.entity.AiAnalysisRecordEntity;
import com.jiuyu.replay.words.producer.AiAnalysisRecordProducer;
import com.jiuyu.replay.words.vo.AiAnalysisRecordInfoVo;
import com.jiuyu.replay.words.vo.AiAnalysisRecordListVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;


/**
 * AI分析出来的关键词总数和未匹配上词库的关键词个数
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-02-21 17:06:53
 */
@Component
public class AiAnalysisRecordBll {

    @Resource
    private AiAnalysisRecordProducer aiAnalysisRecordProducer;

    /**
     * AI分析出来的关键词总数和未匹配上词库的关键词个数列表
     * @param aiAnalysisRecordListBo AI分析出来的关键词总数和未匹配上词库的关键词个数列表查询参数
     * @return
     */
    public R<PageUtils<AiAnalysisRecordListVo>> queryPage(AiAnalysisRecordListBo aiAnalysisRecordListBo) {
        return R.ok("获取成功", aiAnalysisRecordProducer.queryPage(aiAnalysisRecordListBo));
    }

    /**
    * AI分析出来的关键词总数和未匹配上词库的关键词个数信息
    * @param id AI分析出来的关键词总数和未匹配上词库的关键词个数id
    * @return
    */
    public R<AiAnalysisRecordInfoVo> info(Long id) {

        AiAnalysisRecordInfoVo aiAnalysisRecordInfoVo = aiAnalysisRecordProducer.info(id);
        return R.ok("获取成功", aiAnalysisRecordInfoVo);
    }

    /**
     * 新增AI分析出来的关键词总数和未匹配上词库的关键词个数
     * @param aiAnalysisRecordBo AI分析出来的关键词总数和未匹配上词库的关键词个数对象
     * @return
     */
    public R<String> save(AiAnalysisRecordBo aiAnalysisRecordBo) {

        AiAnalysisRecordInfoVo aiAnalysisRecordInfoVo = aiAnalysisRecordProducer.save(aiAnalysisRecordBo);
        return R.ok("添加成功");
    }

    /**
     * 修改AI分析出来的关键词总数和未匹配上词库的关键词个数
     * @param aiAnalysisRecordBo AI分析出来的关键词总数和未匹配上词库的关键词个数对象
     * @return
     */
    public R<String> update(AiAnalysisRecordBo aiAnalysisRecordBo) {

        aiAnalysisRecordProducer.update(aiAnalysisRecordBo);
        return R.ok("修改成功");
    }

    /**
     * 删除AI分析出来的关键词总数和未匹配上词库的关键词个数
     * @param id AI分析出来的关键词总数和未匹配上词库的关键词个数id
     * @return
     */
    public R<String> delete(Long id) {

        aiAnalysisRecordProducer.deleteById(id);
        return R.ok("删除成功");
    }


    public List<AiAnalysisRecordEntity> allList() {
        return aiAnalysisRecordProducer.allList();
    }

    public List<AiAnalysisRecordEntity> allListByVideoIds(List<String> videoIds) {
        return aiAnalysisRecordProducer.allListByVideoIds(videoIds);
    }
}

