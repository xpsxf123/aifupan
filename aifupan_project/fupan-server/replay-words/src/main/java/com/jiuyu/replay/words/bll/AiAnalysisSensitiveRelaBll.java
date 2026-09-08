package com.jiuyu.replay.words.bll;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

import com.jiuyu.replay.generic.vo.words.AiAnalysisSensitiveRelaListVo;
import com.jiuyu.replay.generic.vo.words.AiAnalysisSensitiveRelaInfoVo;
import com.jiuyu.replay.words.bo.AiAnalysisSensitiveRelaBo;
import com.jiuyu.replay.words.bo.AiAnalysisSensitiveRelaListBo;
import com.jiuyu.replay.words.producer.AiAnalysisSensitiveRelaProducer;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;


/**
 * AI分析关键词与记录关联关系表
 *
 * @author DearSil
 * @email 2437495924@qq.com
 * @date 2025-03-03 18:49:53
 */
@Component
public class AiAnalysisSensitiveRelaBll {

    @Resource
    private AiAnalysisSensitiveRelaProducer aiAnalysisSensitiveRelaProducer;


    /**
     * AI分析关键词与记录关联关系表列表
     * @param aiAnalysisSensitiveRelaListBo AI分析关键词与记录关联关系表列表查询参数
     * @return
     */
    public R<PageUtils<AiAnalysisSensitiveRelaListVo>> queryPage(AiAnalysisSensitiveRelaListBo aiAnalysisSensitiveRelaListBo) {

        return R.ok("获取成功", aiAnalysisSensitiveRelaProducer.queryPage(aiAnalysisSensitiveRelaListBo));
    }

    /**
    * AI分析关键词与记录关联关系表信息
    * @param id AI分析关键词与记录关联关系表id
    * @return
    */
    public R<AiAnalysisSensitiveRelaInfoVo> info(Long id) {

        AiAnalysisSensitiveRelaInfoVo aiAnalysisSensitiveRelaInfoVo = aiAnalysisSensitiveRelaProducer.info(id);
        return R.ok("获取成功", aiAnalysisSensitiveRelaInfoVo);
    }

    /**
     * 新增AI分析关键词与记录关联关系表
     * @param aiAnalysisSensitiveRelaBo AI分析关键词与记录关联关系表对象
     * @return
     */
    public R<String> save(AiAnalysisSensitiveRelaBo aiAnalysisSensitiveRelaBo) {

        AiAnalysisSensitiveRelaInfoVo aiAnalysisSensitiveRelaInfoVo = aiAnalysisSensitiveRelaProducer.save(aiAnalysisSensitiveRelaBo);
        return R.ok("添加成功");
    }

    /**
     * 修改AI分析关键词与记录关联关系表
     * @param aiAnalysisSensitiveRelaBo AI分析关键词与记录关联关系表对象
     * @return
     */
    public R<String> update(AiAnalysisSensitiveRelaBo aiAnalysisSensitiveRelaBo) {

        aiAnalysisSensitiveRelaProducer.update(aiAnalysisSensitiveRelaBo);
        return R.ok("修改成功");
    }

    /**
     * 删除AI分析关键词与记录关联关系表
     * @param id AI分析关键词与记录关联关系表id
     * @return
     */
    public R<String> delete(Long id) {

        aiAnalysisSensitiveRelaProducer.deleteById(id);
        return R.ok("删除成功");
    }


    /**
     * 批量保存AI分析关键词与记录关联关系
     * @param notMarkWordList 数据列表
     */
    public R<String> saveBatch(List<AiAnalysisSensitiveRelaBo> notMarkWordList) {

        aiAnalysisSensitiveRelaProducer.saveBatch(notMarkWordList);

        return R.ok("批量添加成功");
    }
}

