package com.jiuyu.replay.words.bll;

import com.jiuyu.replay.generic.feign.power.UserFeign;
import com.jiuyu.replay.generic.utils.ResultUtil;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.power.UserCacheVo;
import com.jiuyu.replay.words.bo.AiOptimizePurposeSaveBo;
import com.jiuyu.replay.words.bo.AiOptimizePurposeUpdateBo;
import com.jiuyu.replay.words.constant.Constant;
import com.jiuyu.replay.words.rse.AiOptimizePurposeRse;
import com.jiuyu.replay.generic.vo.words.AiOptimizePurposeVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * AI优化目的Bll
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2026-01-07
 */
@Component
public class AiOptimizePurposeBll {

    @Resource
    private AiOptimizePurposeRse aiOptimizePurposeRse;
    @Resource
    private UserFeign userFeign;

    /**
     * 新增AI优化目的
     *
     * @param saveBo 保存参数
     * @return 新增的ID
     */
    public R<Long> save(AiOptimizePurposeSaveBo saveBo) {
        if (saveBo.getSourceId() == null) {
            return R.error("来源id不能为空");
        }
        if (saveBo.getSourceType() == null) {
            return R.error("来源类型不能为空");
        }
        // 获取当前登录用户信息
        UserCacheVo user = ResultUtil.getResult(userFeign.getLocalUser());
        if (user == null) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "用户未登录");
        }
        saveBo.setUserId(user.getId());
        saveBo.setTenantId(user.getActiveTenantId());
        // 检查该来源id是否已存在记录
        AiOptimizePurposeVo existVo = aiOptimizePurposeRse.getBySourceId(saveBo.getSourceId());
        if (existVo != null) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "该来源已存在AI优化目的记录，不允许重复添加");
        }
        Long id = aiOptimizePurposeRse.save(saveBo);
        return R.ok("新增成功", id);
    }

    /**
     * 修改AI优化目的
     *
     * @param updateBo 更新参数
     * @return 是否成功
     */
    public R<String> update(AiOptimizePurposeUpdateBo updateBo) {
        if (updateBo.getId() == null) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "id不能为空");
        }
        boolean result = aiOptimizePurposeRse.update(updateBo);
        if (result) {
            return R.ok("修改成功");
        }
        return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "修改失败");
    }

    /**
     * 根据来源id查询AI优化目的信息
     *
     * @param sourceId   来源id
     * @return 优化目的信息
     */
    public R<AiOptimizePurposeVo> getBySourceId(String sourceId) {
        if (sourceId == null) {
            return R.error("来源id不能为空");
        }
        AiOptimizePurposeVo vo = aiOptimizePurposeRse.getBySourceId(sourceId);
        return R.ok("获取成功", vo);
    }
}
