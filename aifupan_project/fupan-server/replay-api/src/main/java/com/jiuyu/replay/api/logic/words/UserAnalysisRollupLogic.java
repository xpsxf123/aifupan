package com.jiuyu.replay.api.logic.words;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

import com.jiuyu.replay.words.vo.UserAnalysisRollupListVo;
import com.jiuyu.replay.words.vo.UserAnalysisRollupInfoVo;
import com.jiuyu.replay.words.bo.UserAnalysisRollupBo;
import com.jiuyu.replay.words.bo.UserAnalysisRollupListBo;


/**
 * 用户分析汇总表
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-01-19 16:56:41
 */
public interface UserAnalysisRollupLogic {


    /**
     * 用户分析汇总表列表
     * @param userAnalysisRollupListBo 用户分析汇总表列表查询参数
     * @return
     */
    R<PageUtils<UserAnalysisRollupListVo>> queryPage(UserAnalysisRollupListBo userAnalysisRollupListBo);

    /**
    * 用户分析汇总表信息
    * @param id 用户分析汇总表id
    * @return
    */
    R<UserAnalysisRollupInfoVo> info(Long id);

    /**
     * 新增用户分析汇总表
     * @param userAnalysisRollupBo 用户分析汇总表对象
     * @return
     */
    R<String> save(UserAnalysisRollupBo userAnalysisRollupBo);

    /**
     * 修改用户分析汇总表
     * @param userAnalysisRollupBo 用户分析汇总表对象
     * @return
     */
    R<String> update(UserAnalysisRollupBo userAnalysisRollupBo);

    /**
     * 删除用户分析汇总表
     * @param id 用户分析汇总表id
     * @return
     */
    R<String> delete(Long id);

}

