package com.jiuyu.replay.api.logic.words.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.jiuyu.replay.ai.bll.DiagnosisCueBll;
import com.jiuyu.replay.ai.bll.DiagnosisModelBll;
import com.jiuyu.replay.api.logic.words.DiagnosisCueLogic;
import com.jiuyu.replay.common.constant.Constant;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.generic.bo.ai.DiagnosisCueBo;
import com.jiuyu.replay.generic.bo.ai.DiagnosisCueListBo;
import com.jiuyu.replay.generic.vo.ai.*;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.words.AnchorVideoInfoVo;
import com.jiuyu.replay.generic.vo.words.CueWordsListVo;
import com.jiuyu.replay.power.utils.GlobalObject;
import com.jiuyu.replay.power.vo.UserCacheVo;
import com.jiuyu.replay.words.bll.AnchorUrlBll;
import com.jiuyu.replay.words.bll.AnchorVideoBll;
import com.jiuyu.replay.words.bll.CueWordsBll;
import com.jiuyu.replay.words.bll.TradeBll;
import com.jiuyu.replay.generic.bo.words.CueWordsListBo;
import com.jiuyu.replay.generic.vo.words.TradeInfoVo;
import com.jiuyu.replay.generic.vo.words.AnchorUrlUserVo;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;


/**
 * ai诊断提示词配置
 *
 * @author lj
 * @email 
 * @date 2025-05-19 14:02:59
 */
@Service
@AllArgsConstructor
public class DiagnosisCueLogicImpl implements DiagnosisCueLogic {

    private final DiagnosisCueBll diagnosisCueBll;
    private final AnchorVideoBll anchorVideoBll;
    private final AnchorUrlBll anchorUrlBll;
    private final TradeBll tradeBll;
    private final CueWordsBll cueWordsBll;
    private final DiagnosisModelBll diagnosisModelBll;


    @Override
    public R<PageUtils<DiagnosisCueListVo>> queryPage(DiagnosisCueListBo diagnosisCueListBo) {

        return diagnosisCueBll.queryPage(diagnosisCueListBo);
    }

    @Override
    public R<DiagnosisCueInfoVo> info(Long id) {

        return diagnosisCueBll.info(id);
    }

    @Override
    public R<String> save(DiagnosisCueBo diagnosisCueBo) {

        return diagnosisCueBll.save(diagnosisCueBo);
    }

    @Override
    public R<String> update(DiagnosisCueBo diagnosisCueBo) {

        return diagnosisCueBll.update(diagnosisCueBo);
    }

    @Override
    public R<String> delete(Long id) {

        return diagnosisCueBll.delete(id);
    }

    @Override
    public R<String> updateDiagnosisCueStatus(DiagnosisCueBo diagnosisCueBo) {
        return diagnosisCueBll.update(diagnosisCueBo);
    }
}

