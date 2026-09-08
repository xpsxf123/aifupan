package com.jiuyu.replay.words.repository.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jiuyu.replay.generic.vo.common.code.StatusCode;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import com.jiuyu.replay.words.entity.AnchorUrlUserEntity;
import com.jiuyu.replay.words.entity.StandardScriptEntity;
import com.jiuyu.replay.words.repository.dao.AnchorUrlUserDao;
import com.jiuyu.replay.words.repository.dao.StandardScriptDao;
import com.jiuyu.replay.words.repository.service.StandardScriptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * 标准直播稿 Service 实现类。
 *
 * <p>提供 findValid / softDelete / validateAccountAndLoadAnchor 三个核心操作，
 * 供 Bll 层调用，实现数据访问与业务编排的分层隔离。</p>
 *
 * @author beta
 * @date 2026-06-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StandardScriptServiceImpl
        extends ServiceImpl<StandardScriptDao, StandardScriptEntity>
        implements StandardScriptService {

    private final AnchorUrlUserDao anchorUrlUserDao;

    /**
     * 按 (tenantId, userId, secUid) 查找有效（is_deleted=0）标准稿。
     *
     * @param tenantId 租户 ID
     * @param userId   用户 ID
     * @param secUid   主播唯一标识
     * @return 有效标准稿 entity；不存在返回 null
     */
    @Override
    public StandardScriptEntity findValid(Long tenantId, Long userId, String secUid) {
        return getOne(new LambdaQueryWrapper<StandardScriptEntity>()
                .eq(StandardScriptEntity::getTenantId, tenantId)
                .eq(StandardScriptEntity::getUserId, userId)
                .eq(StandardScriptEntity::getSecUid, secUid)
                .eq(StandardScriptEntity::getIsDeleted, 0));
    }

    /**
     * 软删除标准稿（is_deleted=1，同步更新 update_date）。
     *
     * <p>事务由调用方 Bll 的 {@code @Transactional} 统一覆盖（默认 PROPAGATION_REQUIRED 加入外层事务），
     * 本方法不重复声明 {@code @Transactional} 避免事务边界冗余。</p>
     *
     * @param id 标准稿 ID
     */
    @Override
    public void softDelete(Long id) {
        update(new LambdaUpdateWrapper<StandardScriptEntity>()
                .eq(StandardScriptEntity::getId, id)
                .set(StandardScriptEntity::getIsDeleted, 1)
                .set(StandardScriptEntity::getUpdateDate, new Date()));
    }

    /**
     * 按 (tenantId, userId, secUid) 查找对应的 AnchorUrlUser 并校验 accountType。
     *
     * <p>查到 accountType=1（竞品/同行）时抛 BusinessException(70004)；
     * 查不到（新增直播间场景）时跳过校验，允许继续。</p>
     *
     * @param tenantId 租户 ID
     * @param userId   用户 ID
     * @param secUid   主播唯一标识
     * @return AnchorUrlUserEntity 或 null（新增直播间场景）
     */
    @Override
    public AnchorUrlUserEntity validateAccountAndLoadAnchor(Long tenantId, Long userId, String secUid) {
        AnchorUrlUserEntity anchorUser = anchorUrlUserDao.selectOne(
                new LambdaQueryWrapper<AnchorUrlUserEntity>()
                        .eq(AnchorUrlUserEntity::getTenantId, tenantId)
                        .eq(AnchorUrlUserEntity::getUserId, userId)
                        .eq(AnchorUrlUserEntity::getAnchorUrlSecUid, secUid)
                        .eq(AnchorUrlUserEntity::getIsRemoveRecord, 0)
                        .last("LIMIT 1"));
        if (anchorUser != null && Integer.valueOf(1).equals(anchorUser.getAccountType())) {
            log.warn("[标准稿] 竞品/同行账号拦截 tenantId={} userId={} secUid={} accountType={}",
                    tenantId, userId, secUid, anchorUser.getAccountType());
            throw new BusinessException(StatusCode.SCRIPT_MONITOR_NOT_OWN_ACCOUNT.getCode(),
                    StatusCode.SCRIPT_MONITOR_NOT_OWN_ACCOUNT.getMsg());
        }
        return anchorUser;
    }
}
