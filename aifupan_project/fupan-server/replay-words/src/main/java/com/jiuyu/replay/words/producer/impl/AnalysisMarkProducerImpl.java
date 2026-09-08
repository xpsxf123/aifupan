package com.jiuyu.replay.words.producer.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.jiuyu.replay.common.annotation.CustomRedissonLock;
import com.jiuyu.replay.common.constant.Constant;
import com.jiuyu.replay.common.utils.BeanConvertUtils;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.feign.power.UserFeign;
import com.jiuyu.replay.generic.utils.ResultUtil;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.power.UserCacheVo;
import com.jiuyu.replay.words.bo.video.AnalysisMarkInfoBo;
import com.jiuyu.replay.words.bo.video.VideoBelongUserBO;
import com.jiuyu.replay.words.entity.AnalysisMarkEntity;
import com.jiuyu.replay.words.enums.VideoSourceType;
import com.jiuyu.replay.words.handler.VideoCombinationHandler;
import com.jiuyu.replay.words.producer.AnalysisMarkProducer;
import com.jiuyu.replay.words.repository.service.AnalysisMarkService;
import com.jiuyu.replay.words.vo.AnalysisMarkVo;
import jakarta.annotation.Resource;
import org.apache.poi.ss.formula.functions.T;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 标注生产者实现类
 * 
 * @author liaoxin
 * @date 2025-06-07
 * @description 实现视频段落标注的业务逻辑处理，包括标注的增删改查功能，支持分布式锁和事务管理
 */
@Component
public class AnalysisMarkProducerImpl implements AnalysisMarkProducer {

    @Resource
    private AnalysisMarkService analysisMarkService;

    @Resource
    private VideoCombinationHandler videoCombinationHandler;
    @Resource
    private UserFeign userFeign;

