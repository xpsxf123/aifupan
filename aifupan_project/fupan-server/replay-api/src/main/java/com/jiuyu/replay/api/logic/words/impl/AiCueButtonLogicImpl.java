package com.jiuyu.replay.api.logic.words.impl;

import com.jiuyu.replay.api.logic.words.AiCueButtonLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bll.AiCueButtonBll;
import com.jiuyu.replay.words.bo.AiCueButtonBo;
import com.jiuyu.replay.words.bo.AiCueButtonListBo;
import com.jiuyu.replay.words.vo.AiCueButtonInfoVo;
import com.jiuyu.replay.words.vo.AiCueButtonListVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;


/**
 * 固定提示按钮

 *
 * @author DearSil
 * @email 2437495924@qq.com
 * @date 2025-02-25 11:11:43
 */
@Service
public class AiCueButtonLogicImpl implements AiCueButtonLogic {

    @Resource
    private AiCueButtonBll aiCueButtonBll;


    @Override
    public R<PageUtils<AiCueButtonListVo>> queryPage(AiCueButtonListBo aiCueButtonListBo) {

        return aiCueButtonBll.queryPage(aiCueButtonListBo);
    }

    @Override
    public R<AiCueButtonInfoVo> info(Long id) {

        return aiCueButtonBll.info(id);
    }

    @Override
    public R<String> save(AiCueButtonBo aiCueButtonBo) {

        return aiCueButtonBll.save(aiCueButtonBo);
    }

    @Override
    public R<String> update(AiCueButtonBo aiCueButtonBo) {

        return aiCueButtonBll.update(aiCueButtonBo);
    }

    @Override
    public R<String> delete(Long id) {

        return aiCueButtonBll.delete(id);
    }


}

