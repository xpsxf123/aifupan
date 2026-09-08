package com.jiuyu.governance.business.org.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jiuyu.governance.business.org.pojo.entity.ManagerConnector;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 管理员连接器
 *
 * @author hehui
 * @date 2026/03/18
 */
@Mapper
public interface ManagerConnectorMapper extends BaseMapper<ManagerConnector> {

    /**
     * 批量插入
     *
     * @param list 列表
     */
    void insertBatch(List<ManagerConnector> list);
}
