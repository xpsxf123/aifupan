package com.jiuyu.replay.third.repository.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jiuyu.replay.third.entity.AsrEngineConfigEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * ASR 引擎按租户 / 用户 / 语言优先级配置 Dao。
 *
 * @author hehh
 * @date 2026-05-25
 */
@Mapper
public interface AsrEngineConfigDao extends BaseMapper<AsrEngineConfigEntity> {

}
