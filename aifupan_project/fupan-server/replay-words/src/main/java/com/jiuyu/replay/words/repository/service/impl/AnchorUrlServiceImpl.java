package com.jiuyu.replay.words.repository.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.framework.util.FunctionUtil;
import com.jiuyu.replay.words.bo.AnchorUrlPegBo;
import com.jiuyu.replay.words.entity.AnchorUrlEntity;
import com.jiuyu.replay.words.repository.dao.AnchorUrlDao;
import com.jiuyu.replay.words.repository.service.AnchorUrlService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service("anchorUrlService")
public class AnchorUrlServiceImpl extends ServiceImpl<AnchorUrlDao, AnchorUrlEntity> implements AnchorUrlService {

    @Override
    public Page<AnchorUrlEntity> pageList(IPage<AnchorUrlEntity> page, AnchorUrlPegBo anchorUrlPegBo) {
        return baseMapper.pageList(page, anchorUrlPegBo);
    }


    /**
     * 批量更新系统行业id
     *
     * @param anchorIds 主播id集合
     * @param tradeId    系统行业ID
     *
     * @return 是否成功
     */
    @Override
    public boolean batchUpdateSystemTradeId(List<Long> anchorIds, Long tradeId) {
        if (EmptyUtil.isEmpty(anchorIds)) {
            return false;
        }

        return super.lambdaUpdate()
            .set(AnchorUrlEntity::getSystemTradeId, tradeId)
            .in(AnchorUrlEntity::getId, anchorIds)
            .update();
    }

    /**
     * 批量获取主播
     *
     * @param anchorIds 主播id
     *
     * @return 主播
     */
    @Override
    public Map<Long, AnchorUrlEntity> getAnchorMap(List<Long> anchorIds) {
        if (EmptyUtil.isEmpty(anchorIds)) {
            return EmptyUtil.emptyMap();
        }
        List<AnchorUrlEntity> entityList = super.baseMapper.selectBatchIds(anchorIds);
        if (EmptyUtil.isEmpty(entityList)) {
            return EmptyUtil.emptyMap();
        }
        return entityList.stream().collect(Collectors.toMap(AnchorUrlEntity::getId, Function.identity()));
    }

    /**
     * 根据主播抖音号查询主播
     *
     * @param anchorNumber 主播抖音号
     * @param platform     平台 0：抖音 1：快手 2：视频号
     *
     * @return 主播
     */
    @Override
    public Optional<AnchorUrlEntity> findByAnchorNumber(String anchorNumber, Integer platform) {
        if (EmptyUtil.isEmpty(anchorNumber)) {
            return Optional.empty();
        }
        return super.lambdaQuery()
            .eq(AnchorUrlEntity::getAnchorNumber, anchorNumber)
            .eq(platform != null , AnchorUrlEntity::getPlatform, platform)
            .last("limit 1")
            .oneOpt();
    }

    /**
     * 根据主播抖音号查询主播
     *
     * @param anchorNumbers 主播抖音号
     * @param platform      0：抖音 1：快手 2：视频号
     *
     * @return 主播
     */
    @Override
    public Map<String, AnchorUrlEntity> getAnchorMapByAnchorNumber(List<String> anchorNumbers, Integer platform) {
        if (EmptyUtil.isEmpty(anchorNumbers)) {
            return EmptyUtil.emptyMap();
        }
        return super.lambdaQuery()
            .eq(platform != null , AnchorUrlEntity::getPlatform, platform)
            .in(AnchorUrlEntity::getAnchorNumber, anchorNumbers)
            .list().stream().collect(Collectors.toMap(AnchorUrlEntity::getAnchorNumber, Function.identity(), FunctionUtil::mergeFirst));
    }
}
