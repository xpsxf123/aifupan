package com.jiuyu.replay.system.repository.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jiuyu.replay.system.entity.SeoImportTaskEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * SEO 文章批量导入任务。
 *
 * @author claude
 * @date 2026-08-13
 */
@Mapper
public interface SeoImportTaskDao extends BaseMapper<SeoImportTaskEntity> {
}
