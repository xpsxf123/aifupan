package com.jiuyu.replay.words.bll;

import cn.hutool.core.util.ObjectUtil;
import com.jiuyu.replay.common.constant.Constant;
import com.jiuyu.replay.generic.enums.words.SourceStarTypeEnum;
import com.jiuyu.replay.generic.feign.power.UserFeign;
import com.jiuyu.replay.generic.utils.ResultUtil;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.power.UserCacheVo;
import com.jiuyu.replay.generic.vo.words.SourceStarVo;
import com.jiuyu.replay.words.bo.SourceStarAddBo;
import com.jiuyu.replay.words.bo.SourceStarCancelBo;
import com.jiuyu.replay.words.producer.SyncContrastProducer;
import com.jiuyu.replay.words.rse.SourceStarRse;
import com.jiuyu.replay.words.vo.SyncContrastInfoVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 星标业务逻辑层
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2026-01-07
 */
@Component
public class SourceStarBll {

    @Resource
    private UserFeign userFeign;
    @Resource
    private SourceStarRse sourceStarRse;
    @Resource
    private SyncContrastProducer syncContrastProducer;

    /**
     * 添加星标
     *
     * @param addBo 添加星标参数
     * @return 操作结果
     */
    @Transactional(rollbackFor = Exception.class)
    public R<String> addStar(SourceStarAddBo addBo) {
        // 参数校验
        if (addBo == null || !StringUtils.hasText(addBo.getSourceId()) || addBo.getSourceType() == null) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "参数不能为空");
        }

        // 获取当前登录用户信息
        UserCacheVo user = ResultUtil.getResult(userFeign.getLocalUser());
        if (user == null) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "用户未登录");
        }
        Long userId = user.getId();
        Long tenantId = user.getActiveTenantId();

        // 检查该来源是否已存在星标记录
        if (sourceStarRse.existsStar(addBo.getSourceId(), addBo.getSourceType())) {
            return R.ok("星标已存在");
        }

        // 保存星标记录
        sourceStarRse.save(addBo.getSourceId(), addBo.getSourceType(), userId, tenantId);

        // 如果是对比类型，还需要给关联的视频或文件添加星标
        if (addBo.getSourceType().equals(SourceStarTypeEnum.CONTRAST.getCode())) {
            addStarForContrastRelated(addBo.getSourceId(), userId, tenantId);
        }

        return R.ok("添加星标成功");
    }

    /**
     * 为对比记录关联的视频或文件添加星标
     *
     * @param contrastId 对比id
     * @param userId     用户id
     * @param tenantId   租户id
     */
    private void addStarForContrastRelated(String contrastId, Long userId, Long tenantId) {
        // 获取对比记录信息
        SyncContrastInfoVo contrastInfo = syncContrastProducer.infoByContrastId(contrastId);
        if (contrastInfo == null) {
            return;
        }

        // 根据对比类型判断是视频对比还是文件对比
        if (contrastInfo.getContrastType() != null && contrastInfo.getContrastType() == 0) {
            // 视频对比，给两个视频添加星标
            addStarIfNotExists(contrastInfo.getVideoOneId(), SourceStarTypeEnum.VIDEO.getCode(), userId, tenantId);
            addStarIfNotExists(contrastInfo.getVideoTwoId(), SourceStarTypeEnum.VIDEO.getCode(), userId, tenantId);
        } else if (contrastInfo.getContrastType() != null && contrastInfo.getContrastType() == 1) {
            // 文件对比，给两个文件添加星标
            addStarIfNotExists(contrastInfo.getFileOneId(), SourceStarTypeEnum.FILE.getCode(), userId, tenantId);
            addStarIfNotExists(contrastInfo.getFileTwoId(), SourceStarTypeEnum.FILE.getCode(), userId, tenantId);
        }
    }

    /**
     * 如果不存在星标则添加
     *
     * @param sourceId   来源id
     * @param sourceType 来源类型
     * @param userId     用户id
     * @param tenantId   租户id
     */
    private void addStarIfNotExists(String sourceId, Integer sourceType, Long userId, Long tenantId) {
        if (!StringUtils.hasText(sourceId)) {
            return;
        }
        // 检查是否已存在星标
        if (!sourceStarRse.existsStar(sourceId, sourceType)) {
            sourceStarRse.save(sourceId, sourceType, userId, tenantId);
        }
    }

    /**
     * 取消星标
     *
     * @param cancelBo 取消星标参数
     * @return 操作结果
     */
    public R<String> cancelStar(SourceStarCancelBo cancelBo) {
        // 参数校验
        if (cancelBo == null || !StringUtils.hasText(cancelBo.getSourceId()) || cancelBo.getSourceType() == null) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "参数不能为空");
        }

        // 获取当前登录用户信息
        UserCacheVo user = ResultUtil.getResult(userFeign.getLocalUser());
        if (user == null) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "用户未登录");
        }

        // 获取星标记录
        SourceStarVo starVo = sourceStarRse.getBySourceIdAndType(cancelBo.getSourceId(), cancelBo.getSourceType());
        if (starVo == null) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "星标记录不存在");
        }

        // 校验权限：登录用户的租户id和星标记录一致才能取消
        if (!ObjectUtil.equals(user.getActiveTenantId(), starVo.getTenantId())) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "没有权限取消该星标");
        }

        // 如果是子账号，用户id和星标记录一致才能取消
        // userType == 2 表示子账号
        if (user.getUserType() != null && user.getUserType() == 2) {
            if (!ObjectUtil.equals(user.getId(), starVo.getUserId())) {
                return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "没有权限取消该星标");
            }
            // 子账号删除时需要匹配userId
            sourceStarRse.remove(cancelBo.getSourceId(), cancelBo.getSourceType());
        } else {
            // 主账号删除时不需要匹配userId
            sourceStarRse.remove(cancelBo.getSourceId(), cancelBo.getSourceType());
        }

        return R.ok("取消星标成功");
    }
}
