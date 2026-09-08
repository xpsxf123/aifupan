package com.jiuyu.replay.api.logic.words.impl;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

import com.jiuyu.replay.power.utils.GlobalObject;
import com.jiuyu.replay.power.vo.UserCacheVo;
import com.jiuyu.replay.words.bo.CruxWordsBatchBo;
import com.jiuyu.replay.words.vo.CruxWordsListVo;
import com.jiuyu.replay.words.vo.CruxWordsInfoVo;
import com.jiuyu.replay.words.bo.CruxWordsBo;
import com.jiuyu.replay.words.bo.CruxWordsListBo;
import com.jiuyu.replay.api.logic.words.CruxWordsLogic;
import com.jiuyu.replay.words.bll.CruxWordsBll;

import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;


/**
 * 关键词
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-06-20 16:22:18
 */
@Service
public class CruxWordsLogicImpl implements CruxWordsLogic {

    @Resource
    private CruxWordsBll cruxWordsBll;


    @Override
    public R<PageUtils<CruxWordsListVo>> queryPage(CruxWordsListBo cruxWordsListBo) {

        return cruxWordsBll.queryPage(cruxWordsListBo);
    }

    @Override
    public R<CruxWordsInfoVo> info(Long id) {

        return cruxWordsBll.info(id);
    }

    @Override
    public R<String> save(CruxWordsBo cruxWordsBo) {

        UserCacheVo user = GlobalObject.getLocalUser();
        cruxWordsBo.setUserId(user.getId());

        return cruxWordsBll.save(cruxWordsBo);
    }

    @Override
    public R<String> update(CruxWordsBo cruxWordsBo) {

        return cruxWordsBll.update(cruxWordsBo);
    }

    @Override
    public R<String> delete(Long id) {

        return cruxWordsBll.delete(id);
    }

    @Override
    public R<String> saveBatch(CruxWordsBatchBo cruxWordsBatchBo) {

        UserCacheVo user = GlobalObject.getLocalUser();
        cruxWordsBatchBo.setUserId(user.getId());

        return cruxWordsBll.saveBatch(cruxWordsBatchBo);
    }


}

