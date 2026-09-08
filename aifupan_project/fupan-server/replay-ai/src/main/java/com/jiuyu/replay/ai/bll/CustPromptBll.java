package com.jiuyu.replay.ai.bll;

import cn.hutool.core.util.NumberUtil;
import com.jiuyu.replay.ai.bo.CustPromptBo;
import com.jiuyu.replay.ai.bo.CustPromptListBo;
import com.jiuyu.replay.ai.rse.CustPromptRse;
import com.jiuyu.replay.common.producer.SystemKvProducer;
import com.jiuyu.replay.common.vo.SystemKvInfoVo;
import com.jiuyu.replay.generic.feign.order.OrderFeign;
import com.jiuyu.replay.generic.feign.power.UserFeign;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.utils.ResultUtil;
import com.jiuyu.replay.generic.vo.ai.CustPromptVo;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import com.jiuyu.replay.generic.vo.order.OrderInfoVo;
import com.jiuyu.replay.generic.vo.power.UserCacheVo;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 用户自定义提示词 Bll
 *
 * @author jxy
 * @date 2025-01-21
 */
@AllArgsConstructor
@Component
public class CustPromptBll {

    private final CustPromptRse custPromptRse;
    private final OrderFeign orderFeign;
    private final UserFeign userFeign;
    private final SystemKvProducer systemKvProducer;


    /**
     * 分页查询用户自定义提示词列表
     *
     * @param listBo 查询参数
     * @return 分页结果
     */
    public PageUtils<CustPromptVo> queryPage(CustPromptListBo listBo) {
        return custPromptRse.queryPage(listBo);
    }

    /**
     * 获取用户自定义提示词详情
     *
     * @param id 提示词ID
     * @return 提示词详情
     */
    public CustPromptVo info(Long id) {
        return custPromptRse.info(id);
    }

    /**
     * 新增用户自定义提示词
     *
     * @param custPromptBo 提示词对象
     * @return 新增后的提示词
     */
    public CustPromptVo save(CustPromptBo custPromptBo) {
        // 校验
        check(custPromptBo);
        UserCacheVo user = ResultUtil.getUserResult(userFeign.getLocalUser());
        OrderInfoVo order = orderFeign.currentOrderByUserId(user.getId());
        if (order == null || order.getLevel() < 20) {
            throw new BusinessException("请升级到企业版及以上使用");
        }
        return custPromptRse.save(custPromptBo);
    }

    /**
     * 修改用户自定义提示词
     *
     * @param custPromptBo 提示词对象
     */
    public void update(CustPromptBo custPromptBo) {
        // 校验
        check(custPromptBo);
        custPromptRse.update(custPromptBo);
    }

    /**
     * 校验
     *
     * @param custPromptBo 提示词对象
     */
    private void check(CustPromptBo custPromptBo) {
        int count = 30000;
        SystemKvInfoVo byKey = systemKvProducer.getByKey("cust_prompt_content_max_count");
        if (byKey != null && byKey.getKvValue() != null) {
            count = NumberUtil.parseInt(byKey.getKvValue(), count);
        }
        if (custPromptBo.getPromptContent().length() > count) {
            throw new BusinessException("提示词内容不能超过" + count + "字");
        }
    }

    /**
     * 删除用户自定义提示词
     *
     * @param id 提示词ID
     */
    public void delete(Long id) {
        custPromptRse.deleteById(id);
    }
}

