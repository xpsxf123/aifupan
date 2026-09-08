package com.jiuyu.replay.order.producer;

import com.jiuyu.replay.generic.utils.PageUtils;

import com.jiuyu.replay.generic.vo.third.AiTokenUseRecordListVo;
import com.jiuyu.replay.generic.vo.third.AiTokenUseRecordInfoVo;
import com.jiuyu.replay.generic.bo.third.AiTokenUseRecordBo;
import com.jiuyu.replay.order.bo.AiTokenUseRecordListBo;


/**
 * ai的token使用记录
 *
 * @author lj
 * @email 
 * @date 2025-03-21 18:00:33
 */
public interface AiTokenUseRecordProducer {


    /**
     * ai的token使用记录列表
     * @param aiTokenUseRecordListBo ai的token使用记录列表查询参数
     * @return
     */
    PageUtils<AiTokenUseRecordListVo> queryPage(AiTokenUseRecordListBo aiTokenUseRecordListBo);

    /**
    * ai的token使用记录信息
    * @param id ai的token使用记录id
    * @return
    */
    AiTokenUseRecordInfoVo info(Long id);

    /**
     * 新增ai的token使用记录
     * @param aiTokenUseRecordBo ai的token使用记录对象
     * @return
     */
     AiTokenUseRecordInfoVo save(AiTokenUseRecordBo aiTokenUseRecordBo);

    /**
     * 修改ai的token使用记录
     * @param aiTokenUseRecordBo ai的token使用记录对象
     * @return
     */
    void update(AiTokenUseRecordBo aiTokenUseRecordBo);

    /**
     * 删除ai的token使用记录
     * @param id ai的token使用记录id
     * @return
     */
    void deleteById(Long id);

    /**
     * 根据请求id获取ai的token使用记录
     *
     * @param requestId 请求id
     * @return ai的token使用记录
     */
    AiTokenUseRecordInfoVo getByRequestId(String requestId);

    /**
     * 统计租户累计算力消耗（total_tokens 求和）
     * sinceCreateDate 为 null 时统计全量历史；非 null 时统计 create_date >= sinceCreateDate
     *
     * @param tenantId        租户id
     * @param sinceCreateDate 起始时间下限，null 表示全量
     *
     * @return 累计 token 数，无记录返回 0
     */
    Long sumTotalTokensByTenantId(Long tenantId, java.util.Date sinceCreateDate);

    /**
     * 租户是否使用过指定类型助手（历史任一次即 true）
     *
     * @param tenantId      租户id
     * @param assistantType 使用类型 0: 运营助手，1:违规助手，2:弹幕助手，3：数据截图，4：弹幕助手
     *
     * @return 存在则返回 true
     */
    Boolean existsByTenantIdAndAssistantType(Long tenantId, Integer assistantType);
}

