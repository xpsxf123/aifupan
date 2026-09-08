package com.jiuyu.governance.business.performance.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.governance.business.performance.pojo.bo.LiveRoomPerformanceBO;
import com.jiuyu.governance.business.performance.pojo.request.LiveRoomPerformancePageRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 直播间业绩Mapper
 *
 * @author lj
 * @date 2026-06-25
 */
@Mapper
public interface LiveRoomPerformanceMapper {

    /**
     * 分页查询直播间业绩（JOIN session_performance 聚合）
     */
    IPage<LiveRoomPerformanceBO> pageQueryLiveRoomPerformance(
            IPage<LiveRoomPerformanceBO> page,
            @Param("req") LiveRoomPerformancePageRequest req);
}
