package com.jiuyu.replay.api.logic.words.impl;

import com.jiuyu.replay.api.logic.words.LexiconLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.power.bll.UserBll;
import com.jiuyu.replay.power.utils.GlobalObject;
import com.jiuyu.replay.power.vo.UserCacheVo;
import com.jiuyu.replay.words.bll.LexiconBll;
import com.jiuyu.replay.words.bo.LexiconBo;
import com.jiuyu.replay.words.bo.LexiconListBo;
import com.jiuyu.replay.words.bo.LexiconWordListBo;
import com.jiuyu.replay.words.vo.LexiconInfoVo;
import com.jiuyu.replay.words.vo.LexiconListVo;
import com.jiuyu.replay.words.vo.SensitiveWordsClientListVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;


/**
 * 词库
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-10-06 16:34:13
 */
@Service
public class LexiconLogicImpl implements LexiconLogic {

    @Resource
    private LexiconBll lexiconBll;
    @Resource
    private UserBll userBll;


    @Override
    public R<PageUtils<LexiconListVo>> queryPage(LexiconListBo lexiconListBo) {

        UserCacheVo user = GlobalObject.getLocalUser();
        R<Long> parentR = userBll.getCacheUserParentId(user.getId());
        if(parentR.getCode() == 0 && !StringUtils.isEmpty(parentR.getData())) {
            lexiconListBo.setUserId(parentR.getData());
        }else {
            lexiconListBo.setUserId(user.getId());
        }

        return lexiconBll.queryPage(lexiconListBo);
    }

    @Override
    public R<LexiconInfoVo> info(Long id) {

        return lexiconBll.info(id);
    }

    @Override
    public R<String> save(LexiconBo lexiconBo) {

        UserCacheVo user = GlobalObject.getLocalUser();
        R<Long> parentR = userBll.getCacheUserParentId(user.getId());
        if(parentR.getCode() == 0 && !StringUtils.isEmpty(parentR.getData())) {
            lexiconBo.setUserId(parentR.getData());
        }else {
            lexiconBo.setUserId(user.getId());
        }

        return lexiconBll.save(lexiconBo);
    }

    @Override
    public R<String> update(LexiconBo lexiconBo) {

        UserCacheVo user = GlobalObject.getLocalUser();
        R<Long> parentR = userBll.getCacheUserParentId(user.getId());
        if(parentR.getCode() == 0 && !StringUtils.isEmpty(parentR.getData())) {
            lexiconBo.setUserId(parentR.getData());
        }else {
            lexiconBo.setUserId(user.getId());
        }

        return lexiconBll.update(lexiconBo);
    }

    @Override
    public R<String> delete(Long id) {

        return lexiconBll.delete(id);
    }

    @Override
    public R<PageUtils<SensitiveWordsClientListVo>> getWordsList(LexiconWordListBo lexiconWordListBo) {

        return lexiconBll.getWordsList(lexiconWordListBo);
    }


}

