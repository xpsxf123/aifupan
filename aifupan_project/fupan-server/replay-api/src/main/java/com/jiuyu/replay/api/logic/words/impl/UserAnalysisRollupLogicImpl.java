package com.jiuyu.replay.api.logic.words.impl;

import com.jiuyu.replay.api.logic.words.UserAnalysisRollupLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

import com.jiuyu.replay.power.bll.UserBll;
import com.jiuyu.replay.words.bll.UploadFileAnalysisRecordBll;
import com.jiuyu.replay.words.bll.VideoAnalysisRecordBll;
import com.jiuyu.replay.words.vo.UserAnalysisRollupListVo;
import com.jiuyu.replay.words.vo.UserAnalysisRollupInfoVo;
import com.jiuyu.replay.words.bo.UserAnalysisRollupBo;
import com.jiuyu.replay.words.bo.UserAnalysisRollupListBo;
import com.jiuyu.replay.words.bll.UserAnalysisRollupBll;

import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;


/**
 * 用户分析汇总表
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-01-19 16:56:41
 */
@Service
public class UserAnalysisRollupLogicImpl implements UserAnalysisRollupLogic {

    @Resource
    private UserAnalysisRollupBll userAnalysisRollupBll;
    @Resource
    private UserBll userBll;
    @Resource
    private VideoAnalysisRecordBll videoAnalysisRecordBll;
    @Resource
    private UploadFileAnalysisRecordBll uploadFileAnalysisRecordBll;

    @Override
    public R<PageUtils<UserAnalysisRollupListVo>> queryPage(UserAnalysisRollupListBo userAnalysisRollupListBo) {

        return userAnalysisRollupBll.queryPage(userAnalysisRollupListBo);
    }

    @Override
    public R<UserAnalysisRollupInfoVo> info(Long id) {

        return userAnalysisRollupBll.info(id);
    }

    @Override
    public R<String> save(UserAnalysisRollupBo userAnalysisRollupBo) {

        return userAnalysisRollupBll.save(userAnalysisRollupBo);
    }

    @Override
    public R<String> update(UserAnalysisRollupBo userAnalysisRollupBo) {

        return userAnalysisRollupBll.update(userAnalysisRollupBo);
    }

    @Override
    public R<String> delete(Long id) {

        return userAnalysisRollupBll.delete(id);
    }

}

