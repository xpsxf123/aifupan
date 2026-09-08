package com.jiuyu.replay.ai.rse;

import com.jiuyu.replay.ai.bo.GlobalProblemBo;
import com.jiuyu.replay.ai.bo.GlobalProblemListBo;
import com.jiuyu.replay.ai.vo.GlobalProblemListVo;
import com.jiuyu.replay.generic.utils.PageUtils;

import java.util.List;

/**
 * 全局提示词 Rse
 *
 * @author lj
 * @date 2026-05-21
 */
public interface GlobalProblemRse {

    PageUtils<GlobalProblemListVo> queryPage(GlobalProblemListBo listBo);

    GlobalProblemListVo info(Long id);

    GlobalProblemListVo save(GlobalProblemBo bo);

    void update(GlobalProblemBo bo);

    void deleteById(Long id);

    List<GlobalProblemListVo> listByConditions(Integer cueType, Integer applyTo, Integer problemType);
}
