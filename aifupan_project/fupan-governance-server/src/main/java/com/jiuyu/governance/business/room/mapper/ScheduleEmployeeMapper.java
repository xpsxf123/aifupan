package com.jiuyu.governance.business.room.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jiuyu.governance.business.room.pojo.bo.EmployeeLiveRoomScheduleRawDto;
import com.jiuyu.governance.business.room.pojo.entity.ScheduleEmployee;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ScheduleEmployeeMapper extends BaseMapper<ScheduleEmployee> {


    int updateBatch(@Param("list") List<ScheduleEmployee> list);

    int updateBatchSelective(@Param("list") List<ScheduleEmployee> list);

    int batchInsert(@Param("list") List<ScheduleEmployee> list);
}
