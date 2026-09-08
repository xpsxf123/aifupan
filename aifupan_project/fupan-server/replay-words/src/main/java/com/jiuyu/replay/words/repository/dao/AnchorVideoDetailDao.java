package com.jiuyu.replay.words.repository.dao;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jiuyu.replay.generic.vo.words.anchor.AnchorUrlTradeVo;
import com.jiuyu.replay.words.entity.AnchorVideoDetailEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 视频的详情
 * 
 * @author lyw
 * @email 1798883178@qq.com
 * @date 2025-05-27 15:30:33
 */
@Mapper
public interface AnchorVideoDetailDao extends BaseMapper<AnchorVideoDetailEntity> {


    @Insert("INSERT IGNORE INTO tb_anchor_video_detail " +
            "(id, video_id, nature_content_status, optimize_content_status" +
            ", has_diagnosis_report, user_id ,tenant_id ,create_date ,update_date) " +
            "VALUES " +
            "(#{id}, #{videoId}, #{natureContentStatus}, #{optimizeContentStatus}, #{hasDiagnosisReport}" +
            ", #{userId}, #{tenantId}, #{createDate}, #{updateDate})")
    void inserto(AnchorVideoDetailEntity anchorVideoDetailEntity);

    List<AnchorVideoDetailEntity> selectVideoDetailData(@Param("limit") Integer limit);

    /**
     * 根据secUid集合获取主播行业
     *
     * @param secUids secUid集合
     * @return 行业
     */
    List<AnchorUrlTradeVo> tradeListBySecUids(@Param("secUids") List<String> secUids);
}
