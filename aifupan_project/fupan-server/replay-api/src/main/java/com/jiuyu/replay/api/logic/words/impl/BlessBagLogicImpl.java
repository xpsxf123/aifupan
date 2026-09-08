package com.jiuyu.replay.api.logic.words.impl;

import cn.hutool.core.collection.CollUtil;
import com.jiuyu.framework.function.BatchQuery;
import com.jiuyu.replay.api.logic.words.BlessBagLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

import com.jiuyu.replay.generic.vo.words.BlessBagListVo;
import com.jiuyu.replay.generic.vo.words.BlessBagInfoVo;
import com.jiuyu.replay.words.bo.BlessBagBo;
import com.jiuyu.replay.words.bo.BlessBagListBo;

import com.jiuyu.replay.words.bll.BlessBagBll;
import com.jiuyu.replay.words.entity.BlessBagEntity;
import com.jiuyu.replay.words.repository.service.BlessBagService;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


/**
 * 福袋信息
 *
 * @author lyw
 * @email 1798883178@qq.com
 * @date 2025-05-22 20:02:21
 */
@Service
public class BlessBagLogicImpl implements BlessBagLogic {

    @Resource
    private BlessBagBll blessBagBll;
    @Resource
    private BlessBagService blessBagService;


    @Override
    public R<PageUtils<BlessBagListVo>> queryPage(BlessBagListBo blessBagListBo) {

        return blessBagBll.queryPage(blessBagListBo);
    }

    @Override
    public R<BlessBagInfoVo> info(Long id) {

        return blessBagBll.info(id);
    }

    @Override
    public R<List<BlessBagInfoVo>> infoByVideo(String videoId) {

        return blessBagBll.infoByVideo(videoId);
    }

    @Override
    public R<String> save(BlessBagBo blessBagBo) {

        return blessBagBll.save(blessBagBo);
    }

    @Override
    public R<String> update(BlessBagBo blessBagBo) {

        return blessBagBll.update(blessBagBo);
    }

    @Override
    public R<String> delete(Long id) {

        return blessBagBll.delete(id);
    }

    @Override
    public R<List<BlessBagListVo>> listByVideoIds(Long tenantId, List<String> videoIds) {

        List<BlessBagEntity> entities = new ArrayList<>();
        for (List<String> partVideoIds : CollUtil.split(videoIds, 500)) {
            entities.addAll(new BatchQuery<>((limit, idx) -> blessBagService.lambdaQuery()
                    .eq(BlessBagEntity::getTenantId, tenantId)
                    .eq(BlessBagEntity::getIsDeleted, 0)
                    .in(BlessBagEntity::getVideoId, partVideoIds)
                    .gt(idx != null, BlessBagEntity::getId, idx)
                    .orderByAsc(BlessBagEntity::getId)
                    .last("limit " + limit)
                    .list(), BlessBagEntity::getId).get());
        }

        List<BlessBagListVo> vos = entities.stream().map(entity -> {
            BlessBagListVo vo = new BlessBagListVo();
            BeanUtils.copyProperties(entity, vo);
            return vo;
        }).collect(Collectors.toList());
        return R.ok("获取成功", vos);
    }

}

