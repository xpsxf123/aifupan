package com.jiuyu.replay.agent.repository.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jiuyu.replay.agent.entity.InviteUrlCodeEntity;
import com.jiuyu.replay.agent.repository.dao.InviteUrlCodeDao;
import com.jiuyu.replay.agent.repository.service.InviteUrlCodeService;
import com.jiuyu.replay.generic.vo.agent.InviteUrlCodeAndPromotionName;
import org.springframework.stereotype.Service;

import java.util.List;


@Service("inviteUrlCodeService")
public class InviteUrlCodeServiceImpl extends ServiceImpl<InviteUrlCodeDao, InviteUrlCodeEntity> implements InviteUrlCodeService {

    @Override
    public List<InviteUrlCodeAndPromotionName> inviteCodeAndPromotionName(List<String> inviteUrlCodes) {
        return baseMapper.inviteCodeAndPromotionName(inviteUrlCodes);
    }
}