package com.jiuyu.replay.power.repository.dao;

import com.jiuyu.replay.power.entity.UserLoginLogEntity;
import com.jiuyu.replay.power.vo.datahub.DataHubLoginStatsVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * 用户登录日志
 * 
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-11-15 18:48:54
 */
@Mapper
public interface UserLoginLogDao extends BaseMapper<UserLoginLogEntity> {

	/**
	 * 按用户批量聚合成功登录记录（opera_type=0 且 opera_status=0）
	 *
	 * @param userIds   用户id列表
	 * @param startDate 窗口起（含，可空）
	 * @param endDate   窗口止（不含，可空）
	 *
	 * @return 每用户一行的登录聚合
	 */
	List<DataHubLoginStatsVo> selectLoginStats(@Param("userIds") Collection<Long> userIds,
											   @Param("startDate") Date startDate,
											   @Param("endDate") Date endDate);
}
