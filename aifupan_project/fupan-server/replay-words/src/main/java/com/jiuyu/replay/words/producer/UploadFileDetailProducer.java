package com.jiuyu.replay.words.producer;


import com.jiuyu.replay.generic.bo.words.GenerateVideoContentBo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.words.VideoContentVo;
import com.jiuyu.replay.words.bo.file.UploadFileDetailBo;
import com.jiuyu.replay.words.bo.file.UploadFileDetailListBo;
import com.jiuyu.replay.words.vo.file.UploadFileDetailInfoVo;
import com.jiuyu.replay.words.vo.file.UploadFileDetailListVo;
import com.jiuyu.replay.words.vo.file.UploadFileDetailVo;
import com.jiuyu.replay.words.vo.video.AnchorVideoFileAllVo;

import java.util.List;


/**
 * 文件的详情
 *
 * @author lyw
 * @email 1798883178@qq.com
 * @date 2025-06-03 14:14:53
 */
public interface UploadFileDetailProducer {


    /**
     * 文件的详情列表
     * @param uploadFileDetailListBo 文件的详情列表查询参数
     * @return
     */
    PageUtils<UploadFileDetailListVo> queryPage(UploadFileDetailListBo uploadFileDetailListBo);

    /**
    * 文件的详情信息
    * @param id 文件的详情id
    * @return
    */
    UploadFileDetailInfoVo info(Long id);

    /**
     * 新增文件的详情
     * @param uploadFileDetailBo 文件的详情对象
     * @return
     */
     UploadFileDetailInfoVo save(UploadFileDetailBo uploadFileDetailBo);

    /**
     * 修改文件的详情
     * @param uploadFileDetailBo 文件的详情对象
     * @return
     */
    Boolean update(UploadFileDetailBo uploadFileDetailBo);

    /**
     * 删除文件的详情
     * @param id 文件的详情id
     * @return
     */
    void deleteById(Long id);


    UploadFileDetailVo getFileContent(String sourceId, Integer type, AnchorVideoFileAllVo videoFileAllVo);


    List<VideoContentVo> selFileContents(String sourceId, Integer type, Integer sourceType, Long userId, Long tenantId);

    void inserto(GenerateVideoContentBo bo);

    UploadFileDetailVo getByFileId(GenerateVideoContentBo bo, Long setTime);

    void updateFileDetailById(UploadFileDetailVo uploadFileDetailVo);

    UploadFileDetailVo getByFile(String sourceId);

    UploadFileDetailVo getEmtyByFileId(String videoId);

    /**
     * 获取待生成的优化、自然原文
     *
     * @param fileIds
     * @param userId
     * @param tenantId
     */
    List<UploadFileDetailInfoVo> contentByToGenerated(List<String> fileIds, Long userId, Long tenantId);

    /**
     * 修改状态
     *
     * @param uploadFileDetailBo
     * @return
     */
    boolean updateContentStatus(UploadFileDetailBo uploadFileDetailBo);

    /**
     * 获取文件详情-如果没有就创建
     * @param uploadFileDetailBo
     * @return
     */
    UploadFileDetailInfoVo getAndSave(UploadFileDetailBo uploadFileDetailBo);

    /**
     * 修改文件详情状态
     *
     * @param id
     * @param type
     * @param contentStatus
     * @return
     */
    boolean updateContentStatus(Long id, int type, int contentStatus);

    /**
     * 根据sourceId更新内容状态（用于定时器异步生成流程）
     *
     * @param sourceId 文件id
     * @param type     内容类型 1自然/2优化
     * @param status   内容状态
     * @param resetJob 是否重置set_job为0
     */
    boolean updateContentStatusBySourceId(String sourceId, Integer type, int status, boolean resetJob);

    /**
     * 乐观锁标记拾取（set_job=0 → set_job=1），返回true表示拾取成功
     *
     * @param sourceId 文件id
     * @param type     内容类型 1自然/2优化
     */
    boolean markPickedUp(String sourceId, Integer type);

    /**
     * 查询待生成原文的文件详情列表
     */
    List<UploadFileDetailVo> selectFileDetailData(Integer limit);
}

