package com.jiuyu.replay.ai.repository.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jiuyu.replay.ai.entity.CustPromptEntity;
import com.jiuyu.replay.ai.repository.dao.CustPromptDao;
import com.jiuyu.replay.ai.repository.service.CustPromptService;
import org.springframework.stereotype.Service;

/**
 * 用户自定义提示词 Service 实现
 *
 * @author jxy
 * @date 2025-01-21
 */
@Service
public class CustPromptServiceImpl extends ServiceImpl<CustPromptDao, CustPromptEntity> implements CustPromptService {
}

