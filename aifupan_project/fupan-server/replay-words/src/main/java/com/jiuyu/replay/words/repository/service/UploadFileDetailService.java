package com.jiuyu.replay.words.repository.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jiuyu.replay.generic.vo.words.VideoContentVo;
import com.jiuyu.replay.words.entity.UploadFileDetailEntity;

import java.util.List;

/**
 * 文件的详情
 *
 * @author lyw
 * @email 1798883178@qq.com
 * @date 2025-06-03 14:14:53
 */
public interface UploadFileDetailService extends IService<UploadFileDetailEntity> {


    List<VideoContentVo> selFileContents(String sourceId, Integer type, Integer sourceType, Long userId, Long tenantId);

    void inserto(String sourceId, Integer type, Integer sourceType, Long userId, Long tenantId);

    /**
     * 查询待生成原文的文件详情
     */
    List<UploadFileDetailEntity> selectFileDetailData(Integer limit);
}

