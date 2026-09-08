package com.jiuyu.replay.words.bll;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

import com.jiuyu.replay.words.vo.HistoryParagraphListVo;
import com.jiuyu.replay.words.vo.HistoryParagraphInfoVo;
import com.jiuyu.replay.words.bo.HistoryParagraphBo;
import com.jiuyu.replay.words.bo.HistoryParagraphListBo;
import com.jiuyu.replay.words.producer.HistoryParagraphProducer;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;


/**
 * ai问答的历史分析段落记录
 *
 * @author lj
 * @email 
 * @date 2025-03-22 15:36:36
 */
@Component
public class HistoryParagraphBll {

    @Resource
    private HistoryParagraphProducer historyParagraphProducer;


    /**
     * ai问答的历史分析段落记录列表
     * @param historyParagraphListBo ai问答的历史分析段落记录列表查询参数
     * @return
     */
    public R<PageUtils<HistoryParagraphListVo>> queryPage(HistoryParagraphListBo historyParagraphListBo) {

        return R.ok("获取成功", historyParagraphProducer.queryPage(historyParagraphListBo));
    }

    /**
    * ai问答的历史分析段落记录信息
    * @param id ai问答的历史分析段落记录id
    * @return
    */
    public R<HistoryParagraphInfoVo> info(Long id) {

        HistoryParagraphInfoVo historyParagraphInfoVo = historyParagraphProducer.info(id);
        return R.ok("获取成功", historyParagraphInfoVo);
    }

    /**
     * 新增ai问答的历史分析段落记录
     * @param historyParagraphBo ai问答的历史分析段落记录对象
     * @return
     */
    public R<HistoryParagraphInfoVo> save(HistoryParagraphBo historyParagraphBo) {

        HistoryParagraphInfoVo historyParagraphInfoVo = historyParagraphProducer.save(historyParagraphBo);
        return R.ok("添加成功", historyParagraphInfoVo);
    }

    /**
     * 修改ai问答的历史分析段落记录
     * @param historyParagraphBo ai问答的历史分析段落记录对象
     * @return
     */
    public R<String> update(HistoryParagraphBo historyParagraphBo) {

        historyParagraphProducer.update(historyParagraphBo);
        return R.ok("修改成功");
    }

    /**
     * 根据code获取ai问答的历史分析段落记录
     * @param type
     * @param sourceId
     * @param sourceType
     * @param code
     * @return
     */
    public R<HistoryParagraphInfoVo> getByCode(Integer type, String sourceId, Integer sourceType, String code){
        return R.ok("获取成功", historyParagraphProducer.getByCode(type, sourceId, sourceType, code));
    }

    /**
     * 删除ai问答的历史分析段落记录
     * @param id ai问答的历史分析段落记录id
     * @return
     */
    public R<String> delete(Long id) {

        historyParagraphProducer.deleteById(id);
        return R.ok("删除成功");
    }


    /**
     * 删除ai问答的历史分析段落记录
     * @param paragraphBo
     * @return
     */
    public R<String> deleteHistoryParagraph(HistoryParagraphBo paragraphBo) {
        historyParagraphProducer.deleteHistoryParagraph(paragraphBo);
        return R.ok("删除成功");
    }
}

