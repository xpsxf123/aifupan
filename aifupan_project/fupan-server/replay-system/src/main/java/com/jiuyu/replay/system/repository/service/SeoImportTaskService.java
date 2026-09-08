package com.jiuyu.replay.system.repository.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jiuyu.replay.system.bo.SeoImportFileBo;
import com.jiuyu.replay.system.entity.SeoImportTaskEntity;
import com.jiuyu.replay.system.vo.SeoImportProgressVo;
import com.jiuyu.replay.system.vo.SeoImportTaskVo;

import java.util.List;

/**
 * SEO 文章批量导入任务。
 *
 * @author claude
 * @date 2026-08-13
 */
public interface SeoImportTaskService extends IService<SeoImportTaskEntity> {

    /**
     * 提交一批 Markdown 文件，立即返回任务 ID，实际导入在后台线程进行。
     *
     * @param files              文件内存副本（不能传 MultipartFile，见 {@link SeoImportFileBo}）
     * @param autoCreateCategory front matter 里的分类不存在时是否自动创建
     * @return 任务 ID 与文件总数
     * @throws com.jiuyu.replay.generic.vo.common.exception.BusinessException 文件为空或超出数量上限
     */
    SeoImportTaskVo submit(List<SeoImportFileBo> files, boolean autoCreateCategory);

    /**
     * 查询导入进度与逐篇结果。
     *
     * @param taskId 任务 ID
     * @return 进度
     * @throws com.jiuyu.replay.generic.vo.common.exception.BusinessException 任务不存在
     */
    SeoImportProgressVo progress(String taskId);
}
