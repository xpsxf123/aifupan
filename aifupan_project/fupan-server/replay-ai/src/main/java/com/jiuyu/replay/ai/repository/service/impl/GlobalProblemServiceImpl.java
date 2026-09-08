package com.jiuyu.replay.ai.repository.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jiuyu.replay.ai.entity.GlobalProblemEntity;
import com.jiuyu.replay.ai.repository.dao.GlobalProblemDao;
import com.jiuyu.replay.ai.repository.service.GlobalProblemService;
import org.springframework.stereotype.Service;

/**
 * 全局提示词 Service 实现
 *
 * @author lj
 * @date 2026-05-21
 */
@Service
public class GlobalProblemServiceImpl extends ServiceImpl<GlobalProblemDao, GlobalProblemEntity> implements GlobalProblemService {
}
