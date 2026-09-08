package com.jiuyu.replay.words.repository.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.replay.generic.vo.words.AnchorAuthStatusVo;
import com.jiuyu.replay.words.entity.AnchorUrlUserEntity;
import com.jiuyu.replay.words.repository.dao.AnchorUrlUserDao;
import com.jiuyu.replay.words.repository.service.AnchorUrlUserService;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service("anchorUrlUserService")
public class AnchorUrlUserServiceImpl extends ServiceImpl<AnchorUrlUserDao, AnchorUrlUserEntity> implements AnchorUrlUserService {

    @Resource
    private AnchorUrlUserDao anchorUrlUserDao;

    @Override
    public Integer sum(Long userId) {
        return anchorUrlUserDao.countByUsrId(userId);
    }

    @Override
    public List<AnchorUrlUserEntity> listBySecUid(List<String> secUids, Long userId, Long tenantId) {
        List<AnchorUrlUserEntity> list = lambdaQuery()
                .in(AnchorUrlUserEntity::getAnchorUrlSecUid, secUids)
                .eq(ObjectUtil.isNotNull(userId),AnchorUrlUserEntity::getUserId, userId)
                .eq(ObjectUtil.isNotNull(tenantId),AnchorUrlUserEntity::getTenantId, tenantId)
                .list();
        if (list!= null && !list.isEmpty()){
            return list;
        }
        return new ArrayList<>();
    }

    /**
     * 获取主播的所属行业
     *
     * @param secUids 主播secUid
     */
    @Override
    public Map<String, List<Long>> getAnchorTradeMap(List<String> secUids) {
        if (EmptyUtil.isEmpty(secUids)) {
            return Map.of();
        }
        return super.lambdaQuery().in(AnchorUrlUserEntity::getAnchorUrlSecUid, secUids)
            .eq(AnchorUrlUserEntity::getIsDeleted, 0)
            .select(AnchorUrlUserEntity::getAnchorUrlSecUid, AnchorUrlUserEntity::getTradeId)
            .list().stream().collect(Collectors.groupingBy(AnchorUrlUserEntity::getAnchorUrlSecUid, Collectors.mapping(AnchorUrlUserEntity::getTradeId, Collectors.toList())));
    }

    /**
     * 获取当前租户已添加主播的行业ID
     *
     * @param tenantId 租户ID
     */
    @Override
    public List<Long> getTenantTradeIds(long tenantId) {
        return anchorUrlUserDao.getTenantTradeIds(tenantId);
    }

    @Override
    public AnchorAuthStatusVo getAuthStatusBySecUid(String secUid, Long userId, Long tenantId) {
        AnchorUrlUserEntity entity = lambdaQuery()
                .eq(AnchorUrlUserEntity::getAnchorUrlSecUid, secUid)
                .eq(ObjectUtil.isNotNull(userId), AnchorUrlUserEntity::getUserId, userId)
                .eq(ObjectUtil.isNotNull(tenantId), AnchorUrlUserEntity::getTenantId, tenantId)
                .eq(AnchorUrlUserEntity::getIsRemoveRecord, 0)
                .select(AnchorUrlUserEntity::getAnchorUrlSecUid,
                        AnchorUrlUserEntity::getAuthJlbyStatus,
                        AnchorUrlUserEntity::getAuthJlbyStatusTime,
                        AnchorUrlUserEntity::getAuthQcStatus,
                        AnchorUrlUserEntity::getAuthQcStatusTime,
                        AnchorUrlUserEntity::getAuthChannelStatus,
                        AnchorUrlUserEntity::getAuthLifeStatus,
                        AnchorUrlUserEntity::getAuthLifeStatusTime)
                .one();
        if (entity == null) {
            return null;
        }
        AnchorAuthStatusVo vo = new AnchorAuthStatusVo();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
