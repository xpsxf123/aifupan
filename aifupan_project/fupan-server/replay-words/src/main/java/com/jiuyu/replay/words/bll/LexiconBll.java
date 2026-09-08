package com.jiuyu.replay.words.bll;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bo.LexiconBo;
import com.jiuyu.replay.words.bo.LexiconListBo;
import com.jiuyu.replay.words.bo.LexiconWordListBo;
import com.jiuyu.replay.words.constant.Constant;
import com.jiuyu.replay.words.producer.CruxTypeProducer;
import com.jiuyu.replay.words.producer.LexiconProducer;
import com.jiuyu.replay.words.vo.LexiconInfoVo;
import com.jiuyu.replay.words.vo.LexiconListVo;
import com.jiuyu.replay.words.vo.SensitiveWordsClientListVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;


/**
 * 词库
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-10-06 16:34:13
 */
@Component
public class LexiconBll {

    @Resource
    private LexiconProducer lexiconProducer;
    @Resource
    private CruxTypeProducer cruxTypeProducer;

    /**
     * 词库列表
     * @param lexiconListBo 词库列表查询参数
     * @return
     */
    public R<PageUtils<LexiconListVo>> queryPage(LexiconListBo lexiconListBo) {

        return R.ok("获取成功", lexiconProducer.queryPage(lexiconListBo));
    }

    /**
    * 词库信息
    * @param id 词库id
    * @return
    */
    public R<LexiconInfoVo> info(Long id) {

        LexiconInfoVo lexiconInfoVo = lexiconProducer.info(id);
        return R.ok("获取成功", lexiconInfoVo);
    }

    /**
     * 新增词库
     * @param lexiconBo 词库对象
     * @return
     */
    public R<String> save(LexiconBo lexiconBo) {

        // 检查当前用户是否已存在该行业的词库
        LexiconInfoVo lexiconInfoVo = this.lexiconProducer.getByUserIdAndTradeId(lexiconBo.getUserId(), lexiconBo.getTradeId());
        if(lexiconInfoVo != null) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "当前行业已存在词库，请勿重复添加");
        }

        lexiconProducer.save(lexiconBo);
        return R.ok("添加成功");
    }

    /**
     * 修改词库
     * @param lexiconBo 词库对象
     * @return
     */
    public R<String> update(LexiconBo lexiconBo) {

        // 检查当前用户是否已存在该行业的词库
        if(!lexiconBo.getTradeId().equals(lexiconBo.getOldTradeId())) {
            LexiconInfoVo lexiconInfoVo = this.lexiconProducer.getByUserIdAndTradeId(lexiconBo.getUserId(), lexiconBo.getTradeId());
            if(lexiconInfoVo != null) {
                return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "当前行业已存在词库，请勿重复添加");
            }
        }

        lexiconProducer.update(lexiconBo);
        return R.ok("修改成功");
    }

    /**
     * 删除词库
     * @param id 词库id
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public R<String> delete(Long id) {
        // 删除词库
        lexiconProducer.deleteById(id);

        return R.ok("删除成功");
    }


    /**
     * 获取词库的词语列表
     * @param lexiconWordListBo 列表查询参数
     * @return
     */
    public R<PageUtils<SensitiveWordsClientListVo>> getWordsList(LexiconWordListBo lexiconWordListBo) {

        if(!StringUtils.isEmpty(lexiconWordListBo.getCruxTypeId())) {
            List<Long> cruxTypeIds = this.cruxTypeProducer.getChildrenIdAndSelfIdList(lexiconWordListBo.getCruxTypeId());
            if(cruxTypeIds != null && cruxTypeIds.size() > 0) {
                lexiconWordListBo.setCruxTypeIds(cruxTypeIds);
            }
        }

        return R.ok("获取成功", lexiconProducer.getWordsList(lexiconWordListBo));
    }
}

