package com.jiuyu.replay.words.repository.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jiuyu.replay.words.entity.AnchorVideoRecodEntity;
import com.jiuyu.replay.words.repository.dao.AnchorVideoRecodDao;
import com.jiuyu.replay.words.repository.service.AnchorVideoRecodService;
import org.springframework.stereotype.Service;

/**
 * @author tisheng
 * @date 2024/9/11
 * @apinNote
 */
@Service("anchorVideoRecodService")
public class AnchorVideoRecodServiceImpl extends ServiceImpl<AnchorVideoRecodDao, AnchorVideoRecodEntity> implements AnchorVideoRecodService {
}
