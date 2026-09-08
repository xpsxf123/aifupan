package com.jiuyu.replay.words.handler;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.toolkit.ChainWrappers;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.replay.common.constant.UserEnums;
import com.jiuyu.replay.generic.dto.power.UserDto;
import com.jiuyu.replay.generic.feign.power.UserFeign;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.power.UserCacheVo;
import com.jiuyu.replay.words.bo.video.VideoBelongUserBO;
import com.jiuyu.replay.words.entity.AnchorVideoEntity;
import com.jiuyu.replay.words.entity.SyncContrastEntity;
import com.jiuyu.replay.words.entity.UploadFileEntity;
import com.jiuyu.replay.words.enums.VideoSourceType;
import com.jiuyu.replay.words.repository.dao.AnchorVideoDao;
import com.jiuyu.replay.words.repository.dao.SyncContrastDao;
import com.jiuyu.replay.words.repository.dao.UploadFilesDao;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 各种类型视频的统一抽象逻辑 用于简化业务对接各类型的视频来源
 *
 * @author HeHui
 * @date 2025-06-07 15:11
 */
@Component
public class VideoCombinationHandler {

    private final Map<VideoSourceType, BiFunction<Collection<String>, Long, List<VideoBelongUserBO>>> videoBelongUserMap = new HashMap<>();

    private final UserFeign userFeign;

    public VideoCombinationHandler(UserFeign userFeign, UploadFilesDao uploadFilesDao, AnchorVideoDao anchorVideoDao, SyncContrastDao syncContrastDao) {
        this.userFeign = userFeign;
        this.initFunction(VideoSourceType.UPLOAD, uploadFilesDao, UploadFileEntity::getFileId, UploadFileEntity::getUserId, UploadFileEntity::getTenantId, UploadFileEntity::getIsDeleted, UploadFileEntity::getUploadStatus);
        this.initFunction(VideoSourceType.LOCAL, anchorVideoDao, AnchorVideoEntity::getVideoId, AnchorVideoEntity::getUserId, AnchorVideoEntity::getTenantId, AnchorVideoEntity::getIsDeleted, AnchorVideoEntity::getUploadStatus);
        this.initFunction(VideoSourceType.CONTRAST, syncContrastDao, SyncContrastEntity::getContrastId, SyncContrastEntity::getUserId, SyncContrastEntity::getTenantId, SyncContrastEntity::getIsDeleted, null);
    }


    /**
     * 初始化视频来源对应的归属用户查询逻辑
     *
     * <p>该方法用于为不同类型的视频来源（如上传文件、本地视频等）注册统一的归属用户查询逻辑。
     * 通过传入的 Mapper 和一组函数式参数，构建一个能够根据视频 ID 查询其所属用户的 Lambda 表达式，
     * 并将其缓存到 {@link #videoBelongUserMap} 中，便于后续复用。</p>
     *
     * <p>此方法的主要作用包括：</p>
     * <ul>
     *   <li>将不同类型视频源的归属用户查询逻辑抽象统一处理</li>
     *   <li>使用泛型方式支持任意实体类和字段，增强代码灵活性和复用性</li>
     *   <li>通过 Lambda 表达式封装查询逻辑，提高可读性和可维护性</li>
     * </ul>
     *
     * @param sourceType   视频来源类型，表示当前初始化的是哪种视频类型的归属用户逻辑
     * @param mapper       数据库操作对象，用于执行对特定实体的数据库查询
     * @param getId        用于从实体中提取 ID 的函数
     * @param getUserId    用于从实体中提取用户 ID 的函数
     * @param getTenantId  用于从实体中提取租户 ID 的函数
     * @param getIsDeleted 用于从实体中提取删除标识的函数，可能为 null
     * @param <T>          数据库实体类型
     * @param <ID>         实体 ID 的类型
     * @param <D>          删除标识字段的类型
     */
    private <T, ID, D> void initFunction(VideoSourceType sourceType, BaseMapper<T> mapper,
                                         SFunction<T, String> getId,
                                         SFunction<T, Long> getUserId,
                                         SFunction<T, Long> getTenantId,
                                         SFunction<T, D> getIsDeleted,
                                         SFunction<T, Integer> getUpload) {
        videoBelongUserMap.put(sourceType, (ids, tenantId) -> {
            if (EmptyUtil.isEmpty(ids)) {
                return List.of();
            }
            LambdaQueryChainWrapper<T> chainWrapper = ChainWrappers.lambdaQueryChain(mapper);
            if (getUpload != null) {
                chainWrapper.select(getId, getUserId, getTenantId, getUpload);
            } else {
                chainWrapper.select(getId, getUserId, getTenantId);
            }
            return chainWrapper.in(getId, ids)
                .eq(tenantId != null, getTenantId, tenantId)
                .eq(getIsDeleted != null, getIsDeleted, 0)
                .list().stream().map(video -> VideoBelongUserBO.of(getUserId.apply(video), getTenantId.apply(video), getUpload != null && getUpload.apply(video) == 1, getId.apply(video))).toList();

        });
    }


