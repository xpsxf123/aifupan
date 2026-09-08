package com.jiuyu.replay.common.bll;


import com.jiuyu.replay.common.vo.OperationLogListVo;
import com.jiuyu.replay.common.vo.OperationLogInfoVo;
import com.jiuyu.replay.common.bo.OperationLogBo;
import com.jiuyu.replay.common.bo.OperationLogListBo;
import com.jiuyu.replay.common.producer.OperationLogProducer;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;


/**
 * 操作日志表
 *
 * @author lyw
 * @email 1798883178@qq.com
 * @date 2025-06-26 13:59:24
 */
@Component
public class OperationLogBll {

    @Resource
    private OperationLogProducer operationLogProducer;


    /**
     * 操作日志表列表
     * @param operationLogListBo 操作日志表列表查询参数
     * @return
     */
    public R<PageUtils<OperationLogListVo>> queryPage(OperationLogListBo operationLogListBo) {

        return R.ok("获取成功", operationLogProducer.queryPage(operationLogListBo));
    }

    /**
    * 操作日志表信息
    * @param id 操作日志表id
    * @return
    */
    public R<OperationLogInfoVo> info(Long id) {

        OperationLogInfoVo operationLogInfoVo = operationLogProducer.info(id);
        return R.ok("获取成功", operationLogInfoVo);
    }

    /**
     * 新增操作日志表
     * @param operationLogBo 操作日志表对象
     * @return
     */
    public R<String> save(OperationLogBo operationLogBo) {

        OperationLogInfoVo operationLogInfoVo = operationLogProducer.save(operationLogBo);
        return R.ok("添加成功");
    }

    /**
     * 修改操作日志表
     * @param operationLogBo 操作日志表对象
     * @return
     */
    public R<String> update(OperationLogBo operationLogBo) {

        operationLogProducer.update(operationLogBo);
        return R.ok("修改成功");
    }

    /**
     * 删除操作日志表
     * @param id 操作日志表id
     * @return
     */
    public R<String> delete(Long id) {

        operationLogProducer.deleteById(id);
        return R.ok("删除成功");
    }


}

