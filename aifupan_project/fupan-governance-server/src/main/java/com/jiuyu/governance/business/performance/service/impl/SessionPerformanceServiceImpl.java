package com.jiuyu.governance.business.performance.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jiuyu.governance.business.performance.mapper.SessionPerformanceMapper;
import com.jiuyu.governance.business.performance.pojo.entity.SessionPerformance;
import com.jiuyu.governance.business.performance.service.SessionPerformanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 场次业绩服务实现类
 *
 * @author lj
 * @date 2026-03-24
 */
@Service
@RequiredArgsConstructor
public class SessionPerformanceServiceImpl extends ServiceImpl<SessionPerformanceMapper, SessionPerformance>
        implements SessionPerformanceService {
}