    /**
     * 根据视频ID和来源类型查找视频所属用户信息
     * 此方法用于确定给定视频属于哪个用户，基于视频ID、来源类型和租户ID进行查询
     * 使用Optional封装返回结果，以优雅地处理可能的空值情况
     *
     * @param videoId    视频ID，用于标识特定的视频
     * @param sourceType 视频来源类型，用于区分视频的来源渠道
     * @param tenantId   租户ID，用于多租户环境下的数据隔离 (非必填)
     *
     * @return Optional<VideoBelongUserBO> 返回一个Optional封装的用户信息对象VideoBelongUserBO，
     *     如果找到视频所属用户，则返回包含用户信息的对象；否则返回空Optional
     */
    public Optional<VideoBelongUserBO> findVideoBelongUser(String videoId, VideoSourceType sourceType, Long tenantId) {
        if (EmptyUtil.isEmpty(videoId)) {
            return Optional.empty();
        }
        BiFunction<Collection<String>, Long, List<VideoBelongUserBO>> belongUserFunction = videoBelongUserMap.get(sourceType);
        if (belongUserFunction == null) {
            return Optional.empty();
        }
        List<VideoBelongUserBO> list = belongUserFunction.apply(List.of(videoId), tenantId);
        if (EmptyUtil.isEmpty(list)) {
            return Optional.empty();
        }
        return list.stream().findFirst().map(user -> {
            if (user.userType() == null) {
                UserDto userDto = userFeign.userById(user.userId());
                if (userDto == null) {
                    return null;
                }
                return user.full(userDto);
            }
            return user;
        });
    }


    /**
     * 根据视频ID集合和来源类型查找视频所属用户信息
     * 此方法用于批量查找多个视频的所属用户信息，基于视频ID、来源类型进行查询
     * 使用Map封装返回结果，以方便后续处理
     *
     * @param videoIdMap  视频ID集合，用于标识特定的视频
     *
     * @return Map<VideoSourceType, Map<String, VideoBelongUserBO>> 返回一个Map，键为视频来源类型，值为一个Map，
     *     键为视频ID，值为对应的用户信息对象VideoBelongUserBO
     */
    public Map<VideoSourceType, Map<String, VideoBelongUserBO>> findVideoBelongUserMap(Map<VideoSourceType, List<String>> videoIdMap) {
        if (EmptyUtil.isEmpty(videoIdMap)) {
            return Map.of();
        }

        return videoIdMap.entrySet().stream().map(entry -> {
            VideoSourceType sourceType = entry.getKey();
            Collection<String> videoIds = entry.getValue();
            BiFunction<Collection<String>, Long, List<VideoBelongUserBO>> belongUserFunction = videoBelongUserMap.get(sourceType);
            if (belongUserFunction == null) {
                return null;
            }
            List<VideoBelongUserBO> list = belongUserFunction.apply(videoIds, null);
            if (EmptyUtil.isEmpty(list)) {
                return null;
            }
            return Map.entry(sourceType, list.stream().collect(Collectors.toMap(VideoBelongUserBO::videoId, Function.identity())));
        }).filter(Objects::nonNull).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }


