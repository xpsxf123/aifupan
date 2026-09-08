package com.jiuyu.replay.api.logic.words.impl;

import com.jiuyu.replay.api.logic.words.LexiconWordLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bll.LexiconWordBll;
import com.jiuyu.replay.words.bo.LexiconWordBo;
import com.jiuyu.replay.words.bo.LexiconWordListBo;
import com.jiuyu.replay.words.vo.LexiconWordInfoVo;
import com.jiuyu.replay.words.vo.LexiconWordListVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;


/**
 * 词库-词语关联
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-10-08 10:41:43
 */
@Service
public class LexiconWordLogicImpl implements LexiconWordLogic {

    @Resource
    private LexiconWordBll lexiconWordBll;


    @Override
    public R<PageUtils<LexiconWordListVo>> queryPage(LexiconWordListBo lexiconWordListBo) {

        return lexiconWordBll.queryPage(lexiconWordListBo);
    }

    @Override
    public R<LexiconWordInfoVo> info(Long id) {

        return lexiconWordBll.info(id);
    }

    @Override
    public R<String> save(LexiconWordBo lexiconWordBo) {

        return lexiconWordBll.save(lexiconWordBo);
    }

    @Override
    public R<String> update(LexiconWordBo lexiconWordBo) {

        return lexiconWordBll.update(lexiconWordBo);
    }

    @Override
    public R<String> delete(Long id) {

        return lexiconWordBll.delete(id);
    }


}

