package com.jiuyu.replay.ai.rse;

import com.jiuyu.replay.ai.bo.CustPromptBo;
import com.jiuyu.replay.ai.bo.CustPromptListBo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.ai.CustPromptVo;

/**
 * 用户自定义提示词 Rse (Producer)
 *
 * @author jxy
 * @date 2025-01-21
 */
public interface CustPromptRse {

    /**
     * 分页查询用户自定义提示词列表
     *
     * @param listBo 查询参数
     * @return 分页结果
     */
    PageUtils<CustPromptVo> queryPage(CustPromptListBo listBo);

    /**
     * 获取用户自定义提示词详情
     *
     * @param id 提示词ID
     * @return 提示词详情
     */
    CustPromptVo info(Long id);

    /**
     * 新增用户自定义提示词
     *
     * @param custPromptBo 提示词对象
     * @return 新增后的提示词
     */
    CustPromptVo save(CustPromptBo custPromptBo);

    /**
     * 修改用户自定义提示词
     *
     * @param custPromptBo 提示词对象
     */
    void update(CustPromptBo custPromptBo);

    /**
     * 删除用户自定义提示词
     *
     * @param id 提示词ID
     */
    void deleteById(Long id);
}

