package com.jiuyu.replay.common.producer;



import com.jiuyu.replay.common.vo.OperationLogListVo;
import com.jiuyu.replay.common.vo.OperationLogInfoVo;
import com.jiuyu.replay.common.bo.OperationLogBo;
import com.jiuyu.replay.common.bo.OperationLogListBo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.power.UserCacheVo;

import java.util.Map;


/**
 * 操作日志表
 *
 * @author lyw
 * @email 1798883178@qq.com
 * @date 2025-06-26 13:59:24
 */
public interface OperationLogProducer {


    /**
     * 操作日志表列表
     * @param operationLogListBo 操作日志表列表查询参数
     * @return
     */
    PageUtils<OperationLogListVo> queryPage(OperationLogListBo operationLogListBo);

    /**
    * 操作日志表信息
    * @param id 操作日志表id
    * @return
    */
    OperationLogInfoVo info(Long id);

    /**
     * 新增操作日志表
     * @param operationLogBo 操作日志表对象
     * @return
     */
     OperationLogInfoVo save(OperationLogBo operationLogBo);

    /**
     * 修改操作日志表
     * @param operationLogBo 操作日志表对象
     * @return
     */
    void update(OperationLogBo operationLogBo);

    /**
     * 删除操作日志表
     * @param id 操作日志表id
     * @return
     */
    void deleteById(Long id);

    /**
     * 操作记录保存
     * @param businessId
     * @param userId
     * @param allBeforeMap
     * @param allAfterMap
     * @param
     */
    void saveOptLog(String businessName, Long businessId, Long userId, Map<String
            , Object> allBeforeMap, Map<String, Object> allAfterMap,Long optUserId, String ip,String optName);

}

