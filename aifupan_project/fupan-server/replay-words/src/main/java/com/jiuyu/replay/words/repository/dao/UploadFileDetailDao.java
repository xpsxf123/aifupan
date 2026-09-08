package com.jiuyu.replay.words.repository.dao;

import com.jiuyu.replay.words.entity.UploadFileDetailEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 文件的详情
 *
 * @author lyw
 * @email 1798883178@qq.com
 * @date 2025-06-03 14:14:53
 */
@Mapper
public interface UploadFileDetailDao extends BaseMapper<UploadFileDetailEntity> {


    @Insert("INSERT IGNORE INTO tb_upload_file_detail " +
            "(id, file_id, nature_content_status, optimize_content_status" +
            ", has_diagnosis_report, user_id ,tenant_id ,create_date ,update_date) " +
            "VALUES " +
            "(#{id}, #{fileId}, #{natureContentStatus}, #{optimizeContentStatus}, #{hasDiagnosisReport}" +
            ", #{userId}, #{tenantId}, #{createDate}, #{updateDate})")

    void inserto(UploadFileDetailEntity uploadFileDetailEntity);

    /**
     * 查询待生成原文的文件详情（状态=生成中 & 未被拾取 & 来源=服务器）
     */
    List<UploadFileDetailEntity> selectFileDetailData(@Param("limit") Integer limit);
}
