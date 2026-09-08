package com.jiuyu.replay.words.repository.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jiuyu.replay.words.entity.ThirdpartyLiveRecordEntity;
import com.jiuyu.replay.words.repository.dao.ThirdpartyLiveRecordDao;
import com.jiuyu.replay.words.repository.service.ThirdpartyLiveRecordService;
import org.springframework.stereotype.Service;

/**
 * 第三方直播录制记录ServiceImpl
 *
 * @author System
 * @date 2026-04-09
 */
@Service("thirdpartyLiveRecordService")
public class ThirdpartyLiveRecordServiceImpl extends ServiceImpl<ThirdpartyLiveRecordDao, ThirdpartyLiveRecordEntity>
        implements ThirdpartyLiveRecordService {

}