    /**
     * 添加分析标注记录
     * 
     * @param bo 标注信息业务对象，包含标注的详细信息
     * @return {@link R}<{@link Long}> 操作结果，成功时返回标注ID
     * @description 使用分布式锁确保编号生成和数据保存的原子性，防止并发情况下的数据冲突，考虑到租户下用户不多，
     *              并发量不会太高，所以分布式锁粒度稍大，后续性能需要再优化双重检查锁定模式或者redis取数器模式
     */
    @Override
    @CustomRedissonLock(key = "'analysis_mark_add:' + #args[0].sourceId + ':' + #args[0].sourceType ", waitTime = 20, leaseTime = 60)
    @Transactional(rollbackFor = Exception.class)
    public R<Long> add(AnalysisMarkInfoBo bo) {
        // 视频类型校验
        Optional<VideoSourceType> videoSourceType = VideoSourceType.codeOf(bo.getSourceType());
        if (videoSourceType.isEmpty()) {
            return R.error(500, "错误的视频类型");
        }

        // 权限校验
        UserCacheVo user = ResultUtil.getUserResult(userFeign.getLocalUser());
        R<VideoBelongUserBO> permissionResult = videoCombinationHandler.checkVideoEditPermission(
                bo.getSourceId(), videoSourceType.get(), user);
        if (permissionResult.fail()) {
            return R.error(permissionResult.getCode(), permissionResult.getMsg());
        }

        // 检查是否已存在相同段落编号的记录（业务限制：每个段落内只能有一次标注）
         Integer checkIndexOverlap = analysisMarkService.checkIndexOverlap(bo.getSourceId(), bo.getSourceType(),bo.getParaphStartNo(),bo.getParaphEndNo(),
         bo.getMarkStartIndex(),bo.getMarkEndIndex());  

        if (checkIndexOverlap>0) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "该段落已被标注");
        }

        // 查询当前视频ID和视频类型下的最大编号并生成新编号
        Integer maxMarkNo = analysisMarkService.getMaxMarkNo(bo.getSourceId(), bo.getSourceType());

        // 设置新编号（递增）
        Integer newMarkNo = maxMarkNo + 1;
        bo.setMarkNo(newMarkNo);

        // 转换业务对象为实体对象并保存
        AnalysisMarkEntity entity = BeanConvertUtils.convert(bo, AnalysisMarkEntity.class);
        entity.setId(SnowflakeManager.nextValue());
        boolean saved = analysisMarkService.save(entity);

        return saved ? R.ok(entity.getId()) : R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "添加失败");
    }

    /**
     * 查询视频标注信息
     * 
     * @param sourceId      视频ID，用于查询指定视频的标注
     * @param sourceType    视频来源类型，配合视频ID进行精确查询
     * @param startParaphNo 段落编号（开始编号），可选参数，用于范围查询
     * @param endParaphNo   段落编号（结束编号），可选参数，用于范围查询
     * @return {@link List}<{@link AnalysisMarkVo}> 标注信息列表
     * @description 根据视频ID和来源类型查询标注信息，支持按段落编号范围过滤，只返回未删除的记录
     */
    @Override
    public R<List<AnalysisMarkVo>> info(String sourceId, Integer sourceType, Integer startParaphNo,
            Integer endParaphNo) {
        // 构建查询条件
        LambdaQueryChainWrapper<AnalysisMarkEntity> queryWrapper = analysisMarkService.lambdaQuery()
            .eq(AnalysisMarkEntity::getSourceId, sourceId)
            .eq(AnalysisMarkEntity::getSourceType, sourceType);
        // 如果指定了段落编号范围，则添加范围查询条件
        if (startParaphNo != null && endParaphNo != null) {
            queryWrapper.ge(AnalysisMarkEntity::getParaphStartNo, startParaphNo);
            queryWrapper.le(AnalysisMarkEntity::getParaphEndNo, endParaphNo);
        }
        // 查询数据并转换为VO对象
        List<AnalysisMarkEntity> list = queryWrapper.list();
        // 按照段落编号升序，再按照段落标注开始索引升序，此处不用数据库排序可节省数据库性能
        return R.ok(list.stream().sorted(Comparator.comparing(AnalysisMarkEntity::getParaphStartNo).thenComparing(AnalysisMarkEntity::getMarkStartIndex))
            .map(analysisMarkEntity -> {
            AnalysisMarkVo vo = new AnalysisMarkVo();
            BeanUtils.copyProperties(analysisMarkEntity, vo);
            return vo;
        }).collect(Collectors.toList()));
    }

    /**
     * 修改标注信息
     * 
     * @param bo 修改数据，包含需要更新的标注信息
     * @return {@link R}<{@link Long}> 操作结果，成功时返回标注ID
     * @description 更新已存在的标注记录，支持修改标注内容或删除标注，包含权限校验
     */
    @Override
    public R<Long> edit(AnalysisMarkInfoBo bo) {
        // 根据ID查询现有标注记录
        AnalysisMarkEntity entity = analysisMarkService.getById(bo.getId());
        if (entity == null) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "标注不存在，修改失败");
        }

        // 权限校验
        Optional<VideoSourceType> codeOf = VideoSourceType.codeOf(entity.getSourceType());
        // 校验用户是否具有编辑视频标注的权限
        UserCacheVo user = ResultUtil.getUserResult(userFeign.getLocalUser());
        R<VideoBelongUserBO> permissionResult = videoCombinationHandler.checkVideoEditPermission(entity.getSourceId(),
                codeOf.get(), user);
        if (permissionResult.fail()) {
            // 权限校验失败，直接返回错误响应
            return R.error(permissionResult.getCode(), permissionResult.getMsg());
        }

        // 修改只能修改对应的文本区域
        entity.setMarkContent(bo.getMarkContent());
        entity.setUpdateTime(new Date());
        entity.setUpdateUserId(user.getId());


        // 执行更新操作
        boolean updated = analysisMarkService.updateById(entity);
        return updated ? R.ok(entity.getId()) : R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "修改失败");
    }

    /**
     * 删除标注信息
     * 
     * @param id 删除标注
     * @return {@link R}<{@link Long}> 操作结果，成功时返回标注ID
     * @description 删除标注
     */
    @Override
    public R<Long> delete(Long id) {
        // 根据ID查询现有标注记录
        AnalysisMarkEntity entity = analysisMarkService.getById(id);
        if (entity == null) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "标注不存在，删除失败");
        }

        // 权限校验
        Optional<VideoSourceType> codeOf = VideoSourceType.codeOf(entity.getSourceType());
        // 校验用户是否具有编辑视频标注的权限
        UserCacheVo user = ResultUtil.getUserResult(userFeign.getLocalUser());
        R<VideoBelongUserBO> permissionResult = videoCombinationHandler.checkVideoEditPermission(entity.getSourceId(),
                codeOf.get(), user);
        if (permissionResult.fail()) {
            // 权限校验失败，直接返回错误响应
            return R.error(permissionResult.getCode(), permissionResult.getMsg());
        }

        // 更新时间
        entity.setUpdateTime(new Date());

        entity.setUpdateUserId(user.getId());
        // 执行更新操作
        boolean updated = analysisMarkService.removeById(entity);
        return updated ? R.ok(entity.getId()) : R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "修改失败");
    }


    /**
     * 批量查询视频标注信息并填充
     *
     * @param items      视频ID列表，用于批量查询指定视频的标注
     * @param sourceType    视频来源类型，配合视频ID进行精确查询
     * @param setExistsMark 批量查询结果处理函数，用于设置查询结果
     */
    @Override
    public <T> void fullExistsMark(List<T> items, Function<T, String> getVideoId, VideoSourceType sourceType, BiConsumer<T, Integer> setExistsMark) {
        if (CollUtil.isEmpty(items) || sourceType == null || getVideoId == null) {
            return;
        }
        List<String> videoIds = items.stream().map(getVideoId).toList();
        List<String> existsMarkVideoIds = analysisMarkService.videoExistsMark(videoIds, sourceType);
        if (CollUtil.isEmpty(existsMarkVideoIds)) {
            return;
        }
        items.forEach(item -> {
            setExistsMark.accept(item, existsMarkVideoIds.contains(getVideoId.apply(item)) ? 1 : 0);
        });
    }
}
