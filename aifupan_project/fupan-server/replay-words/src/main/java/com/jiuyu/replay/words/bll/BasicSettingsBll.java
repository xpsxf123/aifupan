package com.jiuyu.replay.words.bll;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.jiuyu.replay.common.constant.WordsEnum;
import com.jiuyu.replay.generic.vo.words.AnchorVideoInfoVo;
import com.jiuyu.replay.words.bo.BasicSettingsBo;
import com.jiuyu.replay.words.bo.video.UpdateAiPartialBo;
import com.jiuyu.replay.words.entity.BasicSettingsEntity;
import com.jiuyu.replay.words.producer.AnchorVideoProducer;
import com.jiuyu.replay.words.producer.BasicSettingsProducer;
import com.jiuyu.replay.words.producer.UploadFileProducer;
import com.jiuyu.replay.generic.vo.words.BasicSettingsInfoVo;
import com.jiuyu.replay.generic.vo.words.BasicSettingsVo;
import com.jiuyu.replay.words.vo.file.UploadFileInfoVo;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * @author ：lujie
 * @description：基础设置业务逻辑类
 * @date ：2025/1/7
 */
@Component
@AllArgsConstructor
public class BasicSettingsBll {

    private final BasicSettingsProducer basicSettingsProducer;
    private final AnchorVideoProducer anchorVideoProducer;
    private final UploadFileProducer uploadFileProducer;

    /**
     * 根据ID查询基础设置
     *
     * @param id ID
     * @return 基础设置VO
     */
    public BasicSettingsVo getById(Long id) {
        BasicSettingsEntity entity = basicSettingsProducer.getById(id);
        return BeanUtil.copyProperties(entity, BasicSettingsVo.class);
    }

    /**
     * 根据ID删除基础设置
     *
     * @param id ID
     * @return 删除结果
     */
    public boolean deleteById(Long id) {
        return basicSettingsProducer.deleteById(id);
    }

    /**
     * 根据来源ID、来源类型、用户ID和租户ID查询单个基础设置
     *
     * @param sourceId   来源ID
     * @param sourceType 来源类型
     * @param userId     用户ID
     * @param tenantId   租户ID
     * @return 基础设置VO
     */
    public BasicSettingsVo getBySourceUser(String sourceId, Integer sourceType, Long userId, Long tenantId) {
        return basicSettingsProducer.getBySourceUser(sourceId, sourceType, userId, tenantId);
    }

    /**
     * 更新基础设置
     *
     * @param basicSettingsBo 基础设置VO
     * @return 更新结果
     */
    public BasicSettingsInfoVo updateAiPartialNew(BasicSettingsBo basicSettingsBo) {
        BasicSettingsInfoVo res = new BasicSettingsInfoVo();
        if (!changeAndSetUpdateAiPartial(basicSettingsBo)) {
            res.setIsUpdate(false);
            return res;
        }

        // 更新基础设置
        BasicSettingsBo basicSettingsBo1 = basicSettingsProducer.updateAiPartialNew(basicSettingsBo);

        res.setId(basicSettingsBo1.getId());
        res.setIsUpdate(true);
        return res;
    }

    /**
     * 检查和更新基础设置
     *
     * @param basicSettingsBo 基础设置VO
     * @return 更新结果
     */
    private boolean changeAndSetUpdateAiPartial(BasicSettingsBo basicSettingsBo) {

        if (ObjectUtil.equals(basicSettingsBo.getSourceType(), WordsEnum.basicSettingsType.ANCHOR.getCode())) {
            return true;
        }

        if (ObjectUtil.equals(basicSettingsBo.getSourceType(), WordsEnum.basicSettingsType.VIDEO.getCode())) {
            // 获取视频信息
            AnchorVideoInfoVo video = anchorVideoProducer.getByVideoId(basicSettingsBo.getSourceId());
            if (video == null || !ObjectUtil.equals(video.getUserId(), basicSettingsBo.getUserId()) || !ObjectUtil.equals(video.getTenantId(), basicSettingsBo.getTenantId())) {
                return false;
            }
            basicSettingsBo.setSourceType(WordsEnum.basicSettingsType.ANCHOR.getCode());
            basicSettingsBo.setSourceId(video.getSecUid());
            return true;
        } else if (ObjectUtil.equals(basicSettingsBo.getSourceType(), WordsEnum.basicSettingsType.FILE.getCode())) {
            // 获取文件信息
            UploadFileInfoVo uploadFile = uploadFileProducer.getByFileId(basicSettingsBo.getSourceId());
            return uploadFile != null && ObjectUtil.equals(uploadFile.getUserId(), basicSettingsBo.getUserId()) && ObjectUtil.equals(uploadFile.getTenantId(), basicSettingsBo.getTenantId());
        }

        return false;
    }

    public BasicSettingsVo getAiPartial(String sourceId, Integer sourceType, Long userId, Long tenantId) {

        if (ObjectUtil.equals(sourceType, WordsEnum.basicSettingsType.VIDEO.getCode())) {
            // 获取视频信息
            AnchorVideoInfoVo video = anchorVideoProducer.getByVideoId(sourceId);
            if (video == null) {
                throw new RuntimeException("视频不存在");
            }
            sourceId = video.getSecUid();
            sourceType = WordsEnum.basicSettingsType.ANCHOR.getCode();
            userId = video.getUserId();
            tenantId = video.getTenantId();
        } else if (ObjectUtil.equals(sourceType, WordsEnum.basicSettingsType.FILE.getCode())) {
            // 获取文件信息
            UploadFileInfoVo uploadFile = uploadFileProducer.getByFileId(sourceId);
            if (uploadFile == null) {
                throw new RuntimeException("文件不存在");
            }
            sourceId = uploadFile.getFileId();
            sourceType = WordsEnum.basicSettingsType.FILE.getCode();
            userId = uploadFile.getUserId();
            tenantId = uploadFile.getTenantId();
        }

        return basicSettingsProducer.getBySourceUser(sourceId, sourceType, userId, tenantId);
    }

    /**
     * 获取ai页面的部分主播字段
     *
     * @param videoId 视频id
     * @return ai页面的部分主播字段
     */
    public UpdateAiPartialBo getAiPartial(String videoId) {
        // 获取视频信息
        AnchorVideoInfoVo video = anchorVideoProducer.getByVideoId(videoId);
        if (video == null) {
            throw new RuntimeException("视频不存在");
        }
        String sourceId = video.getSecUid();

        BasicSettingsVo basicSettings = basicSettingsProducer.getBySourceUser(sourceId, WordsEnum.basicSettingsType.ANCHOR.getCode(), video.getUserId(), video.getTenantId());
        if (basicSettings == null) {
            return null;
        }

        UpdateAiPartialBo updateAiPartialBo = BeanUtil.copyProperties(basicSettings, UpdateAiPartialBo.class);
        updateAiPartialBo.setVideoId(video.getVideoId());
        updateAiPartialBo.setSecUid(video.getSecUid());
        updateAiPartialBo.setUserId(video.getUserId());
        updateAiPartialBo.setTenantId(video.getTenantId());
        return updateAiPartialBo;
    }
}