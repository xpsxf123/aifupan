package com.jiuyu.replay.words.bll;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bo.IndustryKnowledgeBo;
import com.jiuyu.replay.words.bo.IndustryKnowledgeListBo;
import com.jiuyu.replay.words.constant.Constant;
import com.jiuyu.replay.words.producer.IndustryKnowledgeProducer;
import com.jiuyu.replay.words.vo.IndustryKnowledgeListVo;
import com.jiuyu.replay.words.vo.IndustryKnowledgeVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 系统行业知识库
 *
 * @author jxy
 * @date 2024-06-24
 */
@Component
public class IndustryKnowledgeBll {

    @Resource
    private IndustryKnowledgeProducer industryKnowledgeProducer;

    /**
     * 知识库列表
     *
     * @param listBo 列表查询参数
     * @return
     */
    public R<PageUtils<IndustryKnowledgeListVo>> queryPage(IndustryKnowledgeListBo listBo) {
        return R.ok("获取成功", industryKnowledgeProducer.queryPage(listBo));
    }

    /**
     * 知识库信息
     *
     * @param id 知识库id
     * @return
     */
    public R<IndustryKnowledgeVo> info(Long id) {
        IndustryKnowledgeVo vo = industryKnowledgeProducer.info(id);
        return R.ok("获取成功", vo);
    }

    /**
     * 新增知识库
     *
     * @param bo 知识库对象
     * @return
     */
    public R<String> save(IndustryKnowledgeBo bo) {
        IndustryKnowledgeVo exist = industryKnowledgeProducer.getByTradeIdAndType(bo.getTradeId(), bo.getKnowledgeType());
        if (exist != null) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "该行业下已存在该类型的知识库");
        }

        industryKnowledgeProducer.save(bo);
        return R.ok("添加成功");
    }

    /**
     * 修改知识库
     *
     * @param bo 知识库对象
     * @return
     */
    public R<String> update(IndustryKnowledgeBo bo) {
        IndustryKnowledgeVo exist = industryKnowledgeProducer.getByTradeIdAndType(bo.getTradeId(), bo.getKnowledgeType());
        if (exist != null && !exist.getId().equals(bo.getId())) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "该行业下已存在该类型的知识库");
        }

        industryKnowledgeProducer.update(bo);
        return R.ok("修改成功");
    }

    /**
     * 删除知识库
     *
     * @param id 知识库id
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public R<String> delete(Long id) {
        industryKnowledgeProducer.deleteById(id);
        return R.ok("删除成功");
    }

}
