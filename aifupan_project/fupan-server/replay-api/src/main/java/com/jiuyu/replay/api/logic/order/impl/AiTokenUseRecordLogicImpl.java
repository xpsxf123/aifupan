package com.jiuyu.replay.api.logic.order.impl;

import com.jiuyu.replay.api.logic.order.AiTokenUseRecordLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.order.bll.AiTokenUseRecordBll;
import com.jiuyu.replay.generic.bo.third.AiTokenUseRecordBo;
import com.jiuyu.replay.order.bo.AiTokenUseRecordListBo;
import com.jiuyu.replay.generic.vo.third.AiTokenUseRecordInfoVo;
import com.jiuyu.replay.generic.vo.third.AiTokenUseRecordListVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;


/**
 * ai的token使用记录
 *
 * @author lj
 * @email 
 * @date 2025-03-21 18:00:33
 */
@Service
public class AiTokenUseRecordLogicImpl implements AiTokenUseRecordLogic {

    @Resource
    private AiTokenUseRecordBll aiTokenUseRecordBll;


    @Override
    public R<PageUtils<AiTokenUseRecordListVo>> queryPage(AiTokenUseRecordListBo aiTokenUseRecordListBo) {

        return aiTokenUseRecordBll.queryPage(aiTokenUseRecordListBo);
    }

    @Override
    public R<AiTokenUseRecordInfoVo> info(Long id) {

        return aiTokenUseRecordBll.info(id);
    }

    @Override
    public R<AiTokenUseRecordInfoVo> save(AiTokenUseRecordBo aiTokenUseRecordBo) {

        return aiTokenUseRecordBll.save(aiTokenUseRecordBo);
    }

    @Override
    public R<String> update(AiTokenUseRecordBo aiTokenUseRecordBo) {

        return aiTokenUseRecordBll.update(aiTokenUseRecordBo);
    }

    @Override
    public R<String> delete(Long id) {

        return aiTokenUseRecordBll.delete(id);
    }


}

