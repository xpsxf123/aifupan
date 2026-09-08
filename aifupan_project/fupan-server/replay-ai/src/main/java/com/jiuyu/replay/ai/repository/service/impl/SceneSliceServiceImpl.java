package com.jiuyu.replay.ai.repository.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jiuyu.replay.ai.entity.SceneSliceEntity;
import com.jiuyu.replay.ai.repository.dao.SceneSliceDao;
import com.jiuyu.replay.ai.repository.service.SceneSliceService;
import org.springframework.stereotype.Service;

/**
 * 场景切片记录 Service 实现
 *
 * @author lj
 * @date 2026-07-06
 */
@Service
public class SceneSliceServiceImpl extends ServiceImpl<SceneSliceDao, SceneSliceEntity>
        implements SceneSliceService {
}
