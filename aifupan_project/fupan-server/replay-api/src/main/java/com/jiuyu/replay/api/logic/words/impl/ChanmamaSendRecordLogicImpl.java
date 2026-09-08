package com.jiuyu.replay.api.logic.words.impl;

import com.jiuyu.replay.api.logic.words.ChanmamaSendRecordLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bll.ChanmamaSendRecordBll;
import com.jiuyu.replay.words.bo.viewing.ChanmamaSendRecordBo;
import com.jiuyu.replay.words.bo.viewing.ChanmamaSendRecordListBo;
import com.jiuyu.replay.words.vo.viewing.ChanmamaSendRecordInfoVo;
import com.jiuyu.replay.words.vo.viewing.ChanmamaSendRecordListVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;


/**
 * 第三方数据平台发送记录
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-04-22 14:21:09
 */
@Service
public class ChanmamaSendRecordLogicImpl implements ChanmamaSendRecordLogic {

    @Resource
    private ChanmamaSendRecordBll chanmamaSendRecordBll;


    @Override
    public R<PageUtils<ChanmamaSendRecordListVo>> queryPage(ChanmamaSendRecordListBo chanmamaSendRecordListBo) {

        return chanmamaSendRecordBll.queryPage(chanmamaSendRecordListBo);
    }

    @Override
    public R<ChanmamaSendRecordInfoVo> info(Long id) {

        return chanmamaSendRecordBll.info(id);
    }

    @Override
    public R<String> save(ChanmamaSendRecordBo chanmamaSendRecordBo) {

        return chanmamaSendRecordBll.save(chanmamaSendRecordBo);
    }

    @Override
    public R<String> update(ChanmamaSendRecordBo chanmamaSendRecordBo) {

        return chanmamaSendRecordBll.update(chanmamaSendRecordBo);
    }

    @Override
    public R<String> delete(Long id) {

        return chanmamaSendRecordBll.delete(id);
    }


}

