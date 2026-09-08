package com.jiuyu.replay.api.logic.common;



import com.jiuyu.replay.common.vo.OperationLogListVo;
import com.jiuyu.replay.common.vo.OperationLogInfoVo;
import com.jiuyu.replay.common.bo.OperationLogBo;
import com.jiuyu.replay.common.bo.OperationLogListBo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;


/**
 * 操作日志表
 *
 * @author lyw
 * @email 1798883178@qq.com
 * @date 2025-06-26 13:59:24
 */
public interface OperationLogLogic {


    /**
     * 操作日志表列表
     * @param operationLogListBo 操作日志表列表查询参数
     * @return
     */
    R<PageUtils<OperationLogListVo>> queryPage(OperationLogListBo operationLogListBo);

    /**
    * 操作日志表信息
    * @param id 操作日志表id
    * @return
    */
    R<OperationLogInfoVo> info(Long id);

    /**
     * 新增操作日志表
     * @param operationLogBo 操作日志表对象
     * @return
     */
    R<String> save(OperationLogBo operationLogBo);

    /**
     * 修改操作日志表
     * @param operationLogBo 操作日志表对象
     * @return
     */
    R<String> update(OperationLogBo operationLogBo);

    /**
     * 删除操作日志表
     * @param id 操作日志表id
     * @return
     */
    R<String> delete(Long id);


}