    /**
     * 检查用户是否具有对指定视频的编辑权限
     * <p>
     * 该方法用于验证当前用户是否有权限对特定视频进行编辑操作。
     * 主要通过以下步骤完成权限校验：
     * <ul>
     *   <li>检查用户是否登录</li>
     *   <li>查找视频所属的用户信息（BelongUserBO）</li>
     *   <li>判断用户是否为视频的所有者或其父用户，或者是否是特殊类型用户（如后台用户）</li>
     * </ul>
     *
     * @param videoId    视频ID，用于标识需要校验权限的视频资源
     * @param sourceType 视频来源类型，用于区分视频的不同来源渠道
     * @param user       当前操作用户的缓存信息对象，包含用户ID、父ID等关键信息
     *
     * @return R<VideoBelongUserBO> 返回一个封装了操作结果的响应对象，若无权限则返回错误码和提示信息；否则返回成功状态
     */
    public R<VideoBelongUserBO> checkVideoEditPermission(String videoId, VideoSourceType sourceType, UserCacheVo user) {
        // 检查用户是否登录
        if (user == null) {
            return R.error(UserEnums.CodeMsgEnum.NO_LOGIN.getCode(), "请先登录");
        }

        // 获取视频归属用户信息
        Optional<VideoBelongUserBO> userOptional = this.findVideoBelongUser(videoId, sourceType, user.getActiveTenantId());
        if (userOptional.isEmpty()) {
            return R.error(UserEnums.CodeMsgEnum.TIP_CUSTOM.getCode(), "无法找到视频源");
        }

        // 检查用户是否有编辑视频的权限
        boolean hasPermission = userOptional
            .filter(belongUser -> {
                // 管理员权限用户直接放行
                if (Objects.equals(belongUser.userType(), 1)) {
                    return true;
                }
                // 如果视频已上传，检查用户是否属于同一租户
                if (belongUser.uploadStatus()) {
                    return Objects.equals(belongUser.tenantId(), user.getActiveTenantId());
                }
                // 判断是否为本人或上级用户
                return Objects.equals(belongUser.userId(), user.getId())
                    || Objects.equals(belongUser.parentId(), user.getId());
            })
            .isPresent();

        // 如果用户没有权限，返回错误信息
        if (!hasPermission) {
            return R.error(UserEnums.CodeMsgEnum.NO_POWER.getCode(), "您没有当前视频权限");
        }

        // 用户有权限，返回成功状态
        return R.ok(userOptional.get());
    }


    /**
     * 根据视频ID和来源类型查询权限
     * 此方法用于确定用户是否有权限访问特定的视频资源
     * 它首先检查用户是否已登录（即用户对象是否非空）如果用户未登录，则使用失败提供的默认值
     * 如果用户已登录，则使用成功获取结果
     *
     * @param videoId      视频的唯一标识符
     * @param sourceType   视频来源的类型，可能影响权限查询的结果
     * @param user         表示当前用户的对象，用于检查用户是否已登录
     * @param supplier     一个函数，用于在用户已登录时提供查询结果
     * @param failSupplier 一个备用函数，用于在用户未登录时提供失败或默认结果
     * @param <T>          泛型参数，表示提供的结果的类型
     *
     * @return 返回提供的结果，如果用户未登录则返回失败提供的结果
     */
    public <T> T queryPermission(String videoId, VideoSourceType sourceType, UserCacheVo user, Supplier<T> supplier, Supplier<T> failSupplier) {
        // 检查用户是否为空，如果为空则用户未登录，返回错误信息
        if (user == null || StrUtil.isBlank(videoId) || sourceType == null) {
            return failSupplier.get();
        }
        // 用户已登录，通过函数获取并返回结果
        return supplier.get();
    }


}
